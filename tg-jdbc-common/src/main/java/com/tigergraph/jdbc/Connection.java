package com.tigergraph.jdbc;

import com.tigergraph.jdbc.utils.ExceptionBuilder;

import java.sql.*;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public abstract class Connection implements java.sql.Connection {

  /**
   * JDBC Url used for this connection
   */
  private String url;

  /**
   * JDBC driver properties
   */
  private Properties properties;

  /**
   * Is the connection is in readonly mode ?
   */
  private boolean readOnly = false;

  /**
   * Holdability of the connection
   */
  private int holdability;

  /**
   * Default constructor with properties.
   *
   * @param properties driver properties
   * @param url connection url
   * @param defaultHoldability connection holdability
   */
  protected Connection(Properties properties, String url, int defaultHoldability) {
    this.url = url;
    this.properties = properties;
    this.holdability = defaultHoldability;
  }

  public static boolean hasDebug(Properties properties) {
    return "true".equalsIgnoreCase(properties.getProperty("debug", "false"));
  }

  /**
   * Get the connection url.
   * 
   * @return String the connection url
   */
  public String getUrl() {
    return url;
  }
  
  /**
   * Get the properties for this connection.
   * 
   * @return Properties the properties for this connection
   */
  public Properties getProperties() {
    return properties;
  }
  
  /**
   * Get user name of this connection.
   *
   * @return String
   */
  public String getUserName() {
    return properties.getProperty("user");
  }

  /**
   * Get the flattening sample rows (-1 if no flattening).
   *
   * @return int
   */
  public int getFlattening() {
    String flatten = properties.getProperty("flatten");
    return flatten == null ? 0 : Integer.parseInt(flatten);
  }

  /*---------------------------------------*/
  /*       Some useful check method        */
  /*---------------------------------------*/

  /**
   * Check if this connection is closed or not.
   * If it's closed, then we throw a SQLException, otherwise we do nothing.
   * @throws SQLException sqlexception
   */
  protected void checkClosed() throws SQLException {
    if (this.isClosed()) {
      throw new SQLException("Connection already closed");
    }
  }

  /**
   * Method to check if we are into autocommit mode.
   * If we do, then it throw an exception.
   * This method is for using into commit and rollback method.
   * @throws SQLException sqlexception
   */
  protected void checkAutoCommit() throws SQLException {
    if (this.getAutoCommit()) {
      throw new SQLException("Cannot commit when in autocommit");
    }
  }

  /**
   * Check if can execute the query into the current mode (ie. readonly or not).
   * If we can't an SQLException is throw.
   *
   * @param query Cypher query
   * @throws SQLException sqlexception
   */
  protected void checkReadOnly(String query) throws SQLException {
    if (isReadOnly() && isMutating(query)) {
      throw new SQLException("Mutating Query in readonly mode: " + query);
    }
  }

  /**
   * Detect some cypher keyword to know if this query mutated the graph.
   * /!\ This not enough now due to procedure procedure.
   *
   * @param query Cypher query
   * @return
   */
  private boolean isMutating(String query) {
    return query.matches("(?is).*\\b(create|relate|delete|set)\\b.*");
  }

  /**
   * Check if the holdability parameter conform to specification.
   * If it doesn't, we throw an exception.
   * {@link java.sql.Connection#setHoldability(int)}
   *
   * @param resultSetHoldability The holdability value to check
   * @throws SQLException sqlexception
   */
  protected void checkHoldabilityParams(int resultSetHoldability) throws SQLException {
    // @formatter:off
    if( resultSetHoldability != ResultSet.HOLD_CURSORS_OVER_COMMIT &&
      resultSetHoldability != ResultSet.CLOSE_CURSORS_AT_COMMIT
    ){
      throw new SQLFeatureNotSupportedException();
    }
    // @formatter:on
  }

  /**
   * Check if the concurrency parameter conform to specification.
   * If it doesn't, we throw an exception.
   *
   * @param resultSetConcurrency The concurrency value to check
   * @throws SQLException sqlexception
   */
  protected void checkConcurrencyParams(int resultSetConcurrency) throws SQLException {
    // @formatter:off
    if( resultSetConcurrency != ResultSet.CONCUR_UPDATABLE &&
      resultSetConcurrency != ResultSet.CONCUR_READ_ONLY
    ){
      throw new SQLFeatureNotSupportedException();
    }
    // @formatter:on
  }

  /**
   * Check if the resultset type parameter conform to specification.
   * If it doesn't, we throw an exception.
   *
   * @param resultSetType The concurrency value to check
   * @throws SQLException sqlexception
   */
  protected void checkTypeParams(int resultSetType) throws SQLException {
    // @formatter:off
    if( resultSetType != ResultSet.TYPE_FORWARD_ONLY &&
      resultSetType != ResultSet.TYPE_SCROLL_INSENSITIVE &&
      resultSetType != ResultSet.TYPE_SCROLL_SENSITIVE
    ){
      throw new SQLFeatureNotSupportedException();
    }
    // @formatter:on
  }

  /**
   * Check if the transaction isolation level parameter conform to specification.
   * If it doesn't, we throw an exception.
   *
   * @param level The transaction isolation level value to check
   * @throws SQLException sqlexception
   */
  protected void checkTransactionIsolation(int level) throws SQLException {
    // @formatter:off
    if( level != TRANSACTION_NONE &&
        level != TRANSACTION_READ_COMMITTED &&
        level != TRANSACTION_READ_UNCOMMITTED &&
        level != TRANSACTION_REPEATABLE_READ &&
        level != TRANSACTION_SERIALIZABLE
        ){
      throw new SQLException();
    }
    // @formatter:on
  }
  
  /**
   * Check if the auto generated keys parameter conform to specification.
   * If it doesn't, we throw an exception.
   * 
   * @param autoGeneratedKeys the auto generated keys value to check
   * @throws SQLException sqlexception
   */
  private void checkAutoGeneratedKeys(int autoGeneratedKeys) throws SQLException {
    // @formatter:off
    if( autoGeneratedKeys != Statement.RETURN_GENERATED_KEYS &&
        autoGeneratedKeys != Statement.NO_GENERATED_KEYS
        ){
      throw new SQLException();
    }
    // @formatter:on
  }

  /*------------------------------------*/
  /*       Default implementation       */
  /*------------------------------------*/

  @Override public void setReadOnly(boolean readOnly) throws SQLException {
    this.checkClosed();
    this.readOnly = readOnly;
  }

  @Override public boolean isReadOnly() throws SQLException {
    this.checkClosed();
    return this.readOnly;
  }

  @Override public void setHoldability(int holdability) throws SQLException {
    this.checkClosed();
    this.checkHoldabilityParams(holdability);
    this.holdability = holdability;
  }

  @Override public int getHoldability() throws SQLException {
    this.checkClosed();
    return this.holdability;
  }

  /**
   * Default implementation of setCatalog.
   */
  @Override public void setCatalog(String catalog) throws SQLException {
    this.checkClosed();
    return;
  }

  /**
   * Default implementation of getCatalog.
   */
  @Override public String getCatalog() throws SQLException {
    this.checkClosed();
    return null;
  }

  /**
   * Default implementation of getTransactionIsolation.
   */
  @Override public int getTransactionIsolation() throws SQLException {
    this.checkClosed();
    return TRANSACTION_READ_COMMITTED;
  }

  /**
   * Default implementation of setTransactionIsolation.
   */
  @Override public void setTransactionIsolation(int level) throws SQLException {
    this.checkClosed();
    this.checkTransactionIsolation(level);
    if (level != TRANSACTION_READ_COMMITTED) {
      throw new SQLException("Unsupported isolation level");
    }
  }

  /**
   * Default implementation of preparedStatement(String, int).
   * We're just ignoring the autoGeneratedKeys param.
   */
  @Override public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
    this.checkAutoGeneratedKeys(autoGeneratedKeys);
    return prepareStatement(sql);
  }

  /**
   * Default implementation of nativeSQL.
   * Here we should implement some hacks for JDBC tools if needed.
   * This method must be used before running a query.
   */
  @Override public String nativeSQL(String sql) throws SQLException {
    return sql;
  }

  @Override public <T> T unwrap(Class<T> iface) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public SQLWarning getWarnings() throws SQLException {
    checkClosed();
    return null;
  }

  @Override public void clearWarnings() throws SQLException {
    checkClosed();
  }

  @Override public String getSchema() throws SQLException {
    checkClosed();
    return null;
  }

  /*-----------------------------*/
  /*       Abstract method       */
  /*-----------------------------*/

  @Override public abstract DatabaseMetaData getMetaData() throws SQLException;

  @Override public abstract void setAutoCommit(boolean autoCommit) throws SQLException;

  @Override public abstract boolean getAutoCommit() throws SQLException;

  @Override abstract public void commit() throws SQLException;

  @Override abstract public void rollback() throws SQLException;

  @Override public abstract Statement createStatement() throws SQLException;

  @Override public abstract Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException;

  @Override public abstract Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException;

  @Override public abstract PreparedStatement prepareStatement(String sql) throws SQLException;

  @Override public abstract PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException;

  @Override public abstract PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLException;

  @Override public abstract void close() throws SQLException;

  @Override public abstract boolean isClosed() throws SQLException;

  @Override public abstract boolean isValid(int timeout) throws SQLException;

  /*---------------------------------*/
  /*       Not implemented yet       */
  /*---------------------------------*/

  @Override public java.sql.CallableStatement prepareCall(String sql) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public Map<String, Class<?>> getTypeMap() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public Savepoint setSavepoint() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public Savepoint setSavepoint(String name) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void rollback(Savepoint savepoint) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void releaseSavepoint(Savepoint savepoint) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public Clob createClob() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public Blob createBlob() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public NClob createNClob() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public SQLXML createSQLXML() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setClientInfo(String name, String value) throws SQLClientInfoException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setClientInfo(Properties properties) throws SQLClientInfoException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public String getClientInfo(String name) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public Properties getClientInfo() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setSchema(String schema) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void abort(Executor executor) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public int getNetworkTimeout() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

}

