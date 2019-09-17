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
  private Integer debug = 0;

  /**
   * For unit test only.
   */
  public RestppResponse() {
    this.results = new ArrayList<>();
    this.code = HttpStatus.SC_OK;
  }

  public RestppResponse(HttpResponse response, Boolean panic_on_fail,
      Integer debug) throws SQLException {
    this.results = new ArrayList<>();

    if (response.getStatusLine() == null) {
      if (panic_on_fail) {
        throw new SQLException("Received response with no status code.");
      } else {
        return;
      }
    }

    this.debug = debug;
    this.code = response.getStatusLine().getStatusCode();

    if (this.code != HttpStatus.SC_OK) {
      if (panic_on_fail) {
        throw new SQLException("Failed to send http request: " + String.valueOf(this.code));
      } else {
        return;
      }
    }

    HttpEntity entity = response.getEntity();
    if (null == entity) {
      if (panic_on_fail) {
        throw new SQLException("Received no entity.");
      } else {
        return;
      }
    }

    try {
      String content = EntityUtils.toString(entity);
      if (debug > 0) {
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
    if (this.is_error) {
      this.errMsg = obj.getString("message");
      /**
       * Some queries' response do not have "error code".
       */
      // this.errCode = obj.getString("code");
    } else {
      Object value = obj.get("results");
      if (value instanceof JSONObject) {
        this.results.add((JSONObject)value);
      } else if (value instanceof JSONArray) {
        JSONArray resultList = (JSONArray)value;
        for(int i = 0; i < resultList.length(); i++){
          this.results.add(resultList.getJSONObject(i));
        }
      }
    }
  }

  public Boolean hasError() {
    return this.is_error;
  }

  public Boolean hasResultSets() {
    return (this.results.size() > 0);
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
