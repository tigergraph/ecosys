package com.tigergraph.jdbc;

import com.tigergraph.jdbc.utils.ExceptionBuilder;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

abstract class DataSource implements javax.sql.DataSource {

  @Override public Connection getConnection() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public Connection getConnection(String username, String password) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public PrintWriter getLogWriter() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setLogWriter(PrintWriter out) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setLoginTimeout(int seconds) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public int getLoginTimeout() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public <T> T unwrap(Class<T> iface) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }
}

