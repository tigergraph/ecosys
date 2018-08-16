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

import java.io.FileWriter; 



public class singleThreadGraph500Importer {
	public static JanusGraph JanusG;
	public static int commitBatch = 1;

	private static HashMap<String, JanusGraphVertex> idset = new HashMap<String, JanusGraphVertex>();

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
		//properties for pageRank

		PropertyKey pageRank_key = mgmt.makePropertyKey("gremlin.pageRankVertexProgram.pageRank").dataType(Double.class).make();
		PropertyKey edgeCount_key = mgmt.makePropertyKey("gremlin.pageRankVertexProgram.edgeCount").dataType(Long.class).make();	

		//properties for WCC

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
			String line;
			long lineCounter = 0;
			long startTime = System.nanoTime();
			while((line = reader.readLine()) != null) {
				try {
					String[] parts = line.split("\t");

					processLine(parts[0], parts[1]);
					
					lineCounter++;
					if(lineCounter % commitBatch == 0){
						System.out.println("---- commit ----: " + Long.toString(lineCounter / commitBatch));
						JanusG.tx().commit(); 
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			JanusG.tx().commit(); 
			long endTime = System.nanoTime();
			long duration = (endTime - startTime);
			System.out.println("######## loading time #######  " + Long.toString(duration/1000000) + " ms");
			reader.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		System.out.println("---- done ----, total V: " + Integer.toString(idset.size()));
		System.exit(0);	
	}

	/** This function add vertex and edge
	* @param srcId the source vertex of the edge
	* @param dstId the destination vertex of the edge
	*/

	private static void processLine(String srcId, String dstId) {
		JanusGraphVertex srcVertex = (JanusGraphVertex)idset.get(srcId);
		JanusGraphVertex dstVertex = (JanusGraphVertex)idset.get(dstId);
		if(srcVertex == null) {
			Long groupId = Long.parseLong(srcId);

			srcVertex = JanusG.addVertex("MyNode");
			srcVertex.property("id", srcId);
			srcVertex.property("gremlin.pageRankVertexProgram.pageRank", 1.0);
			srcVertex.property("gremlin.pageRankVertexProgram.edgeCount", 0);
			srcVertex.property("WCC.groupId", groupId);
			
			idset.put(srcId, srcVertex);
		}
		if(dstVertex == null) {
			Long groupId = Long.parseLong(dstId);
			
			dstVertex = JanusG.addVertex("MyNode");
			dstVertex.property("id", dstId);
			dstVertex.property("gremlin.pageRankVertexProgram.pageRank", 1.0);
			dstVertex.property("gremlin.pageRankVertexProgram.edgeCount", 0);
			dstVertex.property("WCC.groupId", groupId);
			
			idset.put(dstId, dstVertex);
		}
		
		srcVertex.addEdge("MyEdge", dstVertex);
	}
	
	
}
