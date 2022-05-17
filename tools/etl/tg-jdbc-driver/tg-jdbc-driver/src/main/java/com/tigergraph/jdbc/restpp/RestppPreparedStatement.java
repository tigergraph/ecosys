package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.common.ParameterMetaData;
import com.tigergraph.jdbc.common.PreparedStatement;
import com.tigergraph.jdbc.common.ResultSetMetaData;
import com.tigergraph.jdbc.restpp.driver.QueryParser;
import com.tigergraph.jdbc.restpp.driver.QueryType;
import com.tigergraph.jdbc.restpp.driver.RestppResponse;
import com.tigergraph.jdbc.log.TGLoggerFactory;
import org.json.JSONObject;
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
        this.query, this.parameters, this.timeout, this.atomic);
    this.query_type = parser.getQueryType();

    /**
     * Spark is trying to get schema for a loading job.
     */
    if (this.query_type == QueryType.QUERY_TYPE_SCHEMA_JOB) {
      String lineSchema = ((RestppConnection) getConnection()).getLineSchema();
      if (lineSchema == null) {
        throw new SQLException("Option \"schema\" must be specified when invoking a loading job.");
      }
      this.currentResultSet = new RestppResultSet(this, lineSchema);
      return Boolean.TRUE;
    }

    RestppResponse response = ((RestppConnection) getConnection()).executeQuery(this.parser, "");

    if (response.hasError()) {
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
        this.query, this.parameters, this.timeout, this.atomic);

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
        sql, this.parameters, this.timeout, this.atomic);

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
        logger.debug("Result: {}", results.get(0));
        JSONObject obj = (JSONObject) results.get(0).get("statistics");
        count[0] = obj.getInt("validLine");
        count[1] = obj.getInt("rejectLine");
      }
      logger.info("Accepted lines: {}, rejected lines: {}", count[0], count[1]);

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
      throw new SQLException(response.getErrMsg());
    }

    List<JSONObject> results = response.getResults();
    if (results.size() > 0) {
      logger.debug("Result: {}", results.get(0));
      count[0] = results.get(0).getInt("accepted_vertices");
      count[1] = results.get(0).getInt("accepted_edges");
    }
    logger.info("Accepted vertices: {}, accepted edges: {}", count[0], count[1]);

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
