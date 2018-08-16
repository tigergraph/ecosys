/*
* Copyright (c)  2015-now, TigerGraph Inc.
* All rights reserved
* It is provided as it is for benchmark reproducible purpose.
* anyone can use it for benchmark purpose with the
* acknowledgement to TigerGraph.
* Author: Litong Shen litong.shen@tigergraph.com
*/
import java.util.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

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

import java.io.FileWriter; 


public class singleThreadEdgeImporter {
	public static JanusGraph JanusG;
	public static int commitBatch = 1;

	private static HashMap<String, Long> idset1= new HashMap<String, Long>();	

	public static void main(String[] args){
		String datasetDir = args[0];
		String confPath = args[1];
		commitBatch = Integer.parseInt(args[2]);
		String hashmapDir = args[3];

		BaseConfiguration config = new BaseConfiguration();
	
		JanusG = JanusGraphFactory.open(confPath);

		try {
			// edge reader
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(datasetDir)));
			String line;

			// hashmap reader
			BufferedReader hashmapReader = new BufferedReader(new InputStreamReader(new FileInputStream(hashmapDir)));
			String line2 = hashmapReader.readLine();
		
			//create vertex hashmap without new vertex
			while((line2 = hashmapReader.readLine()) != null) {
				String[] kvPairs = line2.split("\t");
				Long internalId = Long.parseLong(kvPairs[1]);
				idset1.put(kvPairs[0],internalId);
			}	

			hashmapReader.close();
			System.out.println("finished construct vertex hashmap");

			//add edge

                        long lineCounter = 0;
                        long startTime = System.nanoTime();

			// add edge with idset1(hashmap without create vertex)
			
			JanusGraphTransaction tx = JanusG.newTransaction();
			while((line = reader.readLine()) != null) {
				try {
					String[] parts = line.split("\t");
					addEdge(parts[0], parts[1], tx);		
					lineCounter ++;
					if(lineCounter % commitBatch == 0) {
						System.out.println("---- commit ----: " + Long.toString(lineCounter / commitBatch));
                                                tx.commit(); 
                                                tx = JanusG.newTransaction();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			tx.commit();

			long endTime = System.nanoTime();
			long duration = (endTime - startTime);
			System.out.println("######## loading time #######  " + Long.toString(duration/1000000) + " ms");
			reader.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		System.out.println("---- done ----");
		System.exit(0);	
	}


	/** this function add edge to graph
	* @para srcId the sourceId(internal id) of source vertex
	* @para dstId the dextionationId(internal id) of destination vertex
	* @para tx the current JanusGraph transaction
	*/
	private static void addEdge(String srcId, String dstId, JanusGraphTransaction tx) {
		Long srcInternalId = idset1.get(srcId);
                Long dstInternalId = idset1.get(dstId);
		JanusGraphVertex srcVertex, dstVertex;

		srcVertex = tx.getVertex(srcInternalId);
		dstVertex = tx.getVertex(dstInternalId);

		srcVertex.addEdge("MyEdge", dstVertex);
	}
	
	
}
