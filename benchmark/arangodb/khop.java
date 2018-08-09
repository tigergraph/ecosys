import java.util.Iterator;
import java.util.Map;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.File;
import java.io.FileReader;

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

public class  khop {


  public static void main(String[] args) {
    
     // k=1,2 timeout is 180s
    final ArangoDB arangoDB = new ArangoDB.Builder().timeout(180000).user("root").password("root").build();
    // k=3,6 timeout is 9000s (or 2.5 hours)
    //final ArangoDB arangoDB = new ArangoDB.Builder().timeout(9000000).user("root").password("root").build();

    if (args.length == 0 || args.length == 1){
       System.out.println("Provide graph name (graph500, twitter) AND depth (1,2,3,6)");
    }
    else if (args[0].equals("graph500")){
       System.out.println("Running kNeighborhood Query on graph500 dataset ...");
       // parameters: arangoDB, dbName, filename, depth
       RunKHop(arangoDB, "graph500","graph500-22-seed", Integer.parseInt(args[1]));
    }
    else if (args[0].equals("twitter")){
       System.out.println("Running kNeighborhood Query on twitter dataset ...");
       // parameters: arangoDB, dbName, filename, depth
       RunKHop(arangoDB, "twitter","twitter_rv.net-seed", Integer.parseInt(args[1]));
    }


  System.exit(0);
        
       }

 public static void RunKHop(ArangoDB arangoDB, String dbName, String fileName, int depth){
  
 ArangoDatabase db = null;
 String[] roots = new String[0];

 long totalSize = 0;
 double totalTime = 0.0;
 int errorQuery = 0; 
  //read file with random start vertices AND connect to ArangoDB
    try {
         File file = new File(fileName);
         FileReader fileReader = new FileReader(file);
         BufferedReader bufferedReader = new BufferedReader(fileReader);
         StringBuffer stringBuffer = new StringBuffer();
         String line = bufferedReader.readLine();
         roots = line.split(" ");

         db = arangoDB.db(dbName);

         //file to write results
         FileWriter writer = new FileWriter("khopResults_" + dbName + "_" + depth);
         writer.write("k-hop query with depth = " + depth + "\n");
         writer.write("start vertex,\tneighbor size,\tquery time (in ms)\n");
     
     int totalQuery = 0;
     String steps = "1.." + depth;
     for(String root:roots) {
         
         if (depth > 2 && totalQuery == 10){
           break;
         }

         String query = "FOR v IN " + steps + " OUTBOUND 'vertex/" +root+ "' edge OPTIONS {bfs:true, uniqueVertices:'global'} COLLECT WITH COUNT INTO v RETURN v";
         long[] Result = kHopQuery(db, query);
         System.out.println("Start vertex: " + root);
         totalQuery++;
         if (Result[0] == -1){
           errorQuery++; 
         }
         else {
           totalSize += Result[0];
           totalTime += Result[1];  
         }

         writer.write(root + ",\t" + Long.toString(Result[0]) + ",\t" + Long.toString(Result[1]) + "\n");
         writer.flush();
     }

 System.out.println("Total "+ depth + "-Neighborhood size: " + totalSize);
 System.out.println("Total elapsed time, ms: " + totalTime);
 System.out.println("Total number of queries: " + totalQuery);
 System.out.println("Number of failed queries: " + errorQuery);
 System.out.println("Average " + depth + "-Neighborhood size: " + totalSize/(totalQuery-errorQuery));
 System.out.println("Average query time, ms: " + totalTime/(totalQuery-errorQuery));

 writer.write("===================SUMMARY=================================\n");
 writer.write("Total number of queries:\t" + totalQuery + "\n" + "Total elapsed time, ms:\t" + totalTime + "\n" + "Total Neighbrhood size:\t" + totalSize + "\n"  + "Total number of failed queries:\t" + errorQuery + "\n" + "Average Neighborhood size:\t" + totalSize/(totalQuery - errorQuery) + "\n" + "Average query time, ms:\t" + totalTime/(totalQuery - errorQuery) + "\n");
 writer.flush();
 writer.close();

    } catch (Exception e) {
            System.err.println("Failed to execute query. " + e.getMessage());
    }
    
}

public static long[] kHopQuery(ArangoDatabase db, String query){

try {
         
         long startTime = System.nanoTime();
         ArangoCursor<String> cursor = db.query(query, null, null, String.class);
         long endTime = System.nanoTime();
         long diff = (endTime - startTime)/1000000;
         
         return new long[]{Long.parseLong(cursor.next()), diff};
} catch (ArangoDBException e){
   System.err.println("Failed to execute query. " + e.getMessage());
}

return new long[]{-1, -1};
}



}
