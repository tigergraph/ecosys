package com.tigergraph.jdbc.restpp.driver;

import com.tigergraph.jdbc.restpp.RestppConnection;
import com.tigergraph.jdbc.restpp.driver.QueryType;
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
import javax.json.Json;
import javax.json.JsonValue;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Map.Entry;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

/**
 * Parse the raw query, and build a http request.
 */
public class QueryParser {

  public enum HttpTypes {
    HttpGet, HttpPost, HttpDelete
  }

  private Object[] paramArray;
  private Integer debug = 0;
  private HttpRequestBase request;
  private String endpoint = "";
  private String query = "";
  private String edge_json = "";
  private String vertex_json = "";
  private String vertex_edge_type = "";
  private StringEntity payload;
  private HttpTypes httpType;
  private QueryType query_type;
  private RestppConnection connection;
  private List<String> field_list_;
  private String line;
  private String job;
  private int timeout;

  // get index of a specified String (first occurrence) in an array,
  // return array length when not found.
  private int getIndexInArray(String[] tokens, String val) {
    for (Integer i = 0; i < tokens.length; ++i) {
      if (tokens[i].toLowerCase().equals(val)) {
        return i;
      }
    }
    return tokens.length;
  }

  private String urlEncode(String s) throws SQLException {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new SQLException("Failed to encode URL: ", e);
    }
  }

  private String getObjectStr(Object o) {
    if (o instanceof String) {
      return (String) o;
    } else if (o instanceof Integer) {
      return String.valueOf(o);
    } else {
      return String.valueOf(o);
    }
  }

  /**
   * Convert a map to an array, use "key" as index.
   */
  private void map2Array(Map<Integer, Object> parameters) {
    this.paramArray = new Object[parameters.size()];
    Iterator<Entry<Integer, Object>> iter = parameters.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<Integer, Object> entry = iter.next();
      // PreparedStatement parameter indices start at 1
      Integer index = entry.getKey() - 1;
      /*
       * Cannot convert to String here, as some values are used as Json values,
       * which could be int, double or string.
       */
      this.paramArray[index] = entry.getValue();
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

  private static JsonObjectBuilder addValue(JsonObjectBuilder obj, String key, Object value) {
    if (value instanceof Integer) {
      obj.add(key, (Integer)value);
    }
    else if (value instanceof String) {
      obj.add(key, (String)value);
    }
    else if (value instanceof Float) {
      obj.add(key, (Float)value);
    }
    else if (value instanceof Double) {
      obj.add(key, (Double)value);
    }
    else if (value instanceof Boolean) {
      obj.add(key, (Boolean)value);
    }
    else if (value instanceof JsonValue) {
      JsonValue val = (JsonValue)value;
      obj.add(key, val);
    }
    return obj;
  }

  /**
   * Parse queries to get vertices and edges.
   * Reference: https://docs.tigergraph.com/dev/restpp-api/built-in-endpoints
   * Return values:
   * -1: not a valid query
   *  0: a vertex query
   *  1: an edge query
   */
  private int parseEdgeOrVertex(String[] tokens) throws SQLException {
    StringBuilder sb = new StringBuilder();
    int ret = -1;

    if (tokens.length <= 1) {
      throw new SQLException("Invalid vertex or edge query.");
    }

    if (tokens[1].toLowerCase().equals("edges")) {
      /**
       * Query syntax:
       * get edges(src_vertex_type, src_vertex_id)
       */
      ret = 1;
      sb.append("edges/").append(tokens[2]);
      if (this.paramArray.length == 1) {
        sb.append("/").append(getObjectStr(this.paramArray[0]));
      } else {
        throw new SQLException("Parameter number not match for edge query.");
      }
    } else if (tokens[1].toLowerCase().equals("edge")) {
      /**
       * Query syntax:
       * get edge(src_vertex_type, src_vertex_id, edge_type, tgt_vertex_type, tgt_vertex_id)
       */
      ret = 1;
      if (tokens.length == 7 && this.paramArray.length == 2) {
        sb.append("edges/").append(tokens[2]);
        sb.append("/").append(getObjectStr(this.paramArray[0]));
        sb.append("/").append(tokens[4]);
        sb.append("/").append(tokens[5]);
        sb.append("/").append(getObjectStr(this.paramArray[1]));
      } else {
        throw new SQLException("Parameter number not match for edge query.");
      }
    } else {
      /**
       * Sample query:
       * get/delete vertex_type(filter=?)
       */
      ret = 0;
      sb.append("vertices/").append(tokens[1]);
      if ((null == this.paramArray) || (this.paramArray.length == 0)) {
        /**
         * Delete all vertices of given type
         */
        if (tokens.length == 2) {
          this.endpoint = sb.toString();
          return ret;
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
        sb.append("?filter=").append(urlEncode(getObjectStr(this.paramArray[0])));
      } else if (tokens[2].toLowerCase().equals("limit")) {
        sb.append("?limit=").append(getObjectStr(this.paramArray[0]));
      } else {
        throw new SQLException("Invalid vertex query.");
      }
    }
    this.endpoint = sb.toString();
    return ret;
  }

  /**
   * Parse queries to upsert vertices or edges
   * e.g., INSERT INTO vertex Page(id, page_id) VALUES("1234", "1234")
   *       INSERT INTO EDGE Linkto(Page, Page, weight) VALUES("1234", "5678", 3)
   *       INSERT INTO job loadFollow ("Person","Person","weight") VALUES (?,?,?)
   */
  private void parseUpsertQuery(String[] tokens,
      Map<Integer, Object> parameters) throws SQLException {

    if (tokens.length < 2 ||
        !(tokens[1].toLowerCase().equals("into"))) {
      throw new SQLException("Invalid upsert query.\nA valid one should start with \"insert into\"");
    }

    // Sanity check on attributes' number
    Integer values_index = getIndexInArray(tokens, "values");
    if (values_index < 0) {
      throw new SQLException("Invalid upsert query.\nA valid one should contains \"VALUES\"");
    }
    Integer count = tokens.length - values_index - 1;
    if (count < 1) {
      throw new SQLException("Invalid upsert query.\nA valid one should contains one attribute at least.");
    }
    if (parameters != null && parameters.size() != count) {
      throw new SQLException("Numbers of attributes and parameters are not match.");
    }

    JsonObjectBuilder obj = Json.createObjectBuilder();
    // Offset of the first parameter.
    Integer param_offset = 4;
    if (tokens[2].toLowerCase().equals("job")) {
      // Spark is trying to invoke loading job
      // e.g., INSERT INTO job load_pagerank
      this.query_type = QueryType.QUERY_TYPE_LOAD_JOB;
      this.httpType = HttpTypes.HttpPost;
      String sep = this.connection.getSeparator();
      StringBuilder sb = new StringBuilder();
      Integer size = this.paramArray.length;
      sb.append(this.paramArray[0]);
      for (int i = 1; i < size; ++i) {
        sb.append(sep);
        sb.append(String.valueOf(this.paramArray[i]));
      }
      this.line = sb.toString();
      this.job = tokens[3]; // loading job name
    } else if (tokens[2].toLowerCase().equals("edge")) {
      // insert an edge
      // e.g., INSERT INTO edge Follow ("Person","Person","weight") VALUES (?,?,?)
      if (count != values_index - param_offset) {
        throw new SQLException("Numbers of attributes and values are not match.");
      }
      if (count < 2) {
        throw new SQLException("Upserting an edge needs two parameters at least.");
      }
      JsonObjectBuilder attrObj = Json.createObjectBuilder();
      if (parameters == null) { // Not a PrepareStatement
        // Parse edge attributes if any
        // the first 2 attributes are source vertex type and target vertex type
        for (Integer i = param_offset + 2; i < values_index; ++i) {
          attrObj.add(tokens[i],
            Json.createObjectBuilder().add("value", tokens[values_index + i - 3]));
        }
        obj.add(tokens[param_offset], // src vertex type
          Json.createObjectBuilder().add(tokens[values_index + 1], // src vertex id
            Json.createObjectBuilder().add(tokens[3], // edge type
              Json.createObjectBuilder().add(tokens[param_offset + 1], // tgt vertex type
                Json.createObjectBuilder().add(tokens[values_index + 2], // tgt vertex id
                  attrObj))))); // attributes
      } else { // It is a PrepareStatement
        // Parse attributes if any
        for (Integer i = param_offset + 2; i < values_index; ++i) {
          attrObj.add(tokens[i],
            addValue(Json.createObjectBuilder(), "value", this.paramArray[i - 4]));
        }
        obj.add(tokens[param_offset], // src vertex type
          Json.createObjectBuilder().add(getObjectStr(this.paramArray[0]), // src vertex id
            Json.createObjectBuilder().add(tokens[3], // edge type
              Json.createObjectBuilder().add(tokens[param_offset + 1], // tgt vertex type
                Json.createObjectBuilder().add(getObjectStr(this.paramArray[1]), // tgt vertex id
                  attrObj))))); // attributes
      }
      // someone may use single-quoted strings
      edge_json = obj.build().toString().replace("\'", "");
      // remove the outmost "{}"
      edge_json = edge_json.substring(1, edge_json.length() -1);
      if (debug > 1) {
        System.out.println(">>> upsert edge: " + edge_json);
      }
    } else if (tokens[2].toLowerCase().equals("vertex")) {
      // insert an vertex
      // e.g., INSERT INTO vertex Person ("id","account") VALUES (?,?)
      if (count != values_index - param_offset) {
        throw new SQLException("Numbers of attributes and values are not match.");
      }
      JsonObjectBuilder attrObj = Json.createObjectBuilder();
      if (parameters == null) { // Not a PrepareStatement
        // Parse attributes if any
        // the first attribute is primary id
        for (Integer i = param_offset + 1; i < values_index; ++i) {
          attrObj.add(tokens[i],
            Json.createObjectBuilder().add("value", tokens[values_index + i - 3]));
        }
        obj.add(tokens[3], // vertex type
          Json.createObjectBuilder().add(tokens[values_index + 1], // vertex id
            attrObj)); // attributes
      } else {
        // Parse attributes if any
        for (Integer i = param_offset + 1; i < values_index; ++i) {
          attrObj.add(tokens[i],
            addValue(Json.createObjectBuilder(), "value", this.paramArray[i - 4]));
        }
        obj.add(tokens[3], // vertex type
          Json.createObjectBuilder().add(getObjectStr(this.paramArray[0]), // vertex id
            attrObj)); // attributes
      }
      // someone may use single-quoted strings
      vertex_json = obj.build().toString().replace("\'", "");
      // remove the outmost "{}"
      vertex_json = vertex_json.substring(1, vertex_json.length() -1);
      if (debug > 1) {
        System.out.println(">>> upsert vertex: " + vertex_json);
      }
    } else {
      throw new SQLException("Invalid upsert query: " + tokens[2]);
    }
  }

  /**
   * Build endpoint based on raw query and parameters.
   */
  public QueryParser(RestppConnection con, String query, Map<Integer, Object> parameters,
      Integer debug, int timeout) throws SQLException {
    if ((query == null) || query.equals("")) {
      throw new SQLException("Query could not be null or empty.");
    }

    this.connection = con;
    this.timeout = timeout;
    this.query = query;
    this.field_list_ = new ArrayList<>();

    if (parameters != null) {
      map2Array(parameters);
      if (debug > 1) {
        System.out.println(">>> parameters: " + Arrays.toString(this.paramArray));
      }
    }

    /**
     * Tokenize the raw query and remove empty items
     */
    this.debug = debug;
    String[] tokens = query.replaceAll("\"", "").trim().split(" |,|\\(|\\)|=");
    tokens = rmNullInArray(tokens);
    if (debug > 0) {
      System.out.println(">>> query: " + query);
    }
    if (debug > 1) {
      System.out.println(">>> tokens: " + Arrays.toString(tokens));
    }

    /**
     * Start to parse query
     */
    if (tokens[0].toLowerCase().equals("get")) {
      /**
       * It is a query to get certain vertex or edge
       * e.g., get Page(limit=?)
       */
      this.httpType = HttpTypes.HttpGet;
      int type = parseEdgeOrVertex(tokens);
      if (type == 0) {
        this.query_type = QueryType.QUERY_TYPE_GRAPH_GET_VERTEX;
      } else {
        this.query_type = QueryType.QUERY_TYPE_GRAPH_GET_EDGE;
      }
    } else if (tokens[0].toLowerCase().equals("insert")) {
      /**
       * It is a query to upsert certain vertex or edge
       * e.g., INSERT INTO Page(id, page_id) VALUES("1234", "1234")
       */
      this.httpType = HttpTypes.HttpPost;
      this.query_type = QueryType.QUERY_TYPE_GRAPH;
      parseUpsertQuery(tokens, parameters);
    } else if (tokens[0].toLowerCase().equals("delete")) {
      /**
       * It is a query to delete certain vertex or edge
       * e.g., delete Page(id=?)
       */
      this.httpType = HttpTypes.HttpDelete;
      int type = parseEdgeOrVertex(tokens);
      if (type == 0) {
        this.query_type = QueryType.QUERY_TYPE_GRAPH_DELET_VERTEX;
      } else {
        this.query_type = QueryType.QUERY_TYPE_GRAPH_DELET_EDGE;
      }
    } else if (tokens[0].toLowerCase().equals("builtins")) {
      /**
       * It is a builtin query.
       * e.g., "builtins stat_vertex_number(type=?)"
       */
      this.httpType = HttpTypes.HttpPost;
      this.query_type = QueryType.QUERY_TYPE_BUILTIN;
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
          sb.append(getObjectStr(this.paramArray[0]));
          sb.append("\"");
        } else {
          sb.append(tokens[4]);
          sb.append("\"");
        }
      } else {
        sb.append("*\"");
      }
      sb.append("}");
      this.payload = new StringEntity(sb.toString(), "UTF-8");
      this.payload.setContentType("application/json");
    } else if (tokens[0].toLowerCase().equals("run")) {
      StringBuilder sb = new StringBuilder();
      Integer size = this.paramArray.length;
      if (tokens[1].toLowerCase().equals("interpreted")) {
        /**
         * It is an interpreted query.
         * e.g., "run interpreted(a=?)"
         */
        String query_body = getObjectStr(this.paramArray[size - 1]);
        size -= 1; // The last parameter is query body.
        this.query_type = QueryType.QUERY_TYPE_INTERPRETED;
        this.httpType = HttpTypes.HttpPost;
        this.payload = new StringEntity(query_body, "UTF-8");
        sb.append("interpreted_query?");
      } else {
        /**
         * It is a pre-installed query.
         * e.g., "run pageRank(maxChange=?, maxIteration=?, dampingFactor=?)"
         */
        this.query_type = QueryType.QUERY_TYPE_INSTALLED;
        this.httpType = HttpTypes.HttpGet;
        sb.append(tokens[1]).append("?");
      }
      if (tokens.length != (2 + 2 * size)) {
        throw new SQLException("Token size and parameter size not match : "
            + String.valueOf(tokens.length) + ", " + size);
      }

      /**
       * Parse parameters.
       */
      Integer index = 2;
      for (Integer i = 0; i < size; ++i) {
        sb.append(tokens[index]).append("=").append(getObjectStr(this.paramArray[i]));
        assert tokens[index+1].trim().equals("?") : "Place holder not equals ?";
        index += 2;
        if (i < size - 1) {
          sb.append("&");
        }
      }
      endpoint = sb.toString();
    } else if (tokens[0].toLowerCase().equals("select")) { // for Spark
      this.httpType = HttpTypes.HttpGet;
      Integer from_index = getIndexInArray(tokens, "from");
      Integer where_index = getIndexInArray(tokens, "where");
      Integer length = tokens.length;
      Boolean isGetSchema = Boolean.FALSE;
      /**
       * Spark may reorder attributes when issuing queries,
       * so we need to keep its order.
       */
      for (int i = 1; i < from_index; ++i) {
        this.field_list_.add(tokens[i]);
      }
      if (query.replace(" ", "").indexOf("1=0") >= 0) { // Spark is trying to get schema
        isGetSchema = Boolean.TRUE;
        if (tokens[from_index + 1].toLowerCase().equals("vertex")) {
          // e.g., SELECT * FROM Person WHERE 1=0
          this.vertex_edge_type = tokens[where_index - 1];
          this.query_type = QueryType.QUERY_TYPE_SCHEMA_VERTEX;
        } else if (tokens[from_index + 1].toLowerCase().equals("edge")) {
          // e.g., SELECT * FROM edge Linkto WHERE 1=0
          this.vertex_edge_type = tokens[where_index - 1];
          this.query_type = QueryType.QUERY_TYPE_SCHEMA_EDGE;
        } else if (tokens[from_index + 1].toLowerCase().equals("job")) {
          // e.g., SELECT * FROM job loadPerson WHERE 1=0
          this.query_type = QueryType.QUERY_TYPE_SCHEMA_JOB;
        } else if (tokens[from_index + 1].toLowerCase().equals("query")) {
          // e.g., SELECT * FROM query pageRank WHERE 1=0
          this.query_type = QueryType.QUERY_TYPE_INSTALLED;
        } else if (tokens[from_index + 1].toLowerCase().equals("interpreted")) {
          // e.g., SELECT * FROM interpreted(a=10) WHERE 1=0
          this.query_type = QueryType.QUERY_TYPE_INTERPRETED;
        } else {
          throw new SQLException("Unsupported dbtable: " + tokens[from_index + 1]);
        }
      } else {
        // e.g., SELECT id, account FROM Person
        StringBuilder sb = new StringBuilder();
        this.httpType = HttpTypes.HttpGet;
        if (tokens[from_index + 1].toLowerCase().equals("vertex")) {
          // e.g., SELECT * FROM Person
          this.query_type = QueryType.QUERY_TYPE_GRAPH_GET_VERTEX;
          sb.append("vertices/");
          sb.append(tokens[where_index - 1]);
          this.endpoint = sb.toString();
        } else if (tokens[from_index + 1].toLowerCase().equals("edge")) {
          // e.g., SELECT from.Page, to.Page FROM edge Linkto
          this.query_type = QueryType.QUERY_TYPE_GRAPH_GET_EDGE;
          sb.append("edges/");
          String src_vertex_type = tokens[1].replace("from.", "");
          if (src_vertex_type.equals("*")) {
            src_vertex_type = this.connection.getSrcVertexType();
            if (src_vertex_type == null) {
              throw new SQLException("\"src_vertex_type\" must be specified for wildcard edge.");
            }
          }
          sb.append(src_vertex_type); // source vertex type
          sb.append("/");
          sb.append(this.connection.getSource());
          sb.append("/");
          sb.append(tokens[where_index - 1]);
          this.endpoint = sb.toString();
        } else if (tokens[from_index + 1].toLowerCase().equals("query")) {
          // e.g., SELECT * FROM query pageRank
          this.query_type = QueryType.QUERY_TYPE_INSTALLED;
        } else if (tokens[from_index + 1].toLowerCase().equals("interpreted")) {
          // e.g., SELECT * FROM interpreted(a=10)
          this.query_type = QueryType.QUERY_TYPE_INTERPRETED;
        } else {
          throw new SQLException("Unsupported dbtable: " + tokens[from_index + 1]);
        }
      }

      if (this.query_type == QueryType.QUERY_TYPE_INTERPRETED) {
        from_index--; // interpreted_query don't have keyword "query"
        this.httpType = HttpTypes.HttpPost;
        // Parse query body
        int start = query.toLowerCase().indexOf("interpret query");
        int end = query.lastIndexOf("}");
        if (start < 0 || end < 0 || start >= end) {
          throw new SQLException("No query body was found for interpreted queries.");
        }
        String query_body = query.substring(start, end + 1);
        this.payload = new StringEntity(query_body, "UTF-8");
        // Re-parse the query without query body
        String new_query = query.substring(0, start) + query.substring(end + 1);
        tokens = new_query.replaceAll("\"", "").trim().split(" |,|\\(|\\)|=");
        tokens = rmNullInArray(tokens);
        if (debug > 1) {
          System.out.println(">>> new_query: " + new_query);
          System.out.println(">>> query_body: " + query_body);
        }
        where_index = getIndexInArray(tokens, "where");
        length = tokens.length;
      }

      String column = null;
      String lowerBound = null;
      String upperBound = null;
      // Spark partitions enabled.
      // e.g., WHERE account < 10 or account is null
      // "=" has been eliminated during tokenization.
      if (where_index + 3 < length) {
        column = tokens[where_index + 1];
        if (tokens[where_index + 2].equals("<")) {
          upperBound = tokens[where_index + 3];
        } else if (tokens[where_index + 2].equals(">")) {
          lowerBound = tokens[where_index + 3];
        }
        if (where_index + 7 < length) {
          if (tokens[where_index + 6].equals("<")) {
            upperBound = tokens[where_index + 7];
          } else if (tokens[where_index + 6].equals(">")) {
            lowerBound = tokens[where_index + 7];
          }
        }
      }

      if (this.query_type == QueryType.QUERY_TYPE_GRAPH_GET_VERTEX
          || this.query_type == QueryType.QUERY_TYPE_GRAPH_GET_EDGE) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.endpoint);
        String limit = this.connection.getLimit();
        if (limit != null) {
          sb.append("?limit=");
          sb.append(limit);
        }
        if (column != null) {
          if (limit != null) {
            sb.append("&");
          } else {
            sb.append("?");
          }
          sb.append("filter=");
          if (lowerBound != null) {
            sb.append(column);
            sb.append(urlEncode(">="));
            sb.append(lowerBound);
            if (upperBound != null) {
              sb.append(",");
            }
          }
          if (upperBound != null) {
            sb.append(column);
            sb.append(urlEncode("<"));
            sb.append(upperBound);
          }
        }
        this.endpoint = sb.toString();
      }

      if (this.query_type == QueryType.QUERY_TYPE_INSTALLED
          || this.query_type == QueryType.QUERY_TYPE_INTERPRETED) {
        StringBuilder sb = new StringBuilder();
        if (this.query_type == QueryType.QUERY_TYPE_INSTALLED) {
          sb.append(tokens[from_index + 2]).append("?");
        } else {
          sb.append("interpreted_query?");
        }

        /**
         * Parse parameters.
         */
        Boolean isFirst = Boolean.TRUE;
        for (Integer i = from_index + 3; i < where_index; i += 2) {
          if (isFirst) {
            isFirst = Boolean.FALSE;
          } else {
            sb.append("&");
          }
          sb.append(tokens[i]).append("=").append(tokens[i + 1]);
        }
        if (lowerBound != null) {
          if (isFirst) {
            isFirst = Boolean.FALSE;
          } else {
            sb.append("&");
          }
          sb.append("lowerBound=").append(lowerBound);
        }
        if (upperBound != null) {
          if (isFirst) {
            isFirst = Boolean.FALSE;
          } else {
            sb.append("&");
          }
          sb.append("upperBound=").append(upperBound);
        }
        if (isGetSchema) {
          if (isFirst) {
            isFirst = Boolean.FALSE;
          } else {
            sb.append("&");
          }
          sb.append("topK=1");
        }
        this.endpoint = sb.toString();
      }
    } else {
      throw new SQLException("Unsupported operation: " + query);
    }
  }

  /**
   * Build a http request based on endpoint, query type and other info.
   */
  public HttpRequestBase buildQuery(String host, Integer port, Boolean secure,
      String graph, String token, String json, String filename,
      String sep, String eol) throws SQLException {
    HttpRequestBase request;
    StringBuilder sb = new StringBuilder();
    switch (this.query_type) {
      case QUERY_TYPE_BUILTIN:
        sb.append("/restpp/builtins");
        break;
      case QUERY_TYPE_INSTALLED:
        sb.append("/restpp/query");
        break;
      case QUERY_TYPE_INTERPRETED:
        sb.append("/gsqlserver");
        break;
      case QUERY_TYPE_GRAPH:
      case QUERY_TYPE_GRAPH_GET_VERTEX:
      case QUERY_TYPE_GRAPH_GET_EDGE:
      case QUERY_TYPE_GRAPH_DELET_VERTEX:
      case QUERY_TYPE_GRAPH_DELET_EDGE:
        sb.append("/restpp/graph");
        break;
      case QUERY_TYPE_SCHEMA_EDGE:
      case QUERY_TYPE_SCHEMA_VERTEX:
        sb.append("/gsqlserver/gsql/schema?");
        if (graph != null && !graph.equals("")) {
          sb.append("graph=").append(graph).append("&");
        }
        sb.append("type=").append(this.vertex_edge_type);
        break;
      case QUERY_TYPE_LOAD_JOB:
        sb.append("/restpp/ddl");
        break;
      default:
       throw new SQLException("Invalid query type.");
    }

    if (this.query_type != QueryType.QUERY_TYPE_BUILTIN
        && this.query_type != QueryType.QUERY_TYPE_SCHEMA_EDGE
        && this.query_type != QueryType.QUERY_TYPE_SCHEMA_VERTEX) {
      if (graph != null && !graph.equals("")
        && this.query_type != QueryType.QUERY_TYPE_INTERPRETED) {
        sb.append("/").append(graph);
      }
      if (!"".equals(this.endpoint)) {
        sb.append("/").append(this.endpoint);
      }
    }

    if (this.query_type == QueryType.QUERY_TYPE_LOAD_JOB) {
      sb.append("?tag=");
      sb.append(this.job);
      sb.append("&filename=");
      sb.append(filename);
      sb.append("&sep=");
      sb.append(urlEncode(sep));
      sb.append("&eol=");
      sb.append(urlEncode(eol));
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
        if (debug > 0) {
          System.out.println(">>> request: " + url.toString());
        }
        request = new HttpGet(url);
        break;
      case HttpPost:
        HttpPost post = new HttpPost(url);
        if (!"".equals(json)) {
          StringEntity payload = new StringEntity(json, "UTF-8");
          if ("".equals(this.job)) {
            payload.setContentType("application/json");
          }
          payload.setContentEncoding("UTF-8");
          post.setEntity(payload);
        } else {
          this.payload.setContentEncoding("UTF-8");
          post.setEntity(this.payload);
        }
        Header[] headers = post.getAllHeaders();
        if (debug > 0) {
          try {
            System.out.println(">>> request: " + post.toString());
            if (debug > 1) {
              for (Header header : headers) {
                System.out.println(">>>> header: " + header.getName() + ": " + header.getValue());
              }
              if (!"".equals(json)) {
                System.out.println(">>>> json: " + json);
              } else {
                String content = EntityUtils.toString(this.payload);
                System.out.println(">>>> payload: " + content);
              }
            }
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
    if (this.timeout >= 0) {
      request.addHeader("GSQL-TIMEOUT", String.valueOf(this.timeout));
    }
    // Schema queries and interpreted queries only support username/password
    if (this.query_type == QueryType.QUERY_TYPE_SCHEMA_EDGE
        || this.query_type == QueryType.QUERY_TYPE_SCHEMA_VERTEX
        || this.query_type == QueryType.QUERY_TYPE_INTERPRETED) {
      request.addHeader("Authorization", this.connection.getBasicAuth());
    } else if (token != null && !token.equals("")) {
      request.addHeader("Authorization", "Bearer " + token);
    }

    return request;
  }

  public String getEdgeJson() {
    return this.edge_json;
  }

  public String getLine() {
    return this.line;
  }

  public List<String> getFieldList() {
    return this.field_list_;
  }

  public String getJob() {
    return this.job;
  }

  public String getVertexJson() {
    return this.vertex_json;
  }

  public QueryType getQueryType() {
    return this.query_type;
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
