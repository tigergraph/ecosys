import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.*;
import java.io.FileWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ProcessBuilder;
import java.net.ConnectException;

public class kn{
  // need to modify URL, depth, seedCount
  static String URL = "http://your-neptune-endpoint:8182";
  static String depth = "6";
  static double seedCount = 200;

  final static long OK = 0;
  final static long TIMEOUT = 1;

  public static void main(String[] args){
    try{
      
      // need to modify file for random seed
      File file = new File("randomTwitter");
      FileReader fileReader = new FileReader(file);
      BufferedReader bufferedReader = new BufferedReader(fileReader);
      StringBuffer stringBuffer = new StringBuffer();
      String line = bufferedReader.readLine();
	
      // need to modify resultFileName, resultFilePath
      String resultFileName = "KN-latency-Twitter-" + depth;
      String resultFilePath = "/home/ec2-user/r4_4xlarge/query/" + resultFileName;

      FileWriter writer = new FileWriter(resultFilePath);
      writer.write("start vertex,\tneighbor size,\tquery time (in ms)\n");
      writer.flush();

      double totalTime = 0.0;
      long totalKNsize = 0;
      double count = 0.0; 
      int errorQuery = 0;
      boolean normal = true;
      /*
      * read vertex id from seed
      * run KN query
      * calculate the k-hop distinct neighbor size and query time
      */
      String[] roots = line.split(" ");
      for(String root:roots) {
	count ++;

        long[] queryResult = sendQuery(root, depth);

        // handle query timeout, http error, and normal query
        if(queryResult[0] == -1) {
	  normal = false; 
	  errorQuery ++;
	}else {
	  normal = true;
	  totalKNsize += queryResult[0];
	  totalTime += queryResult[1];
	}
	writer.write(root + ",\t" + String.valueOf(queryResult[0])
	  + ",\t" + String.valueOf(queryResult[1]) + "\n");
	System.out.println(count + "\t" + root + "\t" + normal);
  
        if(count % 10 == 0.0 || count == seedCount){
          writer.flush();
	  if(count == seedCount){
	    break;
	  }
        }
      }
      
      writer.write("====================================================\n");
      writer.write("number of start vertex:\t" + (long) count + "\n"
	+ "number of query didn't finish correctly:\t" + errorQuery + "\n"
	+ "total neighbor size:\t" + totalKNsize + "\n"
        + "total query time:\t" + totalTime + "\n"
	+ "average neighbor size:\t" + totalKNsize/(count - errorQuery) + "\n"
	+ "average query time:\t" + totalTime/(count - errorQuery) + "\n");
   
      // print final result to screen

      System.out.println("====================================================\n"
        + "number of start vertex:\t" + (long) count + "\n"
        + "total neighbor size:\t" + totalKNsize + "\n"
        + "total query time:\t" + totalTime + "\n"
        + "average neighbor size:\t" + totalKNsize/(count - errorQuery) + "\n"
        + "average query time:\t" + totalTime/(count - errorQuery) + "\n");

 
      writer.flush();
      writer.close();	
    }catch(Exception e){
      e.printStackTrace();
    }

  }


  /**
   * This function opens a http connection and returns the output from server.
   *
   * @param url the url to connect.
   * @param method the request method, can be GET, POST, DELETE
   * @param data the data that need to be put into the http request output stream, it can be null
   *     which indicate nothing to be put.
   * @param headers, the http headers to be used in the curl request.
   * @return the output from server, or null if get errors.
   */

  public static String SendHttpConnection(String url, String method, String data, HashMap<String,
      String> headers) {

    try {

      //1. open connection
      HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();

      // set headers, e.g. user auth token
      headers.forEach((k, v) -> conn.setRequestProperty(k, v));
      conn.setRequestMethod(method);
      conn.setDoInput(true);
	
      //2. write data to the connect if needed
      if (data != null) {
        conn.setDoOutput(true);

        //Send request
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        wr.writeBytes(data);
        wr.flush();
        wr.close();
      }

      //3. get response
      if (conn.getResponseCode() != 200) {
        throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode()
	 + " error message : " + conn.getResponseMessage());
      }

      BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
      String ret = "";
      String output;
      while ((output = br.readLine()) != null) {
        ret += output;
      }

      //4. close connection
      conn.disconnect();
      return ret;
    }catch (ConnectException connectFailed) {
      try{
        Thread.sleep(70000);
        return null;
      }catch(InterruptedException e){
        e.printStackTrace();
        return null;
      }
     } catch(Exception e) {
      e.printStackTrace();
      return null;
    }
  }


  /** This function send K steps query and return the k-hop distinct neighbor size and query time
  * @param id the start vertex
  * @param depth number of steps 
  */
  public static long[] sendQuery(String id, String depth){
    try{
      String url = URL;
      String method = "POST";

      JSONObject json = new JSONObject();
      json.put("gremlin", "g.V(\"" + id +"\").repeat(out()).times(" + depth
        + ").dedup().count()");
	
      String data = json.toString();
      HashMap<String, String> headers = new HashMap<String, String>();
      headers.put("Accept", "application/json");
     
      long startTime = System.nanoTime();
      String responseInfo = SendHttpConnection(url, method, data, headers);
      long endTime = System.nanoTime();
      long duration = (endTime - startTime)/1000000;
      
      // check query timeout or not

      JSONObject obj = new JSONObject(responseInfo);
      if(!obj.has("code")) {
        long neighborSize = getNeighborSize(responseInfo);
        long[] result = new long[]{neighborSize, duration}; 
        return result;
      }
    }catch(Exception e){
      e.printStackTrace();
    }
    // the query didn't finish correctly
    return new long[]{-1, -1};
  }

  /* parse K-hop distinct neighbor size from the output in Json format */
  public static long getNeighborSize(String str) {
    JSONObject obj = new JSONObject(str);
    JSONObject objtmp = obj.getJSONObject("result");
    JSONObject objtmp1 = objtmp.getJSONObject("data");
    JSONArray array = objtmp1.getJSONArray("@value");
    long result = array.getJSONObject(0).getLong("@value");
    return result;
  }

}

