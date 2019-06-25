package com.tigergraph.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

abstract class DataSource implements javax.sql.DataSource {

  /**
   * Methods not implemented yet.
   */

  @Override public Connection getConnection() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Connection getConnection(String username,
    String password) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public PrintWriter getLogWriter() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setLogWriter(PrintWriter out) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setLoginTimeout(int seconds) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getLoginTimeout() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }
}

