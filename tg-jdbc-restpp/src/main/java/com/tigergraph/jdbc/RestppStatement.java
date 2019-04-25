package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.utils.ExceptionBuilder;
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

  private List<String> batchStatements;
  private Boolean debug = Boolean.FALSE;

  public RestppStatement(RestppConnection restppConnection, Boolean debug) {
    super(restppConnection);
    this.debug = debug;
    batchStatements = new ArrayList<>();
  }

  @Override public ResultSet executeQuery(String query) throws SQLException {
    this.execute(query);
    return currentResultSet;
  }

  @Override public int executeUpdate(String query) throws SQLException {
    this.execute(query);
    return currentUpdateCount;
  }

  @Override public boolean execute(String query) throws SQLException {
    checkClosed();

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

  @Override public int getResultSetConcurrency() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public int getResultSetType() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public int getResultSetHoldability() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  /*-------------------*/
  /*       Batch       */
  /*-------------------*/

  @Override public void addBatch(String sql) throws SQLException {
    this.checkClosed();
    this.batchStatements.add(sql);
  }

  @Override public void clearBatch() throws SQLException {
    this.checkClosed();
    this.batchStatements.clear();
  }

  @Override public int[] executeBatch() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

}

