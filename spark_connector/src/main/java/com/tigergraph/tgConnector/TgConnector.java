package com.tigergraph.tgConnector;

import java.io.*;
import java.util.*;
import java.lang.StringBuffer;
import java.lang.StringBuilder;
import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;

import org.json.JSONObject;

public class TgConnector implements Serializable {

  String tg_ip;  //tigergraph server ip
  String tg_port;  //tigergraph server restpp port
  String tg_user;  // tigergraph server user

  public TgConnector(String tg_ip, String tg_port, String tg_user) {
    this.tg_ip = tg_ip;
    this.tg_port = tg_port;
    this.tg_user = tg_user;
  }

  /**
  * This function will take a query endpoint with parameters.
  * It makes the REST call and store the result in a JSONObject.
  */
  public JSONObject getJsonForQuery(String query_endpoint) {
    String response = "";
    try {
      String requestUrl = "http://" + this.tg_ip + ":" + this.tg_port +
          "/query/" + query_endpoint;
      response = sendRequest(requestUrl, "GET", "");
    } catch (Exception e) {
      throw e;
    }
    return new JSONObject(response);
  }

  public JSONObject getJsonForPost(String post_endpoint, String payload) {
    String response = "";
    try {
      String requestUrl = "http://" + this.tg_ip + ":" + this.tg_port +
          "/ddl?sep=,&tag=" + post_endpoint + "&eol=%0A&filename=f";
      response = sendRequest(requestUrl, "POST", payload);
    } catch (Exception e) {
      throw e;
    }
    return new JSONObject(response);
  }

  /**
  * This function is to copy file from TG server to local
  * SSH connection must be set between this machine and remote TG server
  */
  public String copyFileToLocal(String remote_file, String local_file) {
    return runBashCmd("scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no " +
        this.tg_user + "@" + this.tg_ip + ":" + remote_file + " " + local_file);
  }

  /**
  * This function is to copy file from TG server to local, using the ssh private key.
  */
  public String copyFileToLocal(String remote_file, String local_file, String key_file) {
    return runBashCmd("scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i " +
        key_file + " " + this.tg_user + "@" + this.tg_ip + ":" + remote_file + " " + local_file);
  }

  /*
  * This function is to run bash command and return its output.
  */
  private static String runBashCmd(String cmd) {
    String output = "";
    try {
      Process p = Runtime.getRuntime().exec(cmd);
      p.waitFor();
      BufferedReader buf = new BufferedReader(new InputStreamReader(
              p.getInputStream()));
      String line = "";

      while ((line = buf.readLine()) != null) {
        output += line + "\n";
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
    return output;
  }

  /*
  *  Send REST request to requestUrl with payload and get the response string.
  *
  */
  private static String sendRequest(String requestUrl, String method, String payload) {
   StringBuffer sb = new StringBuffer();
   try {
     URL url = new URL(requestUrl);
     HttpURLConnection connection = (HttpURLConnection) url.openConnection();

     connection.setDoInput(true);
     if (payload.isEmpty()) {
       connection.setDoOutput(false);
     } else {
       connection.setDoOutput(true);
     }
     connection.setRequestMethod(method);
     connection.setRequestProperty("Accept", "application/json");
     connection.setRequestProperty("Content-Type", "application/json; charset = UTF-8");
     if (!payload.isEmpty()) {
       OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
       writer.write(payload);
       writer.close();
     }
     BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
     String line = null;
     while ((line = br.readLine()) != null) {
       sb.append(line);
     }
     br.close();
   } catch (Exception e) {
     throw new RuntimeException(e.getMessage());
   }
   return sb.toString();
 }
}
