package com.tigergraph.jdbc.restpp.driver;

import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import org.apache.http.util.EntityUtils;
import java.io.IOException;
import java.sql.SQLException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

/**
 * Parse the raw query, and build a http request.
 */
public class QueryParser {

  public enum HttpTypes {
    HttpGet, HttpPost, HttpDelete
  }

  private String[] paramArray;
  private Boolean debug = Boolean.FALSE;
  private HttpRequestBase request;
  private String endpoint;
  private StringEntity payload;
  private HttpTypes httpType;
  private Boolean is_bultin_query = Boolean.FALSE;
  private Boolean is_run_query = Boolean.FALSE;

  /**
   * Convert a map to an array, use "key" as index.
   */
  private void map2Array(Map<String, Object> parameters) {
    this.paramArray = new String[parameters.size()];
    Iterator<Entry<String, Object>> iter = parameters.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<String, Object> entry = iter.next();
      Integer index = Integer.valueOf(entry.getKey());
      this.paramArray[index] = String.valueOf(entry.getValue());
    }
  }

  /**
   * Remove empty string in an array.
   */
  private String[] rmNullInArray(String[] strArray) {
    List<String> strListNew = new ArrayList<>();
    for (int i = 0; i < strArray.length; i++) {
      if (strArray[i] != null && !strArray[i].equals("")) {
        strListNew.add(strArray[i]);
      }
    }
    return strListNew.toArray(new String[strListNew.size()]);
  }

  /**
   * Parse queries to get vertices and edges.
   * Reference: https://docs.tigergraph.com/dev/restpp-api/built-in-endpoints
   */
  private void parseEdgeAndVertex(String[] tokens) throws SQLException {
    StringBuilder sb = new StringBuilder();

    if (tokens.length <= 1) {
      throw new SQLException("Invalid vertex or edge query.");
    }

    if (tokens[1].toLowerCase().equals("edges")) {
      /**
       * Query syntax:
       * get edges(src_vertex_type, src_vertex_id)
       */
      sb.append("edges/").append(tokens[2]);
      if (this.paramArray.length == 1) {
        sb.append("/").append(this.paramArray[0]);
      } else {
        throw new SQLException("Parameter number not match for edge query.");
      }
    } else if (tokens[1].toLowerCase().equals("edge")) {
      /**
       * Query syntax:
       * get edge(src_vertex_type, src_vertex_id, edge_type, tgt_vertex_type, tgt_vertex_id)
       */
      if (tokens.length == 7 && this.paramArray.length == 2) {
        sb.append("edges/").append(tokens[2]);
        sb.append("/").append(this.paramArray[0]);
        sb.append("/").append(tokens[4]);
        sb.append("/").append(tokens[5]);
        sb.append("/").append(this.paramArray[1]);
      } else {
        throw new SQLException("Parameter number not match for edge query.");
      }
    } else {
      /**
       * Sample query:
       * get/delete vertex_type(filter=?)
       */
      sb.append("vertices/").append(tokens[1]);
      if ((null == this.paramArray) || (this.paramArray.length == 0)) {
        /**
         * Delete all vertices of given type
         */
        if (tokens.length == 2) {
          this.endpoint = sb.toString();
          return;
        } else {
          throw new SQLException("Parameter number not match for vertex query.");
        }
      }

      if (tokens.length != 4 || this.paramArray.length != 1) {
        throw new SQLException("Invalid vertex query.");
      }
      if (tokens[2].toLowerCase().equals("id")) {
        sb.append("/").append(this.paramArray[0]);
      } else if (tokens[2].toLowerCase().equals("filter")) {
        sb.append("?filter=").append(this.paramArray[0]);
      } else if (tokens[2].toLowerCase().equals("limit")) {
        sb.append("?limit=").append(this.paramArray[0]);
      } else {
        throw new SQLException("Invalid vertex query.");
      }
    }
    this.endpoint = sb.toString();
  }

  /**
   * Build endpoint based on raw query and parameters.
   */
  public QueryParser(String query, Map<String, Object> parameters, Boolean debug) throws SQLException {
    if ((query == null) || query.equals("")) {
      throw new SQLException("Query could not be null or empty.");
    }
  
    if (parameters != null) {
      map2Array(parameters);
      if (debug) {
        System.out.println(">>> parameters: " + Arrays.toString(this.paramArray));
      }
    }

    /**
     * Tokenize the raw query and remove empty items
     */
    this.debug = debug;
    String[] tokens = query.trim().split(" |,|\\(|\\)|=");
    tokens = rmNullInArray(tokens);
    if (debug) {
      System.out.println(">>> tokens: " + Arrays.toString(tokens));
    }

    /**
     * Start to parse query
     */
    if (tokens[0].toLowerCase().equals("get")) {
      /**
       * It is a query to get certain vertex or edge
       * e.g., get Block(limit=?)"
       */
      this.httpType = HttpTypes.HttpGet;
      parseEdgeAndVertex(tokens);
    } else if (tokens[0].toLowerCase().equals("delete")) {
      /**
       * It is a query to delete certain vertex or edge
       * e.g., delete Block(id=?)"
       */
      this.httpType = HttpTypes.HttpDelete;
      parseEdgeAndVertex(tokens);
    } else if (tokens[0].toLowerCase().equals("builtins")) {
      /**
       * It is a builtin query.
       * e.g., "builtins stat_vertex_number(type=?)"
       */
      this.httpType = HttpTypes.HttpPost;
      this.is_bultin_query = Boolean.TRUE;
      if (tokens.length < 2) {
        throw new SQLException("Need to specify builtins function name.");
      }
      StringBuilder sb = new StringBuilder();
      sb.append("{\"function\":\"");
      sb.append(tokens[1]);
      sb.append("\", \"type\":\"");
      if (tokens.length >= 4) {
        if (tokens[3].trim().equals("?")) {
          if (parameters.size() != 1) {
            throw new SQLException("Parameter size not match (expected 1 parameter): "
                + String.valueOf( parameters.size()));
          }
          sb.append(this.paramArray[0]);
          sb.append("\"");
        } else {
          sb.append(tokens[4]);
          sb.append("\"");
        }
      } else {
        sb.append("*\"");
      }
      sb.append("}");
      this.payload = new StringEntity(sb.toString(), ContentType.APPLICATION_JSON);
    } else if (tokens[0].toLowerCase().equals("run")) {
      /**
       * It is a pre-installed query.
       * e.g., "run pageRank(maxChange=?, maxIteration=?, dampingFactor=?)"
       */
      this.is_run_query = Boolean.TRUE;
      this.httpType = HttpTypes.HttpGet;
      StringBuilder sb = new StringBuilder();
      sb.append(tokens[1]).append("?");
      Integer size = this.paramArray.length;
      if (tokens.length != (2 + 2 * size)) {
        throw new SQLException("Token size and parameter size not match : "
            + String.valueOf(tokens.length) + String.valueOf(parameters.size()));
      }

      /**
       * Parse parameters.
       */
      Integer index = 2;
      for (Integer i = 0; i < size; ++i) {
        sb.append(tokens[index]).append("=").append(this.paramArray[i]);
        assert tokens[index+1].trim().equals("?") : "Place holder not equals ?";
        index += 2;
        if (i < size - 1) {
          sb.append("&");
        }
      }
      endpoint = sb.toString();
    }
  }

  /**
   * Build a http request based on endpoint, query type and other info.
   */
  public HttpRequestBase buildQuery(String host, Integer port, Boolean secure, String graph, String token) throws SQLException {
    HttpRequestBase request;
    StringBuilder sb = new StringBuilder();
    if (this.is_bultin_query) {
      sb.append("/builtins");
    } else {
      if (this.is_run_query) {
        sb.append("/query");
      } else {
        sb.append("/graph");
      }
      if (graph != null && !graph.equals("")) {
        sb.append("/").append(graph);
      }
      sb.append("/").append(this.endpoint);
    }

    String url = "";
    try {
      if (secure)
        url = new URL("https", host, port, sb.toString()).toString();
      else
        url = new URL("http", host, port, sb.toString()).toString();
    } catch (MalformedURLException e) {
      throw new SQLException("Invalid server URL", e);
    }

    switch (this.httpType) {
      case HttpGet:
        if (debug) {
          System.out.println(">>> request: " + url.toString());
        }
        request = new HttpGet(url);
        break;
      case HttpPost:
        HttpPost post = new HttpPost(url);
        post.setEntity(this.payload);
        Header[] headers = post.getAllHeaders();
        if (debug) {
          try {
            String content = EntityUtils.toString(payload);
            System.out.println(">>> request: " + post.toString());
            for (Header header : headers) {
              System.out.println(">>>> " + header.getName() + ": " + header.getValue());
            }
            System.out.println(">>>> " + content);
          } catch (IOException e) {
            throw new SQLException("Failed to convert EntityUtils to string", e);
          }
        }
        request = post;
        break;
      case HttpDelete:
        request = new HttpDelete(url);
        break;
     default:
       throw new SQLException("Invalid http request type.");
    }

    request.addHeader("Accept", ContentType.APPLICATION_JSON.toString());
    if (token != null && !token.equals("")) {
      request.addHeader("Authorization", "Bearer " + token);
    }

    return request;
  }

  /**
   * For unit test only.
   */
  public String getEndpoint() {
    return this.endpoint;
  }

  /**
   * For unit test only.
   */
  public String getPayload() throws IOException {
    return EntityUtils.toString(this.payload);
  }

}
