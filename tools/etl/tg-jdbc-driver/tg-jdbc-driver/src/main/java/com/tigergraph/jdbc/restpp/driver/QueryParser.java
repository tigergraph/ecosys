package com.tigergraph.jdbc.restpp.driver;

import com.tigergraph.jdbc.restpp.RestppConnection;
import com.tigergraph.jdbc.JobIdGenerator;
import com.tigergraph.jdbc.log.TGLoggerFactory;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonArrayBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Array;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.Map.Entry;
import java.nio.charset.StandardCharsets;

/** Parse the raw query, and build a http request. */
public class QueryParser {

  private static final Logger logger = TGLoggerFactory.getLogger(QueryParser.class);

  public enum HttpTypes {
    HttpGet,
    HttpPost,
    HttpDelete
  }

  private Object[] paramArray = {};
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
  private int atomic;
  private boolean
      isLoggable; // Disable logging in batch mode, whose logs will be printed in `executeBatch()`

  private static final Integer MAX_PAYLOAD_LOG_SIZE = 64;

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
      // Remove single-quoted strings in url query parameters
      return URLEncoder.encode(s.replace("\'", ""), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      logger.error("Failed to encode URL", e);
      throw new SQLException("Failed to encode URL", e);
    }
  }

  /**
   * Construct url query string e.g. ?a=5&b=6 query = "token0 token1 token2=token3, token4=token5"
   * => tokens = {token0, token1, token2, token3, token4, token5} token_begin=2, token_end=6 =>
   * query string: ?tokens[2]=tokens[3]&tokens[4]=tokens[5]
   */
  private String getUrlQueryString(
      String[] tokens, int token_begin, int token_end, int params_begin) throws SQLException {
    // no params found
    if (token_end <= token_begin) return "";
    // Should be in pairs, e.g. limit=5
    if (((token_end - token_begin) & 0x1) == 1) {
      String errMsg =
          "Invalid query parameter(s), should be in pairs: name=value. Missing name or value.";
      logger.error(errMsg);
      throw new SQLException(errMsg);
    }
    StringBuilder sb = new StringBuilder();
    Boolean isFirst = Boolean.TRUE;
    for (int i = token_begin; i < token_end; i += 2) {
      if (isFirst) {
        isFirst = Boolean.FALSE;
        sb.append("?").append(tokens[i]).append("=");
      } else {
        sb.append("&").append(tokens[i]).append("=");
      }
      if (tokens[i + 1].equals("?")) {
        sb.append(urlEncode(getObjectStr(this.paramArray[params_begin])));
        params_begin++;
      } else {
        sb.append(urlEncode(tokens[i + 1]));
      }
    }
    return sb.toString();
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

  /** Convert a map to an array, use "key" as index. */
  private void map2Array(Map<Integer, Object> parameters) throws SQLException {
    this.paramArray = new Object[parameters.size()];
    Iterator<Entry<Integer, Object>> iter = parameters.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<Integer, Object> entry = iter.next();
      // PreparedStatement parameter indices start at 1
      Integer index = entry.getKey() - 1;
      // Check if parameter index out of range
      if (index >= paramArray.length) {
        String errMsg =
            "Parameter index out of range, index: "
                + (index + 1)
                + ", value: "
                + entry.getValue().toString();
        logger.error(errMsg);
        throw new SQLException(errMsg);
      }
      /*
       * Cannot convert to String here, as some values are used as Json values,
       * which could be int, double or string.
       */
      this.paramArray[index] = entry.getValue();
    }
  }

  /** Tokenize a query string */
  private String[] tokenize(String query) {
    char code = '$';
    String codeStr = Character.toString(code);
    char[] encoded = query.toCharArray();
    boolean inStr = Boolean.FALSE;
    int j = 0;
    String[] values;
    List<String> strList = new ArrayList<String>();
    StringBuilder sb = new StringBuilder();
    sb.append('\'');
    /** Replace string with '$' first to prevent strings from being tokenized. */
    for (int i = 0; i < encoded.length; ++i) {
      if (encoded[i] == '\'') {
        inStr = !inStr;
        if (!inStr) {
          encoded[j++] = code;
          sb.append('\'');
          strList.add(sb.toString());
          sb.setLength(0);
          sb.append('\'');
        }
      } else if (inStr) {
        sb.append(encoded[i]);
      } else {
        if (i > j) {
          encoded[j] = encoded[i];
        }
        ++j;
      }
    }
    query = String.valueOf(encoded).substring(0, j);
    String[] strArray = query.replaceAll("\"", "").trim().split(" |,|\\(|\\)|=");
    List<String> strListNew = new ArrayList<>();
    j = 0;
    /** Remove empty tokens and put the original strings back. */
    for (int i = 0; i < strArray.length; i++) {
      String str = strArray[i];
      if (str != null && !str.equals("")) {
        if (str.equals(codeStr)) {
          strListNew.add(strList.get(j++));
        } else {
          strListNew.add(str);
        }
      }
    }
    return strListNew.toArray(new String[strListNew.size()]);
  }

  /** Get the count of '?' in tokens */
  private Integer getParamCount(String[] tokens) {
    Integer count = 0;
    for (int i = 0; i < tokens.length; i++) {
      count += tokens[i].equals("?") ? 1 : 0;
    }
    return count;
  }

  /** Parse value and convert to corresponding data type to build json object. */
  private Object parseValueType(String value) {
    if (value.indexOf('\'') >= 0) { // it's a string
      return value;
    } else if (value.indexOf('.') >= 0) { // it's a double
      return Double.parseDouble(value);
    } else if (value.toLowerCase().equals("true")
        || value.toLowerCase().equals("false")) { // it's a boolean
      return Boolean.parseBoolean(value);
    } else { // it's a long(TG int has 8 bytes)
      return Long.parseLong(value);
    }
  }

  private static JsonObjectBuilder addValue(JsonObjectBuilder obj, String key, Object value)
      throws SQLException {
    if (value instanceof Integer) {
      obj.add(key, (Integer) value);
    } else if (value instanceof Long) {
      obj.add(key, (Long) value);
    } else if (value instanceof String) {
      obj.add(key, (String) value);
    } else if (value instanceof Float) {
      obj.add(key, (Float) value);
    } else if (value instanceof Double) {
      obj.add(key, (Double) value);
    } else if (value instanceof BigInteger) {
      obj.add(key, (BigInteger) value);
    } else if (value instanceof Boolean) {
      obj.add(key, (Boolean) value);
    } else if (value instanceof Array) {
      Object[] elements = (Object[]) ((Array) value).getArray();
      JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
      for (Object e : elements) {
        addValue(jsonArrayBuilder, e);
      }
      obj.add(key, jsonArrayBuilder);
    } else if (value instanceof JsonValue) {
      JsonValue val = (JsonValue) value;
      obj.add(key, val);
    } else {
      obj.add(key, String.valueOf(value));
    }
    return obj;
  }

  /** Parse element type of an Array */
  private static JsonArrayBuilder addValue(JsonArrayBuilder obj, Object value) {
    if (value instanceof Integer) {
      obj.add((Integer) value);
    } else if (value instanceof Short) {
      obj.add((Short) value);
    } else if (value instanceof Byte) {
      obj.add((Byte) value);
    } else if (value instanceof Long) {
      obj.add((Long) value);
    } else if (value instanceof String) {
      obj.add((String) value);
    } else if (value instanceof Float) {
      obj.add((Float) value);
    } else if (value instanceof Double) {
      obj.add((Double) value);
    } else if (value instanceof Boolean) {
      obj.add((Boolean) value);
    } else if (value instanceof byte[]) {
      obj.add(new String((byte[]) value, StandardCharsets.UTF_8));
    } else if (value instanceof Timestamp) {
      obj.add(((Timestamp) value).toString().split("\\.")[0]);
    } else if (value instanceof Date) {
      obj.add(((Date) value).toString());
    } else if (value instanceof BigDecimal) {
      obj.add(((BigDecimal) value).doubleValue());
    } else {
      obj.add(String.valueOf(value));
    }
    return obj;
  }

  /**
   * Parse queries to get vertices and edges. Reference:
   * https://docs.tigergraph.com/dev/restpp-api/built-in-endpoints Return values: -1: not a valid
   * query 0: a vertex query 1: an edge query
   */
  private int parseEdgeOrVertex(String[] tokens) throws SQLException {
    StringBuilder sb = new StringBuilder();
    int ret = -1;

    if (tokens.length <= 1) {
      logger.error("Invalid vertex or edge query.");
      throw new SQLException("Invalid vertex or edge query.");
    }

    int params_index = getIndexInArray(tokens, "params");

    if (tokens[1].toLowerCase().equals("edge")) {
      /**
       * Query syntax: get edge(src_vertex_type, src_vertex_id) [params(limit=5,
       * sort='attr1,attr2')] get edge(src_vertex_type, src_vertex_id, edge_type) [params(limit=5,
       * sort='attr1,attr2')] get edge(src_vertex_type, src_vertex_id, edge_type, tgt_vertex_type)
       * [params(limit=5, sort='attr1,attr2')] get edge(src_vertex_type, src_vertex_id, edge_type,
       * tgt_vertex_type, tgt_vertex_id) [params(limit=5, sort='attr1,attr2')]
       */
      ret = 1;
      sb.append("edges");
      if (params_index < 4 || params_index > 7) {
        String errMsg = "Invalid query, should have 2~5 args in edge(), got " + (params_index - 2);
        logger.error(errMsg);
        throw new SQLException(errMsg);
      }
    } else if (tokens[1].toLowerCase().equals("vertex")) {
      /**
       * Query syntax: get vertex(src_vertex_type) [params(limit=5, sort='attr1,attr2')] get
       * vertex(src_vertex_type, src_vertex_id) [params(limit=5, sort='attr1,attr2')]
       */
      ret = 0;
      sb.append("vertices");
      if (params_index < 3 || params_index > 4) {
        String errMsg =
            "Invalid query, should have 1~2 args in vertex(), got " + (params_index - 2);
        logger.error(errMsg);
        throw new SQLException(errMsg);
      }
    } else {
      String errMsg =
          "Invalid query, the second token should be 'edge' or 'vertex', got " + tokens[1];
      logger.error(errMsg);
      throw new SQLException(errMsg);
    }

    // Construct the REST part
    int paramArray_index = 0;
    for (int i = 2; i < params_index; i++) {
      if (tokens[i].equals("?")) {
        sb.append("/").append(getObjectStr(this.paramArray[paramArray_index]));
        paramArray_index++;
      } else {
        sb.append("/").append(tokens[i]);
      }
    }

    // Construct the query params part
    // User can set any parameters and JDBC won't check the validity but left it to
    // restpp.
    sb.append(getUrlQueryString(tokens, params_index + 1, tokens.length, paramArray_index));

    // Remove the single-quoted strings in REST part
    this.endpoint = sb.toString().replace("\'", "");
    return ret;
  }

  /**
   * Parse queries to upsert vertices or edges e.g., INSERT INTO vertex Page(id, page_id)
   * VALUES("1234", "1234") INSERT INTO EDGE Linkto(Page, Page, weight) VALUES("1234", "5678", 3)
   * INSERT INTO job loadFollow ("Person","Person","weight") VALUES (?,?,?)
   */
  private void parseUpsertQuery(String[] tokens, Map<Integer, Object> parameters)
      throws SQLException {

    if (tokens.length < 2 || !(tokens[1].toLowerCase().equals("into"))) {
      String errMsg = "Invalid upsert query. A valid one should start with \"insert into\"";
      logger.error(errMsg);
      throw new SQLException(errMsg);
    }

    // Sanity check on attributes' number
    Integer values_index = getIndexInArray(tokens, "values");
    if (values_index == tokens.length) {
      String errMsg = "Invalid upsert query. A valid one should contains \"VALUES\"";
      logger.error(errMsg);
      throw new SQLException(errMsg);
    }
    Integer count = tokens.length - values_index - 1;
    if (count < 1) {
      String errMsg = "Invalid upsert query. A valid one should contains one attribute at least.";
      logger.error(errMsg);
      throw new SQLException(errMsg);
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
      sb.append(Objects.toString(this.paramArray[0], ""));
      for (int i = 1; i < size; ++i) {
        sb.append(sep);
        sb.append(Objects.toString(this.paramArray[i], ""));
      }
      this.line = sb.toString();
      this.job = tokens[3]; // loading job name
    } else if (tokens[2].toLowerCase().equals("edge")) {
      // insert an edge
      // e.g., INSERT INTO edge Follow ("Person","Person","weight") VALUES (?,?,?)
      if (count != values_index - param_offset) {
        String errMsg =
            "Numbers of attributes and values are not match. Attribute: "
                + count
                + ", value: "
                + (values_index - param_offset);
        logger.error(errMsg);
        throw new SQLException(errMsg);
      }
      if (count < 2) {
        String errMsg = "Upserting an edge needs two parameters at least, got " + count;
        logger.error(errMsg);
        throw new SQLException(errMsg);
      }

      JsonObjectBuilder attrObj = Json.createObjectBuilder();
      Integer paramArray_attr_index = 0;
      // fill src_vertex_id and tgt_vertex_id with parameter
      for (Integer i = values_index + 1; i < values_index + 3; ++i) {
        if (tokens[i].equals("?")) {
          tokens[i] = getObjectStr(paramArray[paramArray_attr_index++]);
        }
      }

      // Parse attributes if any
      for (Integer i = 0; i < count - 2; ++i) {
        attrObj.add(
            tokens[param_offset + 2 + i],
            addValue(
                Json.createObjectBuilder(),
                "value",
                tokens[values_index + 3 + i].equals("?")
                    ? this.paramArray[paramArray_attr_index++]
                    : parseValueType(tokens[values_index + 3 + i])));
      }

      obj.add(
          tokens[param_offset], // src vertex type
          Json.createObjectBuilder()
              .add(
                  tokens[values_index + 1], // src vertex id
                  Json.createObjectBuilder()
                      .add(
                          tokens[3], // edge type
                          Json.createObjectBuilder()
                              .add(
                                  tokens[param_offset + 1], // tgt vertex type
                                  Json.createObjectBuilder()
                                      .add(
                                          tokens[values_index + 2], // tgt vertex id
                                          attrObj))))); // attributes

      // someone may use single-quoted strings
      edge_json = obj.build().toString().replace("\'", "");
      // remove the outmost "{}"
      edge_json = edge_json.substring(1, edge_json.length() - 1);
    } else if (tokens[2].toLowerCase().equals("vertex")) {
      // insert an vertex
      // e.g., INSERT INTO vertex Person ("id","account") VALUES (?,?)
      if (count != values_index - param_offset) {
        String errMsg =
            "Numbers of attributes and values are not match. Attribute: "
                + count
                + ", value: "
                + (values_index - param_offset);
        logger.error(errMsg);
        throw new SQLException(errMsg);
      }

      JsonObjectBuilder attrObj = Json.createObjectBuilder();
      Integer paramArray_attr_index = 0;
      // fill vertex_id with parameter
      if (tokens[values_index + 1].equals("?")) {
        tokens[values_index + 1] = getObjectStr(paramArray[paramArray_attr_index++]);
      }

      // Parse attributes if any
      for (Integer i = 0; i < count - 1; ++i) {
        attrObj.add(
            tokens[param_offset + 1 + i],
            addValue(
                Json.createObjectBuilder(),
                "value",
                tokens[values_index + 2 + i].equals("?")
                    ? this.paramArray[paramArray_attr_index++]
                    : parseValueType(tokens[values_index + 2 + i])));
      }

      obj.add(
          tokens[3], // vertex type
          Json.createObjectBuilder()
              .add(
                  tokens[values_index + 1], // vertex id
                  attrObj)); // attributes

      // someone may use single-quoted strings
      vertex_json = obj.build().toString().replace("\'", "");
      // remove the outmost "{}"
      vertex_json = vertex_json.substring(1, vertex_json.length() - 1);
    } else {
      logger.error("Invalid upsert query: {}", tokens[2]);
      throw new SQLException("Invalid upsert query: " + tokens[2]);
    }
  }

  /**
   * Parse queries to find shortestpath/paths between 2 vertices e.g. find shortest_path(person,
   * tom, person, jack) find allpaths(person, tom, person, jack, 5)
   */
  private void parsePathFindingQuery(String[] tokens) throws SQLException {
    if (tokens.length < 2) {
      logger.error("Invalid query.");
      throw new SQLException("Invalid query.");
    }
    if (tokens[1].toLowerCase().equals("shortestpath")) {
      if (tokens.length != 6) {
        String errMsg =
            "Invalid query, should specify src_vertex_type, src_vertex_id, tgt_vertex_type and"
                + " tgt_vertex_id.";
        logger.error(errMsg);
        throw new SQLException(errMsg);
      }
      this.query_type = QueryType.QUERY_TYPE_SHORTESTPATH;
    } else if (tokens[1].toLowerCase().equals("allpaths")) {
      if (tokens.length != 7) {
        String errMsg =
            "Invalid query, should specify src_vertex_type, src_vertex_id, tgt_vertex_type,"
                + " tgt_vertex_id and max_length.";
        logger.error(errMsg);
        throw new SQLException(errMsg);
      }
      this.query_type = QueryType.QUERY_TYPE_ALLPATHS;
    } else {
      String errMsg =
          "Invalid query string, the second token should be 'shortestpath' or 'allpaths'.";
      logger.error(errMsg);
      throw new SQLException(errMsg);
    }
    Integer paramArray_index = 0;
    // fill tokens with params
    for (Integer i = 2; i < tokens.length; i++) {
      if (tokens[i].toLowerCase().equals("?")) {
        tokens[i] = getObjectStr(paramArray[paramArray_index++]);
      }
    }

    JsonObjectBuilder obj = Json.createObjectBuilder();
    obj.add("source", Json.createObjectBuilder().add("type", tokens[2]).add("id", tokens[3]));
    obj.add("target", Json.createObjectBuilder().add("type", tokens[4]).add("id", tokens[5]));
    if (this.query_type == QueryType.QUERY_TYPE_ALLPATHS) {
      obj.add("maxLength", Integer.parseInt(tokens[6]));
    }

    this.payload = new StringEntity(obj.build().toString().replace("\'", ""), "UTF-8");
    this.payload.setContentType("application/json");
  }

  /** Build endpoint based on raw query and parameters. */
  public QueryParser(
      RestppConnection con,
      String query,
      Map<Integer, Object> parameters,
      Integer timeout,
      Integer atomic,
      boolean isLoggable)
      throws SQLException {
    if ((query == null) || query.equals("")) {
      logger.error("Query could not be null or empty.");
      throw new SQLException("Query could not be null or empty.");
    }

    this.connection = con;
    this.timeout = timeout;
    this.atomic = atomic;
    this.query = query;
    this.field_list_ = new ArrayList<>();
    this.isLoggable = isLoggable;

    if (parameters != null) {
      map2Array(parameters);
      if (isLoggable) logger.debug("Parameters: {}", Arrays.toString(this.paramArray));
    }

    /** Tokenize the raw query and remove empty items */
    String[] tokens = tokenize(query);
    if (isLoggable) {
      logger.info("Query: {}", query);
      logger.debug("Tokenized query: {}", Arrays.toString(tokens));
    }

    if (!tokens[0].toLowerCase().equals("select")) {
      // For interpreted query, it has a query body.
      if (getParamCount(tokens) + (tokens[1].toLowerCase().equals("interpreted") ? 1 : 0)
          != this.paramArray.length) {
        String errMsg =
            "Parameter size not match, the number of '?' should be equal to the number of"
                + " parameters you set, got "
                + getParamCount(tokens)
                + " '?' and "
                + this.paramArray.length
                + " parameters";
        logger.error(errMsg);
        throw new SQLException(errMsg);
      }
    }

    /** Start to parse query */
    if (tokens[0].toLowerCase().equals("get")) {
      /** It is a query to get certain vertex or edge e.g., get Page(limit=?) */
      this.httpType = HttpTypes.HttpGet;
      int type = parseEdgeOrVertex(tokens);
      if (type == 0) {
        this.query_type = QueryType.QUERY_TYPE_GRAPH_GET_VERTEX;
      } else {
        this.query_type = QueryType.QUERY_TYPE_GRAPH_GET_EDGE;
      }
    } else if (tokens[0].toLowerCase().equals("find")) {
      /**
       * It is a query to find shortest path or all paths between 2 vertices. e.g. find
       * shortest_path(person, tom, person, jack)
       */
      this.httpType = HttpTypes.HttpPost;
      parsePathFindingQuery(tokens);
    } else if (tokens[0].toLowerCase().equals("insert")) {
      /**
       * It is a query to upsert certain vertex or edge e.g., INSERT INTO Page(id, page_id)
       * VALUES("1234", "1234")
       */
      this.httpType = HttpTypes.HttpPost;
      this.query_type = QueryType.QUERY_TYPE_GRAPH;
      parseUpsertQuery(tokens, parameters);
    } else if (tokens[0].toLowerCase().equals("delete")) {
      /** It is a query to delete certain vertex or edge e.g., delete Page(id=?) */
      this.httpType = HttpTypes.HttpDelete;
      int type = parseEdgeOrVertex(tokens);
      if (type == 0) {
        this.query_type = QueryType.QUERY_TYPE_GRAPH_DELET_VERTEX;
      } else {
        this.query_type = QueryType.QUERY_TYPE_GRAPH_DELET_EDGE;
      }
    } else if (tokens[0].toLowerCase().equals("builtins")) {
      /** It is a builtin query. e.g., "builtins stat_vertex_number(type=?)" */
      this.httpType = HttpTypes.HttpPost;
      this.query_type = QueryType.QUERY_TYPE_BUILTIN;
      if (tokens.length < 2) {
        logger.error("Need to specify builtins function name.");
        throw new SQLException("Need to specify builtins function name.");
      }
      StringBuilder sb = new StringBuilder();
      sb.append("{\"function\":\"");
      sb.append(tokens[1]);
      sb.append("\", \"type\":\"");
      if (tokens.length >= 4) {
        if (tokens[3].trim().equals("?")) {
          sb.append(getObjectStr(this.paramArray[0]));
          sb.append("\"");
        } else {
          sb.append(tokens[3]);
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
        /** It is an interpreted query. e.g., "run interpreted(a=?)" */
        String query_body = getObjectStr(this.paramArray[size - 1]);
        size -= 1; // The last parameter is query body.
        this.query_type = QueryType.QUERY_TYPE_INTERPRETED;
        this.httpType = HttpTypes.HttpPost;
        this.payload = new StringEntity(query_body, "UTF-8");
        sb.append("interpreted_query");
      } else {
        /**
         * It is a pre-installed query. e.g., "run pageRank(maxChange=?, maxIteration=?,
         * dampingFactor=?)"
         */
        this.query_type = QueryType.QUERY_TYPE_INSTALLED;
        this.httpType = HttpTypes.HttpGet;
        sb.append(tokens[1]);
      }

      /** Parse parameters. */
      int token_index = 2;
      int paramArray_index = 0;
      sb.append(getUrlQueryString(tokens, token_index, tokens.length, paramArray_index));
      endpoint = sb.toString();
    } else if (tokens[0].toLowerCase().equals("status")) {
      /**
       * It's a status query. "status jobid(JOBID)" is to query the loading stats can support other
       * status query in the future, e.g., check query status, check system status
       */
      StringBuilder sb = new StringBuilder();
      if (tokens.length < 2) {
        throw new SQLException("Need to specify status query item.");
      }
      // query loading job stats
      if (tokens[1].toLowerCase().equals("jobid")) {
        if (tokens.length < 3) {
          throw new SQLException("Missing jobid value");
        }
        sb.append("gsql/loading-jobs?action=getprogress&jobId=");
        if (tokens[2].trim().equals("?")) {
          if (this.paramArray.length == 0) {
            throw new SQLException("The parameter 'jobid' of the preparedStatement was not set.");
          }
          sb.append(getObjectStr(this.paramArray[0]));
        } else {
          sb.append(tokens[2]);
        }
        this.query_type = QueryType.QUERY_TYPE_JOBID_STATS;
        this.httpType = HttpTypes.HttpGet;
        this.endpoint = sb.toString();
      } else {
        throw new SQLException("Unsupported status query item: " + tokens[1]);
      }
    } else if (tokens[0].toLowerCase().equals("select")) { // for Spark
      this.httpType = HttpTypes.HttpGet;
      Integer from_index = getIndexInArray(tokens, "from");
      Integer where_index = getIndexInArray(tokens, "where");
      Integer length = tokens.length;
      Boolean isGetSchema = Boolean.FALSE;
      /** Spark may reorder attributes when issuing queries, so we need to keep its order. */
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
          this.query_type = QueryType.QUERY_TYPE_EXISTENCE_JOB;
        } else if (tokens[from_index + 1].toLowerCase().equals("query")) {
          // e.g., SELECT * FROM query pageRank WHERE 1=0
          this.query_type = QueryType.QUERY_TYPE_INSTALLED;
        } else if (tokens[from_index + 1].toLowerCase().equals("interpreted")) {
          // e.g., SELECT * FROM interpreted(a=10) WHERE 1=0
          this.query_type = QueryType.QUERY_TYPE_INTERPRETED;
        } else if (tokens[from_index + 1].toLowerCase().equals("jobid")) {
          // e.g., SELECT * FROM jobid jdbc.1666666.ab23efa WHERE 1=0
          this.query_type = QueryType.QUERY_TYPE_JOBID_STATS;
        } else {
          logger.error("Unsupported dbtable: {} ", tokens[from_index + 1]);
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
              logger.error("\"src_vertex_type\" must be specified for wildcard edge.");
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
        } else if (tokens[from_index + 1].toLowerCase().equals("jobid")) {
          // e.g., SELECT * FROM jobid jdbc.1666666.ab23efa
          this.query_type = QueryType.QUERY_TYPE_JOBID_STATS;
        } else {
          logger.error("Unsupported dbtable: {}", tokens[from_index + 1]);
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
          logger.error("No query body was found for interpreted queries.");
          throw new SQLException("No query body was found for interpreted queries.");
        }
        String query_body = query.substring(start, end + 1);
        this.payload = new StringEntity(query_body, "UTF-8");
        // Re-parse the query without query body
        String new_query = query.substring(0, start) + query.substring(end + 1);
        tokens = tokenize(new_query);
        logger.debug("Interpreted query: {}", new_query);
        logger.debug("Query body: {}", query_body);
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

        /** Parse parameters. */
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

      // e.g. SELECT * FROM jobid $JOBID WHERE
      if (this.query_type == QueryType.QUERY_TYPE_JOBID_STATS) {
        if (where_index - from_index < 3) {
          logger.error("Missing jobid: {}", query);
          throw new SQLException("Missing jobid: " + query);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("gsql/loading-jobs?action=getprogress&jobId=");
        sb.append(tokens[from_index + 2]);
        this.endpoint = sb.toString();
      }
    } else {
      logger.error("Unsupported operation: {}", query);
      throw new SQLException("Unsupported operation: " + query);
    }
  }

  /** Build a http request based on endpoint, query type and other info. */
  public HttpRequestBase buildQuery(
      String host,
      Integer port,
      Boolean secure,
      String graph,
      String token,
      String json,
      String filename,
      String sep,
      String eol,
      String jobid,
      String max_num_error,
      String max_percent_error)
      throws SQLException {
    HttpRequestBase request;
    StringBuilder sb = new StringBuilder();
    switch (this.query_type) {
      case QUERY_TYPE_BUILTIN:
        sb.append("/restpp/builtins");
        if (graph != null && !graph.equals("")) {
          sb.append("/").append(graph);
        }
        break;
      case QUERY_TYPE_SHORTESTPATH:
        sb.append("/restpp/shortestpath");
        break;
      case QUERY_TYPE_ALLPATHS:
        sb.append("/restpp/allpaths");
        break;
      case QUERY_TYPE_INSTALLED:
        sb.append("/restpp/query");
        break;
      case QUERY_TYPE_INTERPRETED:
      case QUERY_TYPE_JOBID_STATS:
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
        logger.error("Invalid query type.");
        throw new SQLException("Invalid query type.");
    }

    if (this.query_type != QueryType.QUERY_TYPE_BUILTIN
        && this.query_type != QueryType.QUERY_TYPE_SCHEMA_EDGE
        && this.query_type != QueryType.QUERY_TYPE_SCHEMA_VERTEX) {
      if (graph != null
          && !graph.equals("")
          && this.query_type != QueryType.QUERY_TYPE_INTERPRETED
          && this.query_type != QueryType.QUERY_TYPE_JOBID_STATS) {
        sb.append("/").append(graph);
      }
      if (!"".equals(this.endpoint)) {
        sb.append("/").append(this.endpoint);
      }
    }

    if (this.query_type == QueryType.QUERY_TYPE_JOBID_STATS) {
      if (graph != null && !graph.equals("")) {
        sb.append("&graph=").append(graph);
      } else {
        throw new SQLException("Property 'graph' is required for querying loading statistics.");
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
      if (jobid != null) {
        JobIdGenerator jobIdGenerator = new JobIdGenerator(graph, this.job);
        if (jobIdGenerator.validate(jobid)) {
          sb.append("&jobid=");
          sb.append(urlEncode(jobid));
        } else {
          String errMsg =
              String.format(
                  "Wrong jobid, the jobid should be generated by JobIdGenerator using graph: %s,"
                      + " job: %s.",
                  graph, this.job);
          logger.error(errMsg);
          throw new SQLException(errMsg);
        }
      }
      if (max_num_error != null) {
        sb.append("&max_num_error=");
        sb.append(urlEncode(max_num_error));
      }
      if (max_percent_error != null) {
        sb.append("&max_percent_error=");
        sb.append(urlEncode(max_percent_error));
      }
      if (this.atomic > 0) {
        sb.append("&atomic_post=true");
      }
    } else if (this.query_type == QueryType.QUERY_TYPE_GRAPH) {
      if (this.atomic > 0) {
        sb.append("?atomic_post=true");
      }
    }

    String url = "";
    try {
      if (secure) url = new URL("https", host, port, sb.toString()).toString();
      else url = new URL("http", host, port, sb.toString()).toString();
    } catch (MalformedURLException e) {
      logger.error("Invalid server URL", e);
      throw new SQLException("Invalid server URL", e);
    }

    switch (this.httpType) {
      case HttpGet:
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
          if (this.payload == null) {
            this.payload = new StringEntity("", "UTF-8");
          } else {
            this.payload.setContentEncoding("UTF-8");
          }
          post.setEntity(this.payload);
        }
        request = post;

        if (logger.isDebugEnabled()) {
          try {
            if (!"".equals(json)) {
              // Loading job payload is extreamly large, so limit the length.
              if (this.getQueryType() == QueryType.QUERY_TYPE_LOAD_JOB
                  && json.length() > MAX_PAYLOAD_LOG_SIZE) {
                logger.debug("Part of payload: {}......", json.substring(0, MAX_PAYLOAD_LOG_SIZE));
              } else {
                logger.debug("Payload: {}", json);
              }
            } else {
              logger.debug("Payload: {}", EntityUtils.toString(this.payload));
            }
          } catch (IOException e) {
            logger.error("Failed to convert EntityUtils to string", e);
            throw new SQLException("Failed to convert EntityUtils to string", e);
          }
        }
        break;
      case HttpDelete:
        request = new HttpDelete(url);
        break;
      default:
        logger.error("Invalid http request type.");
        throw new SQLException("Invalid http request type.");
    }

    request.addHeader("Accept", ContentType.APPLICATION_JSON.toString());
    if (this.timeout > 0) {
      request.addHeader("GSQL-TIMEOUT", String.valueOf(this.timeout * 1000));
    }
    if (this.atomic > 0 && this.query_type == QueryType.QUERY_TYPE_GRAPH) {
      request.addHeader("gsql-atomic-level", "atomic");
    }

    // Schema queries and interpreted queries only support username/password
    if (this.query_type == QueryType.QUERY_TYPE_SCHEMA_EDGE
        || this.query_type == QueryType.QUERY_TYPE_SCHEMA_VERTEX
        || this.query_type == QueryType.QUERY_TYPE_INTERPRETED
        || this.query_type == QueryType.QUERY_TYPE_JOBID_STATS) {
      request.addHeader("Authorization", this.connection.getBasicAuth());
    } else if (token != null && !token.equals("")) {
      request.addHeader("Authorization", "Bearer " + token);
    }

    if (logger.isDebugEnabled()) {
      Header[] headers = request.getAllHeaders();
      for (Header header : headers) {
        if (header.getName() == "Authorization") {
          logger.debug("Header: {}: {}", header.getName(), "[REDACTED]");
        } else {
          logger.debug("Header: {}: {}", header.getName(), header.getValue());
        }
      }
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

  /** For unit test only. */
  public String getEndpoint() {
    return this.endpoint;
  }

  /** For unit test only. */
  public String getPayload() throws IOException {
    return EntityUtils.toString(this.payload);
  }
}
