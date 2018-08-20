import java.util.Iterator;
import java.util.Map;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.File;
import java.io.FileReader;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.util.MapBuilder;
import com.arangodb.model.AqlQueryOptions;

class ArangoTask implements Callable<long[]> {
    private ArangoDB arangoDB = null;
    private ArangoDatabase db = null;
    private int depth = 0;
    private String root = "";
    public ArangoTask(String dbName, int depth, String root){
        this.arangoDB = new ArangoDB.Builder().user("root").password("root").build();
        this.db = arangoDB.db(dbName);
        this.depth = depth;       
        this.root = root;
    }
    @Override
    public long[] call() throws Exception {
 
        String query = "FOR v IN "+depth+".."+depth+" OUTBOUND 'vertex/"+root+"' edge RETURN distinct v._id";
        long startTime = System.nanoTime();
        ArangoCursor<String> cursor = db.query(query, null, new AqlQueryOptions().count(true), String.class);
        long endTime = System.nanoTime();
        long diff = (endTime - startTime)/1000000;
        arangoDB.shutdown();
        
        return new long[]{cursor.count(), diff};
    }
}

public class  khop {


  public static void main(String[] args) {
        
        if (args.length == 0 || args.length < 3){
            System.out.println("Provide graph name (graph500, twitter), depth (1,2,3,6) AND timeout in seconds");
            System.exit(0);
        }
  
        String dbName = args[0];
        int depth = Integer.parseInt(args[1]);
        int timeout = Integer.parseInt(args[2]);
        String seedFile = "graph500-22-seed";
        if (dbName.equals("twitter")){
            seedFile = "twitter_rv.net-see";
        }
  try {     
        // reed seeds
        File file = new File(seedFile);
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        StringBuffer stringBuffer = new StringBuffer();
        String line = bufferedReader.readLine();
        String[] roots = line.split(" ");

        //file to write results
        FileWriter writer = new FileWriter("khopResults_" + dbName + "_" + depth);
        writer.write("k-hop query with depth = " + depth + "\n");
        writer.write("start vertex,\tneighbor size,\tquery time (in ms)\n");
        
        long totalSize = 0;
        double totalTime = 0.0;
        int errorQuery = 0;
        int totalQuery = 0;
        long[] result = new long[0];;
        for(String root:roots) {
            
            // for depth 3 qnd 6 only need to run 10 queries 
            if (depth > 2 && totalQuery == 10){
                break;
            }
            
            totalQuery++;
            ArangoTask arangoTask = new ArangoTask(dbName, depth, root); 
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<long[]> future = executor.submit(arangoTask);
            try {
                System.out.println("Starting...seed=" + root);
                result = future.get(timeout, TimeUnit.SECONDS);
                System.out.println("Finished!");
            } catch (TimeoutException e) {
                future.cancel(true);
                result = new long[]{-1, -1};
                errorQuery++;
                System.out.println("TIMEOUT: query terminated!");
            } catch (Exception e){
                System.out.println("Failed to terminate:" + e.getMessage());
            } 
         
            executor.shutdownNow();
            if (result[0] != -1){
                totalSize += result[0];
                totalTime += result[1];
            }
            
            writer.write(root + ",\t" + Long.toString(result[0]) + ",\t" + Long.toString(result[1]) + "\n");
            writer.flush();
        
        }
        double avgSize = totalQuery == errorQuery ? -1.0 : (double)totalSize/(double)(totalQuery-errorQuery);
        double avgTime = totalQuery == errorQuery ? -1.0 : totalTime/(double)(totalQuery-errorQuery);
        System.out.println("===================SUMMARY=================================\n");
        System.out.println("Total "+ depth + "-Neighborhood size: " + totalSize);
        System.out.println("Total elapsed time, ms: " + totalTime);
        System.out.println("Total number of queries: " + totalQuery);
        System.out.println("Number of failed queries: " + errorQuery);
        System.out.println("Average " + depth + "-Neighborhood size: " + avgSize);
        System.out.println("Average query time, ms: " + avgTime);

        writer.write("===================SUMMARY=================================\n");
        writer.write("Total number of queries:\t" + totalQuery + "\n" + "Total elapsed time, ms:\t" + totalTime + "\n" + "Total Neighborhood size:\t" + totalSize + "\n"  + "Total number of failed queries:\t" + errorQuery + "\n" + "Average Neighborhood size:\t" + avgSize + "\n" + "Average query time, ms:\t" + avgTime + "\n");
            writer.flush();
            writer.close();
        } catch (Exception e) {
                e.printStackTrace();
        } 
        System.out.println("Done!");
        System.exit(0); 
  
  }


}

