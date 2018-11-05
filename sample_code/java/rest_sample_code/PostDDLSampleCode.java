/**
* Copyright (c)  2015-now, TigerGraph Inc.
* Licensed under the Apache License v2.0
* http://www.apache.org/licenses/LICENSE-2.0
*
* Author: Litong Shen litong.shen@tigergraph.com
*/
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.*;
import java.io.IOException;
import java.lang.ProcessBuilder;
import java.net.ConnectException;

/**
 * RESTPP POST method sample code using DDL loader
 * 
 * Sending Linux curl command to submit the HTTP request
 * to the REST++ server to upsert(update and insert) vertex/edge
 */
public class PostDDLSampleCode {
  static String URL = "http://127.0.0.1:9000/";
  static String postInfo;
  static String data;
  public static void main(String[] args){
    try {
      // type is vertex or edge
      String type = args[0];
      // graph name
      String graphName = args[1];
      // ddl loading job name
      String loadingJobName = args[2];
      // file name declared in ddl loading job
      String fileName = args[3];
      // separator of CSV file
      String separator = args[4];
      // end of line charactor
      String endOfLineChar = args[5];

      /*
         upsert vertices - Syntax for postInfo:
         ddl/{graph_name}?tag={loading_job_name}&filename={post_vertex_file_name}&sep={separator_of_CSV_file}&eol={end_of_line_charactor}

         upsert edges - Syntax for postInfo:
         ddl/{graph_name}?tag={loading_job_name}&filename={post_edge_file_name}&sep={separator_of_CSV_file}&eol={end_of_line_charactor}
      */

      postInfo = "ddl/" + graphName+ "?tag=" + loadingJobName + "&filename=" 
	      + fileName + "&sep=" + separator + "&eol=" + endOfLineChar;

      if (args.length > 6) {
        // upsert vertex/edge stored in separate data file
	
        String postFilePath = args[6];
	data = new String(Files.readAllBytes(Paths.get(postFilePath)));
      } else { 
	// upsert vertex/edge use inline method
	
        //end_of_line_charactordata = "Amanda,Amanda,20,female,ca;Leo,Leo,23,male,ca";
        data = "Amanda,Leo,2011-08-08;Amanda,Tom,2016-03-05";
      }
      sendHTTPRequest(postInfo, data, type);
    } catch(Exception e) {
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
        BufferedReader reader = new BufferedReader(new InputStreamReader((conn.getErrorStream())));
        String errorResult = "";
        String errorOutput;
        while ((errorOutput = reader.readLine()) != null) {
          errorResult += errorOutput;
        }

        throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode()
			+"\n connection error info :\n" + errorResult);
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
    } catch (ConnectException connectFailed) {
      try{
	Thread.sleep(70000);
	return null;
      } catch(InterruptedException e) {
	e.printStackTrace();
	return null;
      }
    } catch(Exception e) {
      e.printStackTrace();
      return null;
    }
  }


  /** 
   * This function send HTTP request to REST++ server to upsert vertex/edge
   * will print out response information from server to screen in json format
   *
   * @param postInfo specify tag, filename, sep, eol infomation
   * @param data vertex/edge details upserting to graph
   */
  public static void sendHTTPRequest(String postInfo, String data, String type){
    try {
      String url = URL + postInfo;
      String method = "POST";

      HashMap<String, String> headers = new HashMap<String, String>();
      headers.put("Accept", "application/json");
     
      String responseInfo = SendHttpConnection(url, method, data, headers);
      
      // check upsert status
      Boolean upsertError = upsertStatus(responseInfo, type);
      if (upsertError) {
        System.out.println("upsert " + type + " failed");
      } else {
        System.out.println("upsert " + type + " succeed");
      }

	System.out.println("response information : ");
	System.out.print(responseInfo);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * This function checking the response info send back from server 
   * whether the upsert request succeed or failed
   *
   * @param responseInfo response information from server
   * @param type upserting vertex or edge
   */
  public static Boolean upsertStatus(String responseInfo, String type) {
    JSONObject obj = new JSONObject(responseInfo);                                                  
    if (!obj.has("reports")) {	 
      return true;
    }
    JSONArray objArray = obj.getJSONArray("reports");                                               
    JSONObject obj1 = objArray.getJSONObject(0);                                                    
    JSONObject obj2 = obj1.getJSONObject("statistics");                                             
    long validLine = obj2.getLong("validLine");
    if (validLine == 0) {
      return true;
    }
    JSONArray vertexInfo = obj2.getJSONArray(type);
    long validObject = vertexInfo.getJSONObject(0).getLong("validObject");
    if (validObject == 0) {
      return true;
    }
    return false;
  }
}
