package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.restpp.RestppConnection;
import com.tigergraph.jdbc.restpp.driver.QueryParser;
import com.tigergraph.jdbc.restpp.driver.RestppResponse;
import com.tigergraph.jdbc.restpp.driver.QueryType;
import com.tigergraph.jdbc.Statement;

import java.sql.BatchUpdateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestppStatement extends Statement {

  private Integer debug = 0;
  private Integer timeout = 0;
  private Integer atomic = 0;
  private List<String> edge_list;
  private List<String> vertex_list;
  private QueryParser parser;
  private QueryType query_type;

  public RestppStatement(RestppConnection restppConnection, Integer debug,
      Integer timeout, Integer atomic) {
    super(restppConnection);
    this.debug = debug;
    this.timeout = timeout;
    this.atomic = atomic;
    edge_list = new ArrayList<String>();
    vertex_list = new ArrayList<String>();
  }

  @Override public ResultSet executeQuery(String query) throws SQLException {
    this.execute(query);
    return currentResultSet;
  }

  @Override public boolean execute(String query) throws SQLException {
    // execute the query
    this.parser = new QueryParser((RestppConnection) getConnection(), query,
        null, this.debug, this.timeout, this.atomic);
    this.query_type = parser.getQueryType();

    RestppResponse response =
      ((RestppConnection) getConnection()).executeQuery(parser, "");

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

  @Override public void addBatch(String sql) throws SQLException {
    this.parser = new QueryParser((RestppConnection) getConnection(), sql,
        null, this.debug, this.timeout, this.atomic);
    String vertex_json = parser.getVertexJson();
    String edge_json = parser.getEdgeJson();
    if (vertex_json != "") {
      vertex_list.add(vertex_json);
    }
    if (edge_json != "") {
      edge_list.add(edge_json);
    }
  }

  @Override public void clearBatch() throws SQLException {
    edge_list.clear();
    vertex_list.clear();
  }

  @Override public int[] executeBatch() throws SQLException {
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
    RestppResponse response =
      ((RestppConnection) getConnection()).executeQuery(this.parser, payload);

    if (response.hasError()) {
      throw new SQLException(response.getErrMsg());
    }

    List<JSONObject> results = response.getResults();
    if (this.debug > 1) {
      System.out.println(">>> payload: " + payload);
      System.out.println(">>> result: " + results.get(0));
    }
    count[0] = results.get(0).getInt("accepted_vertices");
    count[1] = results.get(0).getInt("accepted_edges");

    return count;
  }

  @Override public int executeUpdate(String query) throws SQLException {
    if (this.debug > 1) {
      System.out.println(">>> executeUpdate: " + query);
    }
    return 0;
  }

  /**
   * Methods not implemented yet.
   */

  @Override public int getResultSetConcurrency() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getResultSetType() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getResultSetHoldability() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

}
