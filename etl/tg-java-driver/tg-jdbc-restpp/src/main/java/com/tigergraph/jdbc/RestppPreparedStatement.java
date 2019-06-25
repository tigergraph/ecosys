package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.restpp.RestppConnection;
import com.tigergraph.jdbc.restpp.driver.QueryParser;
import com.tigergraph.jdbc.restpp.driver.RestppResponse;
import com.tigergraph.jdbc.*;

import java.sql.BatchUpdateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestppPreparedStatement extends PreparedStatement {

  private String query;
  private Boolean debug = Boolean.FALSE;

  public RestppPreparedStatement(RestppConnection restppConnection, String query, Boolean debug) {
    super(restppConnection, query);
    this.query = query;
    this.debug = debug;
  }

  @Override public ResultSet executeQuery() throws SQLException {
    this.execute();
    return currentResultSet;
  }

  @Override public boolean execute() throws SQLException {
    // execute the query
    RestppResponse response =
      ((RestppConnection) getConnection()).
      executeQuery(new QueryParser(this.query, this.parameters, this.debug));

    if (response.hasError()) {
      throw new SQLException(response.getErrMsg());
    }
    
    // Parse response data
    boolean hasResultSets = response.hasResultSets();

    this.currentResultSet = hasResultSets ? new RestppResultSet(this, response.getResults()) : null;

    return hasResultSets;
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

  @Override public ParameterMetaData getParameterMetaData() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public ResultSetMetaData getMetaData() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int executeUpdate() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void addBatch(String sql) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void clearBatch() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int[] executeBatch() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

}

