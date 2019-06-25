package com.tigergraph.jdbc;

import java.sql.*;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public abstract class Connection implements java.sql.Connection {
  private String url;
  private Properties properties;

  /**
   * Default constructor.
   */
  protected Connection(Properties properties, String url) {
    this.url = url;
    this.properties = properties;
  }

  /**
   * Get the connection url.
   */
  public String getUrl() {
    return url;
  }
  
  /**
   * Get the properties for this connection.
   */
  public Properties getProperties() {
    return properties;
  }
  
  /**
   * Abstract Methods.
   */

  @Override public abstract DatabaseMetaData getMetaData() throws SQLException;

  @Override public abstract void setAutoCommit(boolean autoCommit) throws SQLException;

  @Override public abstract boolean getAutoCommit() throws SQLException;

  @Override abstract public void commit() throws SQLException;

  @Override abstract public void rollback() throws SQLException;

  @Override public abstract Statement createStatement() throws SQLException;

  @Override public abstract Statement createStatement(int resultSetType,
    int resultSetConcurrency) throws SQLException;

  @Override public abstract Statement createStatement(int resultSetType,
    int resultSetConcurrency, int resultSetHoldability) throws SQLException;

  @Override public abstract PreparedStatement prepareStatement(String sql) throws SQLException;

  @Override public abstract PreparedStatement prepareStatement(String sql,
    int resultSetType, int resultSetConcurrency) throws SQLException;

  @Override public abstract PreparedStatement prepareStatement(String sql,
    int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLException;

  @Override public abstract void close() throws SQLException;

  @Override public abstract boolean isClosed() throws SQLException;

  @Override public abstract boolean isValid(int timeout) throws SQLException;

  /**
   * Methods not implemented yet.
   */

  @Override public java.sql.CallableStatement prepareCall(String sql) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public java.sql.CallableStatement prepareCall(String sql,
    int resultSetType, int resultSetConcurrency) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Map<String, Class<?>> getTypeMap() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Savepoint setSavepoint() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Savepoint setSavepoint(String name) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void rollback(Savepoint savepoint) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void releaseSavepoint(Savepoint savepoint) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public CallableStatement prepareCall(String sql, int resultSetType,
    int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public PreparedStatement prepareStatement(String sql,
    int[] columnIndexes) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public PreparedStatement prepareStatement(String sql,
    String[] columnNames) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Clob createClob() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Blob createBlob() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public NClob createNClob() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public SQLXML createSQLXML() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setClientInfo(String name, String value) throws SQLClientInfoException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setClientInfo(Properties properties) throws SQLClientInfoException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getClientInfo(String name) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Properties getClientInfo() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setSchema(String schema) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void abort(Executor executor) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getNetworkTimeout() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public String getUserName() {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public int getFlattening() {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setReadOnly(boolean readOnly) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isReadOnly() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setHoldability(int holdability) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getHoldability() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setCatalog(String catalog) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getCatalog() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getTransactionIsolation() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setTransactionIsolation(int level) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public PreparedStatement prepareStatement(String sql,
    int autoGeneratedKeys) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String nativeSQL(String sql) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public SQLWarning getWarnings() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void clearWarnings() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getSchema() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

}

