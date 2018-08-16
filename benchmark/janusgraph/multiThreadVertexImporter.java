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
import java.util.concurrent.ThreadFactory;

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

public class multiThreadVertexImporter {
	public static JanusGraph JanusG;
	public static int commitBatch = 1;

	private static Set<Long> allVertices = new HashSet<>();
	private static Set<Long> vertices = new HashSet<>();

	private static List<String> fileBuffer = new ArrayList<>();

	private static ExecutorService pool = Executors.newFixedThreadPool(8, new MyThreadFactory());

	public static void main(String[] args){
		String datasetDir = args[0];
		String confPath = args[1];
		commitBatch = Integer.parseInt(args[2]);

		BaseConfiguration config = new BaseConfiguration();
	
		JanusG = JanusGraphFactory.open(confPath);
                JanusG.close();
                JanusGraphCleanup.clear(JanusG);
                JanusG = JanusGraphFactory.open(confPath);

		ManagementSystem mgmt = (ManagementSystem) JanusG.openManagement();
                mgmt.makeEdgeLabel("MyEdge").make();
                mgmt.makeVertexLabel("MyNode").make();
                PropertyKey id_key = mgmt.makePropertyKey("id").dataType(String.class).make();		
		PropertyKey pageRank_key = mgmt.makePropertyKey("gremlin.pageRankVertexProgram.pageRank").dataType(Double.class).make();
                PropertyKey edgeCount_key = mgmt.makePropertyKey("gremlin.pageRankVertexProgram.edgeCount").dataType(Long.class).make();
		PropertyKey groupId_key = mgmt.makePropertyKey("WCC.groupId").dataType(Long.class).make();
		mgmt.buildIndex("byId", JanusGraphVertex.class).addKey(id_key).unique().buildCompositeIndex();
                mgmt.commit();
                try{
                        mgmt.awaitGraphIndexStatus(JanusG, "byId").call();
                }
                catch(Exception ex) {
                        ex.printStackTrace();
                        System.exit(-1);
                }
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(datasetDir)));
			long lineCounter = 0;
                        long startTime = System.nanoTime();

			readFile2Buffer(reader);	
		
			for(String line : fileBuffer) {
				try {
					Long node = Long.parseLong(line);
					allVertices.add(node);
					vertices.add(node);
					lineCounter++;
                                	if(lineCounter % commitBatch == 0){
						batchCommit();
                                		System.out.println("---- commit ----: " + Long.toString(lineCounter / commitBatch));
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
	
			long endTime = System.nanoTime();
                        long duration = (endTime - startTime);
                        System.out.println("######## time to generateVertexHashMap and load vertex file #######  " + Long.toString(duration/1000000) + " ms");
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("---- done ----, total V: " + Integer.toString(allVertices.size()));
		System.exit(0);	
	}

	/** this function read file from disk to memory 
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

	/** this function use multi threads to batch load data and 
	* write hashmap(vertexExternalId: vertexInternalId) to disk
	*/
	private static void batchCommit() {
		Set<Long> newVertices = vertices;
		vertices = new HashSet<>();
		
		pool.submit(() -> {
			try {
				String threadId = Thread.currentThread().getName();
				String hashMapName = "hashMap" + threadId;
				String hashMapPath = "/Ebs/benchmark/code/janusgraph/data/" + hashMapName;
				
				FileWriter writer = new FileWriter(hashMapPath, true);
				
				JanusGraphTransaction tx = JanusG.newTransaction();
				for (Long n : newVertices) {
					JanusGraphVertex srcVertex = tx.addVertex("MyNode");
					srcVertex.property("id", n);
					srcVertex.property("gremlin.pageRankVertexProgram.pageRank", 1.0);
					srcVertex.property("gremlin.pageRankVertexProgram.edgeCount", 0);
					srcVertex.property("WCC.groupId", n);
					try {
						writer.write(n + "\t" + srcVertex.id() + "\n");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				writer.flush();
				writer.close();
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	/** this class define name for threads
	*/
	private static class MyThreadFactory implements ThreadFactory {
		private int counter = 0;
		public Thread newThread(Runnable r) {
			return new Thread(r, "" + counter++);
		}
	}		

}
