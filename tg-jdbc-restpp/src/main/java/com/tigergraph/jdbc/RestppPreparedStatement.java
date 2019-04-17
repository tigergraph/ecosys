package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.utils.ExceptionBuilder;
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

  private List<String> batchStatements;
  private String query;
  private Boolean debug = Boolean.FALSE;

  public RestppPreparedStatement(RestppConnection restppConnection, String query, Boolean debug) {
    super(restppConnection, query);
    this.query = query;
    this.debug = debug;
    batchStatements = new ArrayList<>();
  }

  @Override public ResultSet executeQuery() throws SQLException {
    this.execute();
    return currentResultSet;
  }

  @Override public int executeUpdate() throws SQLException {
    this.execute();
    return currentUpdateCount;
  }

  @Override public boolean execute() throws SQLException {
    checkClosed();

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

  @Override public int getResultSetConcurrency() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public int getResultSetType() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public int getResultSetHoldability() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public ParameterMetaData getParameterMetaData() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public ResultSetMetaData getMetaData() throws SQLException {
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
    this.checkClosed();
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

}

