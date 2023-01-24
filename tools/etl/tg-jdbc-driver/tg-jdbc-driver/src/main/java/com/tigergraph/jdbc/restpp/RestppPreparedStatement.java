package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.common.ParameterMetaData;
import com.tigergraph.jdbc.common.PreparedStatement;
import com.tigergraph.jdbc.common.ResultSetMetaData;
import com.tigergraph.jdbc.restpp.driver.QueryParser;
import com.tigergraph.jdbc.restpp.driver.QueryType;
import com.tigergraph.jdbc.restpp.driver.RestppResponse;
import com.tigergraph.jdbc.log.TGLoggerFactory;

import org.json.JSONObject;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.json.JSONArray;
import org.slf4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RestppPreparedStatement extends PreparedStatement {

  private static final Logger logger = TGLoggerFactory.getLogger(RestppPreparedStatement.class);
  private static final String[] INVALID_LINE_KEY = {
    "rejectLine", "failedConditionLine", "notEnoughToken", "invalidJson", "oversizeToken"
  };
  private static final String[] INVALID_OBJECT_KEY = {
    "invalidAttribute",
    "noIdFound",
    "invalidVertexType",
    "invalidSecondaryId",
    "incorrectFixedBinaryLength",
    "invalidPrimaryId"
  };

  private String query;
  private List<String> edge_list;
  private List<String> vertex_list;
  private QueryParser parser;
  private QueryType query_type;
  private String eol = null;
  private String sep = null;
  private int timeout;
  private int atomic;
  private ComparableVersion tg_version;
  private StringBuilder stringBuilder = null;

  public RestppPreparedStatement(
      RestppConnection restppConnection,
      String query,
      Integer timeout,
      Integer atomic,
      ComparableVersion tg_version) {
    super(restppConnection, query);
    this.query = query;
    this.timeout = timeout;
    this.atomic = atomic;
    this.tg_version = tg_version;
    edge_list = new ArrayList<String>();
    vertex_list = new ArrayList<String>();
  }

  @Override
  public ResultSet executeQuery() throws SQLException {
    this.execute();
    return currentResultSet;
  }

  @Override
  public boolean execute() throws SQLException {
    // execute the query
    this.parser =
        new QueryParser(
            (RestppConnection) getConnection(),
            this.query,
            this.parameters,
            this.timeout,
            this.atomic,
            true);
    this.query_type = parser.getQueryType();

    /** Spark is trying to check existence of a loading job. */
    if (this.query_type == QueryType.QUERY_TYPE_EXISTENCE_JOB) {
      // Spark doesn't care the actual schema
      String lineSchema = "";
      this.currentResultSet = new RestppResultSet(this, lineSchema);
      return Boolean.TRUE;
    }

    RestppResponse response = ((RestppConnection) getConnection()).executeQuery(this.parser, "");

    if (response.hasError()) {
      logger.error(response.getErrMsg());
      throw new SQLException(response.getErrMsg());
    }

    // Parse response data
    boolean hasResultSets = response.hasResultSets();

    // If source vertex id is not null, Spark is trying to retrieve edge.
    boolean isGettingEdge = ((RestppConnection) getConnection()).getSource() != null;
    /** Return an empty ResultSet instead of null, otherwise Spark will panic. */
    this.currentResultSet =
        new RestppResultSet(
            this, response.getResults(), parser.getFieldList(), this.query_type, isGettingEdge);

    return hasResultSets;
  }

  @Override
  public void addBatch() throws SQLException {
    // Shortcut for loading jobs.
    if (this.query_type == QueryType.QUERY_TYPE_LOAD_JOB) {
      if (this.stringBuilder.length() > 0) {
        this.stringBuilder.append(eol);
      }
      this.stringBuilder.append(Objects.toString(this.parameters.get(1), ""));
      for (int i = 1; i < this.parameters.size(); ++i) {
        this.stringBuilder.append(sep);
        this.stringBuilder.append(Objects.toString(this.parameters.get(i + 1), ""));
      }
      return;
    }

    this.parser =
        new QueryParser(
            (RestppConnection) getConnection(),
            this.query,
            this.parameters,
            this.timeout,
            this.atomic,
            false);

    if (this.parser.getQueryType() == QueryType.QUERY_TYPE_LOAD_JOB) {
      this.query_type = this.parser.getQueryType();
      this.eol = ((RestppConnection) getConnection()).getEol();
      this.sep = ((RestppConnection) getConnection()).getSeparator();
      this.stringBuilder = new StringBuilder();
      this.stringBuilder.append(this.parser.getLine());
      return;
    }

    String vertex_json = parser.getVertexJson();
    String edge_json = parser.getEdgeJson();
    if (vertex_json != "") {
      vertex_list.add(vertex_json);
    }
    if (edge_json != "") {
      edge_list.add(edge_json);
    }
  }

  @Override
  public void addBatch(String sql) throws SQLException {
    this.query = sql;
    this.parser =
        new QueryParser(
            (RestppConnection) getConnection(),
            sql,
            this.parameters,
            this.timeout,
            this.atomic,
            false);

    this.query_type = this.parser.getQueryType();
    this.eol = ((RestppConnection) getConnection()).getEol();

    String vertex_json = parser.getVertexJson();
    String edge_json = parser.getEdgeJson();
    if (vertex_json != "") {
      vertex_list.add(vertex_json);
    }
    if (edge_json != "") {
      edge_list.add(edge_json);
    }
  }

  @Override
  public void clearBatch() throws SQLException {
    edge_list.clear();
    vertex_list.clear();
  }

  /**
   * Batch update. For updating vertices/edges, the return values are number of updated vertices and
   * edges. For loading jobs, the return values are number of lines being accepted and rejected.
   */
  @Override
  public int[] executeBatch() throws SQLException {
    logger.info("Batch Query: {}. Type: {}.", this.query, this.parser.getQueryType());
    int[] count = new int[2];

    // It is a loading job.
    if (this.query_type == QueryType.QUERY_TYPE_LOAD_JOB) {
      if (this.stringBuilder == null) {
        return count;
      }
      String payload = this.stringBuilder.toString();
      RestppResponse response =
          ((RestppConnection) getConnection()).executeQuery(this.parser, payload);
      if (response.hasError()) {
        String errMsg =
            String.format(
                "%s: %s\n%s",
                response.getErrCode(),
                response.getErrMsg(),
                loadingJobPostAction(response.getErrCode()));
        logger.error(errMsg);
        throw new SQLException(errMsg);
      }
      List<JSONObject> results = response.getResults();
      if (results.size() > 0) {
        if (this.tg_version.compareTo(new ComparableVersion("3.9.0")) < 0)
          return parseStatsV1(results);
        else return parseStatsV2(results);
      } else {
        StringBuilder errMsgSb = new StringBuilder();
        errMsgSb.append("Failed to run loading job, empty response.");
        // GLE-4684: the /ddl endpoint should return error message when the given filename is
        // invalid
        errMsgSb.append(
            String.format(
                " Please check if the filename '%s' is defined in the loading job.",
                ((RestppConnection) getConnection()).getFilename()));
        if (((RestppConnection) getConnection()).getJobId() != null) {
          errMsgSb
              .append("(Job ID: ")
              .append(((RestppConnection) getConnection()).getJobId())
              .append(")");
        }
        logger.error(errMsgSb.toString());
        throw new SQLException(errMsgSb.toString());
      }
    }

    if (this.edge_list.size() == 0 && this.vertex_list.size() == 0) {
      return count;
    }

    // It is a normal job to upsert vertex or edge via the 'graph' endpoint.
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    if (this.vertex_list.size() > 0) {
      sb.append("\"vertices\": {");
      sb.append(this.vertex_list.get(0));
      for (int i = 1; i < this.vertex_list.size(); ++i) {
        sb.append(",");
        sb.append(this.vertex_list.get(i));
      }
      sb.append("}");
    }
    if (this.edge_list.size() > 0) {
      if (this.vertex_list.size() > 0) {
        sb.append(",");
      }
      sb.append("\"edges\": {");
      sb.append(this.edge_list.get(0));
      for (int i = 1; i < this.edge_list.size(); ++i) {
        sb.append(",");
        sb.append(this.edge_list.get(i));
      }
      sb.append("}");
    }
    sb.append("}");
    String payload = sb.toString();
    RestppResponse response =
        ((RestppConnection) getConnection()).executeQuery(this.parser, payload);

    if (response.hasError()) {
      logger.error(response.getErrMsg());
      throw new SQLException(response.getErrMsg());
    }

    List<JSONObject> results = response.getResults();
    if (results.size() > 0) {
      logger.debug("Result: {}", results.get(0));
      count[0] = results.get(0).getInt("accepted_vertices");
      count[1] = results.get(0).getInt("accepted_edges");
      logger.info("Accepted vertices: {}, accepted edges: {}", count[0], count[1]);
    } else {
      logger.error("Failed to upsert: {}", response.getErrMsg());
      throw new SQLException("Failed to upsert: " + response.getErrMsg());
    }

    return count;
  }

  /** Methods not implemented yet. */
  @Override
  public int getResultSetConcurrency() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public int getResultSetType() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public int getResultSetHoldability() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public ParameterMetaData getParameterMetaData() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public ResultSetMetaData getMetaData() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public int executeUpdate() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  /** For TG version < 3.9.0 */
  private int[] parseStatsV1(List<JSONObject> results) throws SQLException {
    int[] count = new int[2];
    JSONObject obj = (JSONObject) results.get(0).get("statistics");
    JSONArray vertexObjArray = obj.getJSONArray("vertex");
    JSONArray edgeObjArray = obj.getJSONArray("edge");
    Integer acceptedLines = obj.getInt("validLine");
    Integer invalidLines = 0;
    for (int i = 0; i < INVALID_LINE_KEY.length; i++) {
      if (obj.has(INVALID_LINE_KEY[i])) {
        invalidLines += obj.getInt(INVALID_LINE_KEY[i]);
      }
    }

    // Print number of valid vertices, and the non-zero value of other invalid types.
    Integer errors = 0;
    for (int i = 0; i < vertexObjArray.length(); i++) {
      JSONObject vertexObj = vertexObjArray.getJSONObject(i);
      for (int j = 0; j < INVALID_OBJECT_KEY.length; j++) {
        if (vertexObj.has(INVALID_OBJECT_KEY[j])) {
          errors += vertexObj.getInt(INVALID_OBJECT_KEY[j]);
        }
      }
    }

    // Print number of valid edges, and the non-zero value of other invalid types.
    for (int i = 0; i < edgeObjArray.length(); i++) {
      JSONObject edgeObj = edgeObjArray.getJSONObject(i);
      for (int j = 0; j < INVALID_OBJECT_KEY.length; j++) {
        if (edgeObj.has(INVALID_OBJECT_KEY[j])) {
          errors += edgeObj.getInt(INVALID_OBJECT_KEY[j]);
        }
      }
    }

    count[0] = acceptedLines;
    count[1] = invalidLines;

    if (invalidLines > 0 || errors > 0) {
      logger.warn("Found rejected line(s)/object(s): {}", results.get(0));
    } else {
      logger.info("Loading Statistics: {}", results.get(0));
    }

    this.stringBuilder = new StringBuilder();
    return count;
  }

  /** For TG version >= 3.9.0 */
  private int[] parseStatsV2(List<JSONObject> results) throws SQLException {
    int[] count = new int[1];
    JSONObject obj = results.get(0).getJSONObject("statistics").getJSONObject("parsingStatistics");
    Object validLine = obj.query("/fileLevel/validLine");
    if (validLine == null) {
      throw new SQLException("Missing 'validLine' from loading statistics: " + obj.toString());
    }
    count[0] = Integer.valueOf(String.valueOf(validLine));

    // Only failed line/object will have sample data in stats
    if (obj.toString().contains("sample\":{")) {
      logger.warn("Found rejected line(s)/object(s): {}", obj.toString());
    } else {
      logger.info("Loading Statistics: {}", results.get(0));
    }

    this.stringBuilder = new StringBuilder();
    return count;
  }

  private String loadingJobPostAction(String errCode) throws SQLException {
    switch (errCode) {
        // The max_num/percent_error limit exceeds
      case "REST-20002":
        this.parameters.clear();
        this.addBatch("status jobid " + ((RestppConnection) getConnection()).getJobId());
        RestppResponse response =
            ((RestppConnection) getConnection()).executeQuery(this.parser, "");
        String msg =
            "Please query the detailed loading statistics of jobid: "
                + ((RestppConnection) getConnection()).getJobId();
        if (!response.hasError() && response.getResults().size() > 0) {
          msg = String.valueOf(response.getResults().get(0));
        }
        return msg;

      default:
        return "";
    }
  }
}
