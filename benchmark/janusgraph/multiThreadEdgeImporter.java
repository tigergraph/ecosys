/*
* Copyright (c)  2015-now, TigerGraph Inc.
* All rights reserved
* It is provided as it is for benchmark reproducible purpose.
* anyone can use it for benchmark purpose with the
* acknowledgement to TigerGraph.
* Author: Litong Shen litong.shen@tigergraph.com
*/
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileWriter; 

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

import org.janusgraph.core.schema.*;
import org.janusgraph.util.datastructures.CompactMap;
import org.janusgraph.core.util.*;
import org.janusgraph.core.*;
import org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration;
import org.janusgraph.graphdb.database.management.*;
import org.janusgraph.graphdb.vertices.AbstractVertex;
import org.janusgraph.graphdb.internal.ElementLifeCycle;
import org.janusgraph.graphdb.vertices.StandardVertex;
import org.janusgraph.graphdb.transaction.StandardJanusGraphTx;

public class multiThreadEdgeImporter {
	public static JanusGraph JanusG;
	public static int commitBatch = 4000;

	private static List<Long> source = new ArrayList<>(commitBatch);
  	private static List<Long> target = new ArrayList<>(commitBatch);
  	private static List<String> fileBuffer = new ArrayList<>();

	private static ExecutorService pool = Executors.newFixedThreadPool(8);

	private static HashMap<Long, Long> idset1= new HashMap<Long, Long>();	

	public static void main(String[] args){
		String datasetDir = args[0];
		String confPath = args[1];
		commitBatch = Integer.parseInt(args[2]);
		String hashmapDir = args[3];

		BaseConfiguration config = new BaseConfiguration();
	
		JanusG = JanusGraphFactory.open(confPath);

		try {
			for(int i =0; i<8; i++) {
				// hashmap reader
				BufferedReader hashmapReader = new BufferedReader(new InputStreamReader(new FileInputStream(hashmapDir + String.valueOf(i))));
				String line2; 
			
				//create vertex hashmap without new vertex
				while((line2 = hashmapReader.readLine()) != null) {
					String[] kvPairs = line2.split("\t");
					idset1.put(Long.parseLong(kvPairs[0]),Long.parseLong(kvPairs[1]));
				}	
				hashmapReader.close();
			}
			System.out.println("finished construct vertex hashmap");

			//add edge

                        long lineCounter = 0;
                        long startTime = System.nanoTime();

			// edge reader
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(datasetDir)));
			readFile2Buffer(reader);
			reader.close();

			// add edge with idset1(hashmap without create vertex)
			
			for(String line : fileBuffer) {
				try {
					String[] parts = line.split("\t");

					Long src = Long.parseLong(parts[0]);
					Long tgt = Long.parseLong(parts[1]);
					source.add(src);
                 			target.add(tgt);

					lineCounter ++;
					if(lineCounter % commitBatch == 0) {
						System.out.println("---- commit ----: " + Long.toString(lineCounter / commitBatch));
						batchCommit();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			batchCommit();
                        pool.shutdown();
                        try {
                                pool.awaitTermination(1440, TimeUnit.MINUTES);
                        } catch (InterruptedException e) {
                                e.printStackTrace();
                        }

			JanusG.tx().commit();


			long endTime = System.nanoTime();
			long duration = (endTime - startTime);
			System.out.println("######## loading time #######  " + Long.toString(duration/1000000) + " ms");
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		System.out.println("---- done ----");
		System.exit(0);	
	}


	/** this function read data from disk to memory
	* @para reader buffering characters
	*/
	private static void readFile2Buffer(BufferedReader reader) {
		try {
			String line;
			while((line = reader.readLine()) != null) {
				fileBuffer.add(line);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/** this function use multi threads to batch load data
	*/
	private static void batchCommit() {
		List<Long> src = source;
		source = new ArrayList<>(commitBatch);
		List<Long> tgt = target;
		target = new ArrayList<>(commitBatch);

		pool.submit(() -> {
			Long srcInternalId, dstInternalId;
			JanusGraphVertex srcVertex, dstVertex;
			JanusGraphTransaction tx = JanusG.newTransaction();
			for (int i = 0; i < src.size(); i++) {
				srcInternalId = idset1.get(src.get(i));
				dstInternalId = idset1.get(tgt.get(i));
				
				srcVertex = tx.getVertex(srcInternalId);
                		dstVertex = tx.getVertex(dstInternalId);
				JanusGraphEdge edge = srcVertex.addEdge("MyEdge", dstVertex);
			}
			tx.commit();
		});
	}
	
}
