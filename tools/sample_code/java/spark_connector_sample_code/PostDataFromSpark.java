/**
* Copyright (c)  2015-now, TigerGraph Inc.
* Licensed under the Apache License v2.0
* http://www.apache.org/licenses/LICENSE-2.0
*
* Author: Litong Shen litong.shen@tigergraph.com
*/
import java.io.*;
import java.util.*;
import java.lang.StringBuffer;
import java.lang.StringBuilder;
import java.lang.ProcessBuilder;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.ConnectException;

import scala.collection.JavaConversions;
import scala.collection.Seq;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;
import org.json.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.nio.file.Files;
import java.nio.file.Paths;
/**
 * this is the sample code for posting data, which were stored in Spark
 * through TigerGraph REST endpoint using DDL loader 
 *
 */
public class PostDataFromSpark {
  static String inputPath;
  static String URL = "http://127.0.0.1:9000/";
  static String postInfo;

  public static void main(String[] args) {
    // type is vertex or edge
    String type = args[0];
    // graph name
    String graphName = args[1];
    // ddl loading job name
    String loadingJobName = args[2];
    // file name declared in ddl loading job
    String fileName = args[3];
    // file separator
    String separator = args[4];
    // end of line charactor
    String endOfLineChar = args[5];
    // path to input data
    inputPath = args[6];

    postInfo = "ddl/" + graphName+ "?tag=" + loadingJobName + "&filename="
              + fileName + "&sep=" + separator + "&eol=" + endOfLineChar;

    // create a spark session
    SparkSession sc = SparkSession.builder().getOrCreate();

    StringBuilder sbData = new StringBuilder();
    List<Row> arrayList= new ArrayList<>();

    // read input file to spark
    Dataset<Row> dataInSpark = sc.read().csv(inputPath);
    arrayList = dataInSpark.collectAsList();

    // convert Dataset<Row> to string, which is the payload info to post into tigergraph
    for(Row cur : arrayList) {
      String tmp = cur.toString();
      sbData.append(tmp.substring(1, tmp.length() - 1) + "\n");

    }
    String data = sbData.toString();
    sendHTTPRequest(postInfo, data, type);
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
      System.out.println("################################################################");
      if (upsertError) {
        System.out.println("upsert " + type + " failed");
      } else {
        System.out.println("upsert " + type + " succeed");
      }

	System.out.println("response information : ");
	System.out.print(responseInfo);
      System.out.println("################################################################");
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
    try{
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
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
}
