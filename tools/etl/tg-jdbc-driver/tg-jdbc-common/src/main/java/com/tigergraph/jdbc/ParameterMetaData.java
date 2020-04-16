package com.tigergraph.jdbc;

import java.sql.SQLException;

public abstract class ParameterMetaData implements java.sql.ParameterMetaData {

  /**
   * Methods not implemented yet.
   */

  @Override public int getParameterCount() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int isNullable(int param) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isSigned(int param) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getPrecision(int param) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getScale(int param) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getParameterType(int param) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getParameterTypeName(int param) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getParameterClassName(int param) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getParameterMode(int param) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }
}

