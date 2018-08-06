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

public class generateVertexHashMap {
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

		JanusGraphTransaction tx = JanusG.newTransaction();

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

		String hashMapName = "test-hashMap";
		String hashMapPath = "/Ebs/benchmark/code/janusgraph/data/" + hashMapName;

		try {
			FileWriter writer = new FileWriter(hashMapPath);
                	writer.write("externalId\tinternalId\n");

			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(datasetDir)));
                        String line;
			long lineCounter = 0;
                        long startTime = System.nanoTime();
			
			while((line = reader.readLine()) != null) {
				try {
                        		processLine(line, writer);
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
			writer.flush();
			writer.close();
			long endTime = System.nanoTime();
                        long duration = (endTime - startTime);
                        System.out.println("######## time to generateVertexHashMap and load vertex file #######  " + Long.toString(duration/1000000) + " ms");
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("---- done ----, total V: " + Integer.toString(idset.size()));
		System.exit(0);	
	}

	/** this function add vertex to graph and write hashmap(vertex's externalId V.S. internalId) to disk
	* @para srcId the current processing vertex
	* @para writer the filewriter to write hashmap to disk
	*/
	private static void processLine(String srcId, FileWriter writer) {
		JanusGraphVertex srcVertex = (JanusGraphVertex)idset.get(srcId);
		if(srcVertex == null) {
			Long groupId = Long.parseLong(srcId);

			srcVertex = JanusG.addVertex("MyNode");
			srcVertex.property("id", srcId);
			srcVertex.property("gremlin.pageRankVertexProgram.pageRank", 1.0);
			srcVertex.property("gremlin.pageRankVertexProgram.edgeCount", 0);
			srcVertex.property("WCC.groupId", groupId);
			
			idset.put(srcId, srcVertex);
			
			try {
				writer.write(srcId + "\t" + srcVertex.id() + "\n");
				writer.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	
}
