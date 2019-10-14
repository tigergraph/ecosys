package com.tigergraph.jdbc;

import com.tigergraph.jdbc.Attribute;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ResultSetMetaData implements java.sql.ResultSetMetaData {

  private String table_name;
  private List<Attribute> attributeList;
  private int column_size;

  /**
   * Default constructor.
   */
  public ResultSetMetaData(String table_name, List<Attribute> attributeList) {
    this.table_name = table_name;
    this.attributeList = attributeList;
    this.column_size = attributeList.size();
  }

  private int getValidColumnIndex(int column) throws SQLException {
    int index = column - 1;
    if ((index < 0) || (index >= this.column_size)) {
      throw new SQLException("column number exceeds the boundary.");
    }

    return index;
  }

  @Override public int getColumnCount() throws SQLException {
    return this.column_size;
  }

  @Override public String getColumnLabel(int column) throws SQLException {
    int index = getValidColumnIndex(column);

    return this.attributeList.get(index).getName();
  }

  @Override public String getColumnName(int column) throws SQLException {
    int index = getValidColumnIndex(column);

    return this.attributeList.get(index).getName();
  }

  @Override public String getCatalogName(int column) throws SQLException {
    int index = getValidColumnIndex(column);

    return this.table_name;
  }

  @Override public boolean isAutoIncrement(int column) throws SQLException {
    return Boolean.FALSE;
  }

  @Override public boolean isSearchable(int column) throws SQLException {
    return Boolean.FALSE;
  }

  @Override public boolean isCurrency(int column) throws SQLException {
    return Boolean.FALSE;
  }

  @Override public int isNullable(int column) throws SQLException {
    return java.sql.ResultSetMetaData.columnNoNulls;
  }

  @Override public boolean isSigned(int column) throws SQLException {
    return Boolean.FALSE;
  }

  @Override public int getPrecision(int column) throws SQLException {
    int index = getValidColumnIndex(column);
    return this.attributeList.get(index).getPrecision();
  }

  @Override public int getScale(int column) throws SQLException {
    int index = getValidColumnIndex(column);
    return this.attributeList.get(index).getScale();
  }

  @Override public int getColumnType(int column) throws SQLException {
    int index = getValidColumnIndex(column);
    String type = this.attributeList.get(index).getType().toLowerCase();
    if (type.equals("string") || type.equals("string compress")) {
      return Types.VARCHAR;
    }
    if (type.equals("bool")) {
      return Types.BOOLEAN;
    }
    if (type.equals("int")) {
      return Types.INTEGER;
    }
    if (type.equals("uint")) {
      return Types.INTEGER;
    }
    if (type.equals("double")) {
      return Types.DOUBLE;
    }
    if (type.equals("real")) {
      return Types.REAL;
    }
    if (type.equals("float")) {
      return Types.FLOAT;
    }
    /*
     * For set/list/map/udt and other types, they will be treated as "Types.VARCHAR".
     * Spark will panic if it returns "Types.OTHER".
     */
    return Types.VARCHAR;
  }

  @Override public String getColumnTypeName(int column) throws SQLException {
    int index = getValidColumnIndex(column);

    return this.attributeList.get(index).getType();
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

  @Override public int getColumnDisplaySize(int column) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

}
