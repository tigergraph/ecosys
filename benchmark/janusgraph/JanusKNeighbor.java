import java.util.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader; 

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.*;
import org.apache.tinkerpop.gremlin.structure.Graph;

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

public class JanusKNeighbor {
	public static JanusGraph janusG;
	public static int steps = 1;
	public static GraphTraversalSource ts;

	public static void main(String[] args){
		// args 0: property file
		String confPath = args[0];
		// args 1: root file
		String rootFile = args[1];
		// args 2: traversal depth
		steps = Integer.parseInt(args[2]);
		// args 3: how many roots to test
		int test_count = Integer.parseInt(args[3]);
		// args 4: step size for uniform sampling
		int sample_step = 1;
		if(args.length >= 5){
			sample_step = Integer.parseInt(args[4]);
		}

                janusG = JanusGraphFactory.open(confPath);

		try {
			// initialize output file 
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(rootFile)));
			String resultFileName = "KN-latency-twitter-Traversal-" + steps;
			String resultFilePath = "/Ebs/benchmark/code/janusgraph/result/" + resultFileName;
			FileWriter writer = new FileWriter(resultFilePath);
			writer.write("start vertex,\tneighbor size,\tquery time (in ms)\n");
			writer.flush();

			String line = reader.readLine();
			reader.close();
			String[] roots = line.split(" ");

			ts = janusG.traversal();

			// Here set query timeout to 180s
			final Duration timeout = Duration.ofSeconds(180);
			
			long total_neighbor_size = 0;
                        long total_duration = 0;
			int errorQuery = 0;

			for(int i = 0; i < test_count; i += sample_step) {
				boolean error = false;
				long neighbor_size;
				long duration;
				String root = roots[i];
				ExecutorService executor = Executors.newSingleThreadExecutor();

				// handle query timeout
				long startTime = System.nanoTime();
				final Future<Long> handler = executor.submit(new Callable() {
					@Override
					 public Long call() throws Exception {
				        	//return runKNeighbor(root) - 1;
				        	return runKNeighbor(root);
				    	}
				});
	
				try {
					neighbor_size = handler.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
				} catch (Exception e) {
					errorQuery ++;
					neighbor_size = 0;
					error = true;
					handler.cancel(true);
					System.out.println("Exception occurred");
					e.printStackTrace();
				}
				
				// query time calculation 
				duration = System.nanoTime() - startTime;	
				executor.shutdownNow();			

				neighbor_size = error? -1:neighbor_size;
				duration = error? -1:duration;
				total_neighbor_size += error? 0: neighbor_size;
				total_duration += error? 0:duration;			
	
				writer.write(root + ",\t" + neighbor_size + ",\t" + duration/1000000.0 + "\n");
				writer.flush();
				System.out.println(root + "," + Long.toString(neighbor_size) + "," + Double.toString((double)duration/1000000.0));
			}
			
			
			writer.write("number of start vertex:\t" + test_count + "\n"
		   	+ "number of query didn't finish correctly:\t" + errorQuery + "\n"
			+ "total neighbor size:\t" + total_neighbor_size + "\n"
	   		+ "total query time:\t" + total_duration/1000000.0 + "\n"
	  		+ "average neighbor size:\t" + total_neighbor_size/(test_count - errorQuery) + "\n"
	  		+ "average query time:\t" + total_duration/1000000.0/(test_count - errorQuery) + "\n");
			
	
			System.out.println("######## number of start vertex #######  " + test_count);
			System.out.println("######## number of timeout queries #######  " + errorQuery);
			System.out.println("######## total time #######  " + Double.toString((double)total_duration/1000000.0) + " ms");
			System.out.println("######## average time #######  " + Double.toString((double)total_duration/1000000.0/(double)(test_count - errorQuery)) + " ms");
			System.out.println("######## average kneighbor #######  " + Double.toString((double)total_neighbor_size/(double)(test_count -errorQuery)));

			writer.flush();
			writer.close();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		System.exit(0);
	}

	/** this funtion calculate k-hop distinct neighbor size
	* return k-hop distinct neighbor size
	* @para root the start vertex
	*/
	private static Long runKNeighbor(String root) {
		return ts.V().has("id", root).repeat(__.out("MyEdge")).times(steps).dedup().count().next();
	}
}
