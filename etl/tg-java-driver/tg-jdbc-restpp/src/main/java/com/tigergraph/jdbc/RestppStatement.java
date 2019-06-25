package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.restpp.RestppConnection;
import com.tigergraph.jdbc.restpp.driver.QueryParser;
import com.tigergraph.jdbc.restpp.driver.RestppResponse;
import com.tigergraph.jdbc.Statement;

import java.sql.BatchUpdateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestppStatement extends Statement {

  private Boolean debug = Boolean.FALSE;

  public RestppStatement(RestppConnection restppConnection, Boolean debug) {
    super(restppConnection);
    this.debug = debug;
  }

  @Override public ResultSet executeQuery(String query) throws SQLException {
    this.execute(query);
    return currentResultSet;
  }

  @Override public boolean execute(String query) throws SQLException {
    // execute the query
    RestppResponse response = 
      ((RestppConnection) getConnection()).executeQuery(new QueryParser(query, null, this.debug));

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

  @Override public void addBatch(String sql) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void clearBatch() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int[] executeBatch() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int executeUpdate(String query) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

}

