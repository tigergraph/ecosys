package com.tigergraph.jdbc.restpp.driver;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parse response from TigerGraph server, and return a JSONObject List.
 */
public class RestppResponse {
  private Integer code;
  private Boolean is_error;
  private String errCode;
  private String errMsg;
  private List<JSONObject> results;
  private Boolean debug = Boolean.FALSE;

  /**
   * For unit test only.
   */
  public RestppResponse() {
    this.code = HttpStatus.SC_OK;
  }

  public RestppResponse(HttpResponse response, Boolean debug) throws SQLException {
    if (response.getStatusLine() == null) {
      throw new SQLException("Received response with no status code.");
    }

    this.debug = debug;
    this.code = response.getStatusLine().getStatusCode();

    if (this.code != HttpStatus.SC_OK) {
      throw new SQLException("Failed to send http request: " + String.valueOf(this.code));
    }

    HttpEntity entity = response.getEntity();
    if (null == entity) {
      throw new SQLException("Received no entity.");
    }

    try {
      String content = EntityUtils.toString(entity);
      if (debug) {
        System.out.println(">>> Response: " + content);
      }
      parse(content);
    } catch (IOException e) {
      throw new SQLException(e);
    }
  }

  public void parse(String content) {
    JSONObject obj;
    obj = new JSONObject(content);
    this.is_error = obj.getBoolean("error");
    this.results = new ArrayList<>();
    if (this.is_error) {
      this.errMsg = obj.getString("message");
      /**
       * Some queries' response do not have "error code".
       */
      // this.errCode = obj.getString("code");
    } else {
      JSONArray resultList = obj.getJSONArray("results");
      for(int i = 0; i < resultList.length(); i++){
        this.results.add(resultList.getJSONObject(i));
      }
    }
  }

  public Boolean hasError() {
    return this.is_error;
  }

  public Boolean hasResultSets() {
    return (this.results != null);
  }

  public String getErrMsg() {
    return this.errMsg;
  }

  public List<JSONObject> getResults() {
    return this.results;
  }

  public void addResults(List<JSONObject> results) {
    this.results.addAll(results);
  }

  /**
   * For unit test only.
   */
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for(int i = 0; i < this.results.size(); i++){
      sb.append(String.valueOf(this.results.get(i)));
      sb.append("\n");
    }
    return sb.toString();
  }
}

