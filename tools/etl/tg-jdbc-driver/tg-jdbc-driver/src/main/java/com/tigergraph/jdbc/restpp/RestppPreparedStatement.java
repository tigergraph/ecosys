package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.common.ParameterMetaData;
import com.tigergraph.jdbc.common.PreparedStatement;
import com.tigergraph.jdbc.common.ResultSetMetaData;
import com.tigergraph.jdbc.restpp.driver.QueryParser;
import com.tigergraph.jdbc.restpp.driver.QueryType;
import com.tigergraph.jdbc.restpp.driver.RestppResponse;
import com.tigergraph.jdbc.log.TGLoggerFactory;

import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RestppPreparedStatement extends PreparedStatement {

  private static final Logger logger = TGLoggerFactory.getLogger(RestppPreparedStatement.class);

  private String query;
  private List<String> edge_list;
  private List<String> vertex_list;
  private QueryParser parser;
  private QueryType query_type;
  private String eol = null;
  private String sep = null;
  private int timeout;
  private int atomic;
  private StringBuilder stringBuilder = null;

  public RestppPreparedStatement(RestppConnection restppConnection,
      String query, Integer timeout, Integer atomic) {
    super(restppConnection, query);
    this.query = query;
    this.timeout = timeout;
    this.atomic = atomic;
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
    this.parser = new QueryParser((RestppConnection) getConnection(),
        this.query, this.parameters, this.timeout, this.atomic, true);
    this.query_type = parser.getQueryType();

    /**
     * Spark is trying to get schema for a loading job.
     */
    if (this.query_type == QueryType.QUERY_TYPE_SCHEMA_JOB) {
      String lineSchema = ((RestppConnection) getConnection()).getLineSchema();
      if (lineSchema == null) {
        logger.error("Option \"schema\" must be specified when invoking a loading job.");
        throw new SQLException("Option \"schema\" must be specified when invoking a loading job.");
      }
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
    /**
     * Return an empty ResultSet instead of null,
     * otherwise Spark will panic.
     */
    this.currentResultSet = new RestppResultSet(this,
        response.getResults(), parser.getFieldList(), this.query_type, isGettingEdge);

    return hasResultSets;
  }

  @Override
  public void addBatch() throws SQLException {
    // Shortcut for loading jobs.
    if (this.query_type == QueryType.QUERY_TYPE_LOAD_JOB) {
      this.stringBuilder.append(eol);
      this.stringBuilder.append(String.valueOf(this.parameters.get(1)));
      for (int i = 1; i < this.parameters.size(); ++i) {
        this.stringBuilder.append(this.sep).append(String.valueOf(this.parameters.get(i + 1)));
      }
      return;
    }

    this.parser = new QueryParser((RestppConnection) getConnection(),
        this.query, this.parameters, this.timeout, this.atomic, false);

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
    this.parser = new QueryParser((RestppConnection) getConnection(),
        sql, this.parameters, this.timeout, this.atomic, false);

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
   * Batch update.
   * For updating vertices/edges, the return values are number of updated vertices
   * and edges.
   * For loading jobs, the return values are number of lines being accepted and
   * rejected.
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
      RestppResponse response = ((RestppConnection) getConnection()).executeQuery(this.parser, payload);
      List<JSONObject> results = response.getResults();
      if (results.size() > 0) {
        // This calculation is used to calculate the mixed insertion of multiple types
        // of edges and vertices through a loading job
        // accepted lines: `validLine`
        // rejected lines: `rejectedLine` + `failedConditionLine` + `notEnoughToken` +
        // `invalidJson` + `oversizeToken`
        // accepted vertices: sum(accepted objs for all vertex types)
        // rejected vertices: sum(`noIdFound` + `invalidAttribute` +
        // `invalidPrimaryId` + `invalidSecondaryId` + `incorrectFixedBinaryLength` for
        // all vertex types) + rejected lines
        // accepted edges: sum(accepted objs for all vertex types)
        // rejected edges: sum(`noIdFound` + `invalidAttribute` +
        // `invalidPrimaryId` + `invalidSecondaryId` + `incorrectFixedBinaryLength` for
        // all edge types) + rejected lines
        JSONObject obj = (JSONObject) results.get(0).get("statistics");
        JSONArray vertexObjArray = obj.getJSONArray("vertex");
        JSONArray edgeObjArray = obj.getJSONArray("edge");
        Integer acceptedLines = obj.getInt("validLine");
        Integer rejectedLines = obj.getInt("rejectLine") + obj.getInt("failedConditionLine")
            + obj.getInt("notEnoughToken") + obj.getInt("invalidJson") + obj.getInt("oversizeToken");
        Integer acceptedVertices = 0;
        Integer rejectedVertices = 0;
        Integer acceptedEdges = 0;
        Integer rejectedEdges = 0;

        for (int i = 0; i < vertexObjArray.length(); i++) {
          acceptedVertices += vertexObjArray.getJSONObject(i).getInt("validObject");
          rejectedVertices += vertexObjArray.getJSONObject(i).getInt("noIdFound");
          rejectedVertices += vertexObjArray.getJSONObject(i).getInt("invalidAttribute");
          rejectedVertices += vertexObjArray.getJSONObject(i).getInt("invalidPrimaryId");
          rejectedVertices += vertexObjArray.getJSONObject(i).getInt("invalidSecondaryId");
          rejectedVertices += vertexObjArray.getJSONObject(i).getInt("incorrectFixedBinaryLength");
          // Only keep "invalidAttributeLines"
          vertexObjArray.getJSONObject(i).remove("invalidAttributeLinesData");
        }

        for (int i = 0; i < edgeObjArray.length(); i++) {
          acceptedEdges += edgeObjArray.getJSONObject(i).getInt("validObject");
          rejectedEdges += edgeObjArray.getJSONObject(i).getInt("noIdFound");
          rejectedEdges += edgeObjArray.getJSONObject(i).getInt("invalidAttribute");
          rejectedEdges += edgeObjArray.getJSONObject(i).getInt("invalidPrimaryId");
          rejectedEdges += edgeObjArray.getJSONObject(i).getInt("invalidSecondaryId");
          rejectedEdges += edgeObjArray.getJSONObject(i).getInt("incorrectFixedBinaryLength");
          // Only keep "invalidAttributeLines"
          edgeObjArray.getJSONObject(i).remove("invalidAttributeLinesData");
        }

        count[0] = acceptedLines;
        count[1] = rejectedLines;

        logger.debug("Result: {}", results.get(0));

        logger.info(
            "Accepted lines: {}, rejected lines: {}, accepted vertices: {}, rejected vertices from accepted lines: {}, accepted edges: {}, rejected edges from accepted lines: {}",
            count[0], count[1], acceptedVertices, rejectedVertices, acceptedEdges, rejectedEdges);
      } else {
        logger.error("Failed to run loading job, empty response.");
        throw new SQLException("Failed to run loading job, empty response.");
      }

      this.stringBuilder = new StringBuilder();
      return count;
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
    RestppResponse response = ((RestppConnection) getConnection()).executeQuery(this.parser, payload);

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
      logger.error("Failed to upsert, empty response.");
      throw new SQLException("Failed to upsert, empty response.");
    }

    return count;
  }

  /**
   * Methods not implemented yet.
   */

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

}
