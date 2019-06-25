package com.tigergraph.jdbc;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public abstract class ResultSetMetaData implements java.sql.ResultSetMetaData {

  /**
   * Default constructor.
   */
  protected ResultSetMetaData() {
  }

  @Override public int getColumnCount() throws SQLException {
    /**
     * Only one column for each ResultSet.
     */
    return 1;
  }

  /**
   * Methods not implemented yet.
   */

  @Override public boolean isWritable(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isDefinitelyWritable(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getColumnLabel(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getColumnName(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getCatalogName(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getColumnDisplaySize(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isAutoIncrement(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isSearchable(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isCurrency(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int isNullable(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isSigned(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getPrecision(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getScale(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getColumnType(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }
  
  @Override public String getColumnTypeName(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getColumnClassName(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }
  
  @Override public String getTableName(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getSchemaName(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isCaseSensitive(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isReadOnly(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

}

