/**
* Copyright (c)  2015-now, TigerGraph Inc.
* All rights reserved
* It is provided as sample code to use RESTPP API purpose.
* anyone can use it for education purpose with the
* acknowledgement to TigerGraph.
* Author: Litong Shen litong.shen@tigergraph.com
*/
import org.json.JSONObject;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.*;
import java.io.IOException;
import java.lang.ProcessBuilder;
import java.net.ConnectException;

/**
 * RESTPP DELETE method sample code
 * 
 * Sending Linux curl command to submit the HTTP request
 * to the REST++ server to delete vertex/edge
 */
public class DeleteSampleCode {
  static String URL = "http://127.0.0.1:9000/";

  public static void main(String[] args) {
    try {
      /*
       modify deleteInfo
       DELETE vertices - Syntax for deleteInfo: 
       graph/{graph_name}/vertices/{vertex_type}[/{vertex_id}]

       DELETE edges - Syntax for deleteInfo: 
       graph/{graph_name}/edges/{source_vertex_type}/{source_vertex_id}[/{edge_type}[/{target_vertex_type}[/{target_vertex_id}]]]
      */

      //String deleteInfo = "graph/social/edges/person/Amanda/friendship";
      String deleteInfo = "graph/social/vertices/person/Amanda";
      sendHTTPRequest(deleteInfo);

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
      } catch (InterruptedException e) {
	e.printStackTrace();
	return null;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }


  /** 
   * This function send HTTP request to REST++ server to delete vertex/edge
   * will print out response information from server to screen in json format
   *
   * @param deleteInfo detail info of deleting vertex/edge  
   */
  public static void sendHTTPRequest(String deleteInfo) {
    try {
      String url = URL + deleteInfo;
      String method = "DELETE";
	
      String data = ""; 
      HashMap<String, String> headers = new HashMap<String, String>();
      headers.put("Accept", "application/json");
     
      String responseInfo = SendHttpConnection(url, method, data, headers);

      JSONObject obj = new JSONObject(responseInfo);
      if (!obj.has("results")) {
	parsingErrorMessage(obj);
      } else {
        System.out.println("DELETE operation succeed \n");
      }
      System.out.println("responseInfo is: \n" + responseInfo);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * This function parse error information form response message send back 
   * from REST++ SERVER
   *
   * @param obj resonse info from server in json format
   */
  public static void parsingErrorMessage(JSONObject obj) {
    String errorMessage = obj.getString("message");
    System.out.println("DELETE operation failed because: \n" + errorMessage);
  }

}
