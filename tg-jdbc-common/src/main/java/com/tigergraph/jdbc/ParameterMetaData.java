package com.tigergraph.jdbc;

import com.tigergraph.jdbc.utils.ExceptionBuilder;
import java.sql.SQLException;

public abstract class ParameterMetaData implements java.sql.ParameterMetaData {

  @Override public int getParameterCount() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public int isNullable(int param) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public boolean isSigned(int param) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public int getPrecision(int param) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public int getScale(int param) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public int getParameterType(int param) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public String getParameterTypeName(int param) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public String getParameterClassName(int param) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public int getParameterMode(int param) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public <T> T unwrap(Class<T> iface) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }
}

