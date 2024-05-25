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
import java.util.ArrayList;
import java.util.List;

/** Parse response from TigerGraph server, and return a JSONObject List. */
public class RestppResponse {

  private static final Logger logger = TGLoggerFactory.getLogger(RestppResponse.class);

  private Integer code;
  private String reason_phase;
  private Boolean has_error;
  private String errCode;
  private String errMsg;
  private String content;
  private String token; // only for 'POST /requesttoken'
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

    // Check both of HTTP code and RESTPP code
    if (this.code != HttpStatus.SC_OK || has_error) {
      if (panic_on_fail) {
        String restResp = has_error ? (errCode == null ? "" : errCode + ": ") + errMsg : content;
        throw new SQLException(
            "Failed to send request: "
                + String.valueOf(this.code)
                + " - "
                + this.reason_phase
                + ". "
                + restResp);
      }
    }
  }

  public void parse(String content) {
    JSONObject obj;
    obj = new JSONObject(content);
    this.token = obj.optString("token", "");
    this.has_error = obj.optBoolean("error", false);
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

  public String getToken() {
    return this.token;
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
