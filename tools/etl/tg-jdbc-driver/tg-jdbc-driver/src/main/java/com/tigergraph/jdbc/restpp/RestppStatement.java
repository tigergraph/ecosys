package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.common.Statement;
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

public class RestppStatement extends Statement {

  private static final Logger logger = TGLoggerFactory.getLogger(RestppStatement.class);

  private Integer timeout = 0;
  private Integer atomic = 0;
  private List<String> edge_list;
  private List<String> vertex_list;
  private QueryParser parser;
  private QueryType query_type;

  public RestppStatement(RestppConnection restppConnection,
      Integer timeout, Integer atomic) {
    super(restppConnection);
    this.timeout = timeout;
    this.atomic = atomic;
    edge_list = new ArrayList<String>();
    vertex_list = new ArrayList<String>();
  }

  @Override
  public ResultSet executeQuery(String query) throws SQLException {
    this.execute(query);
    return currentResultSet;
  }

  @Override
  public boolean execute(String query) throws SQLException {
    // execute the query
    this.parser = new QueryParser((RestppConnection) getConnection(), query,
        null, this.timeout, this.atomic);
    this.query_type = parser.getQueryType();

    RestppResponse response = ((RestppConnection) getConnection()).executeQuery(parser, "");

    if (response.hasError()) {
      throw new SQLException(response.getErrMsg());
    }

    // Parse response data
    boolean hasResultSets = response.hasResultSets();

    // If source vertex id is not null, Spark is trying to retrieve edge.
    boolean isGettingEdge = ((RestppConnection) getConnection()).getSource() != null;
    this.currentResultSet = hasResultSets ? new RestppResultSet(this,
        response.getResults(), parser.getFieldList(), this.query_type, isGettingEdge) : null;

    return hasResultSets;
  }

  @Override
  public void addBatch(String sql) throws SQLException {
    this.parser = new QueryParser((RestppConnection) getConnection(), sql,
        null, this.timeout, this.atomic);
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

  @Override
  public int[] executeBatch() throws SQLException {
    int[] count = new int[2];
    if (this.edge_list.size() == 0 && this.vertex_list.size() == 0) {
      return count;
    }

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

  @Override
  public int executeUpdate(String query) throws SQLException {
    logger.debug("executeUpdate: {}", query);
    return 0;
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

}
