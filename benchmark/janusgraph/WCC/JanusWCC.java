/*
* Copyright (c)  2015-now, TigerGraph Inc.
* All rights reserved
* It is provided as it is for benchmark reproducible purpose.
* anyone can use it for benchmark purpose with the
* acknowledgement to TigerGraph.
* Author: Litong Shen litong.shen@tigergraph.com
*/
package WCC;
import java.util.*;  
import java.io.BufferedReader; 
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration; 
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.*;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.process.computer.ComputerResult;  

import org.janusgraph.core.schema.*; 
import org.janusgraph.util.datastructures.CompactMap;  
import org.janusgraph.core.util.*; 
import org.janusgraph.core.*;
import org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration; 
import org.janusgraph.graphdb.database.management.*; 

import java.io.FileWriter;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.time.Duration;  
import java.util.concurrent.Callable; 
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors; 

public class JanusWCC {
	public static JanusGraph JanusG; 
	
	public static void main(String[] args){
		// arg 0: property file
		String confPath = args[0];

		JanusG = JanusGraphFactory.open(confPath); 
		
		//set query timeout
		try{
                        // initialize output file 
                        String resultFileName = "WCC-latency-twitter" ;
                        String resultFilePath = "/5ebs/benchmark/code/janusgraph/result/" + resultFileName;
                        FileWriter writer = new FileWriter(resultFilePath);
                        writer.write("starting WCC, query time (in ms)\n");
			writer.flush();
			
			//set query timeout here
			final Duration timeout = Duration.ofSeconds(86400); 
			long duration = 0;

			ExecutorService executor = Executors.newSingleThreadExecutor();
			long startTime = System.nanoTime();
			final Future<String> handler = executor.submit(new Callable() {
				@Override
				public String call() throws Exception {
					Future<ComputerResult> result = JanusG.compute().workers(4).program(WCC.build().create(JanusG)).submit();	
					
					Graph tmp = result.get().graph();
					return "complete";	
				}
			});

			try {
				handler.get(timeout.toMillis(), TimeUnit.MILLISECONDS); 
			} catch (Exception e) {
				handler.cancel(true); 
				System.out.println("Exception occurred"); 
				e.printStackTrace();
			}
			
			// calculation query time
			duration = System.nanoTime() - startTime;
			System.out.println("=============================================");
			System.out.println("toal time = " + duration/1000000.0 + "ms");
			System.out.println("=============================================");
			executor.shutdownNow();
			
			writer.write("=============================================\n"
				+ "toal time = " + duration/1000000.0 + "ms");
			writer.flush();
			writer.close();

		}catch(Exception ioe) {
			ioe.printStackTrace();
		}
		System.exit(0);
	}
}

