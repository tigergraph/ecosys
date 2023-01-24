package com.tigergraph.jdbc.restpp.driver;

import com.tigergraph.jdbc.log.TGLoggerFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.slf4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.sql.SQLTransientException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

/** Parse response from TigerGraph server, and return a JSONObject List. */
public class RestppResponse {

  private static final Logger logger = TGLoggerFactory.getLogger(RestppResponse.class);
  private static final Set<Integer> TRANSIENT_FAILURE_CODE =
      new HashSet<Integer>(
          Arrays.asList(
              HttpStatus.SC_REQUEST_TIMEOUT,
              HttpStatus.SC_INTERNAL_SERVER_ERROR,
              HttpStatus.SC_BAD_GATEWAY,
              HttpStatus.SC_SERVICE_UNAVAILABLE,
              HttpStatus.SC_GATEWAY_TIMEOUT));

  private Integer code;
  private String reason_phase;
  private Boolean has_error;
  private String errCode;
  private String errMsg;
  private String content;
  private List<JSONObject> results;

  /** For unit test only. */
  public RestppResponse() {
    this.results = new ArrayList<>();
    this.code = HttpStatus.SC_OK;
    this.content = "";
    this.has_error = Boolean.FALSE;
  }

  public RestppResponse(HttpResponse response, Boolean panic_on_fail) throws SQLException {
    this.results = new ArrayList<>();
    this.code = HttpStatus.SC_OK;
    this.content = "";
    this.has_error = Boolean.FALSE;

    // Some responses have no status code, but still have entities.
    if (response.getStatusLine() == null) {
      if (panic_on_fail) {
        throw new SQLException("Received response with no status code.");
      }
    } else {
      this.code = response.getStatusLine().getStatusCode();
      this.reason_phase = response.getStatusLine().getReasonPhrase();
    }

    HttpEntity entity = response.getEntity();
    if (null != entity) {
      String content = "";
      try {
        content = EntityUtils.toString(entity);
        parse(content);
      } catch (JSONException e) {
        // Not a json, save the content directly.
        this.content = content;
      } catch (IOException e) {
        throw new SQLException(e);
      }
    } else {
      if (panic_on_fail) {
        throw new SQLException(this.code + ", received no entity.");
      } else {
        return;
      }
    }

    // Some responses' status codes are not 200, but still have entities.
    if (this.code != HttpStatus.SC_OK) {
      if (panic_on_fail) {
        if (TRANSIENT_FAILURE_CODE.contains(this.code)) {
          throw new SQLTransientException(
              "Failed to send request: "
                  + String.valueOf(this.code)
                  + ": "
                  + this.reason_phase
                  + ". "
                  + content);
        } else {
          throw new SQLNonTransientException(
              "Failed to send request: "
                  + String.valueOf(this.code)
                  + ": "
                  + this.reason_phase
                  + ". "
                  + content);
        }
      }
    }
  }

  public void parse(String content) {
    JSONObject obj;
    obj = new JSONObject(content);
    this.has_error = obj.getBoolean("error");
    if (this.has_error) {
      this.errMsg = obj.getString("message");
      /** Some queries' response do not have "error code". */
      if (obj.has("code")) {
        this.errCode = obj.getString("code");
      }
    } else {
      Object value = obj.get("results");
      if (value instanceof JSONObject) {
        this.results.add((JSONObject) value);
      } else if (value instanceof JSONArray) {
        JSONArray resultList = (JSONArray) value;
        for (int i = 0; i < resultList.length(); i++) {
          this.results.add(resultList.getJSONObject(i));
        }
      }
    }
  }

  public Boolean hasError() {
    return this.has_error;
  }

  public Boolean hasResultSets() {
    return (this.results.size() > 0);
  }

  public String getErrMsg() {
    return this.errMsg;
  }

  public String getErrCode() {
    return this.errCode;
  }

  public Integer getCode() {
    return this.code;
  }

  public String getContent() {
    return this.content;
  }

  public List<JSONObject> getResults() {
    return this.results;
  }

  public void addResults(List<JSONObject> results) {
    this.results.addAll(results);
  }

  /** For unit test only. */
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < this.results.size(); i++) {
      sb.append(String.valueOf(this.results.get(i)));
      sb.append(System.lineSeparator());
    }
    return sb.toString();
  }
}
