package com.tigergraph.jdbc;

import com.tigergraph.jdbc.utils.ExceptionBuilder;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public abstract class ResultSetMetaData implements java.sql.ResultSetMetaData {

  /**
   * List of column of the ResultSet
   */
  protected final List<String> keys;

  /**
   * Default constructor with the list of column.
   *
   * @param keys List of column of the ResultSet
   */
  protected ResultSetMetaData(List<String> keys) {
    if (keys != null) {
      this.keys = keys;
    } else {
      this.keys = new ArrayList<>();
    }
  }

  /*------------------------------------*/
  /*       Default implementation       */
  /*------------------------------------*/

  @Override public int getColumnCount() throws SQLException {
    int result = 0;
    if (this.keys != null) {
      result = this.keys.size();
    }
    return result;
  }

  @Override public String getColumnLabel(int column) throws SQLException {
    return this.getColumnName(column);
  }

  @Override public String getColumnName(int column) throws SQLException {
    if (this.keys == null || column > this.keys.size() || column <= 0) {
      throw new SQLException("Column out of range");
    }
    return this.keys.get(column - 1);
  }

  @Override public String getCatalogName(int column) throws SQLException {
    return ""; //not applicable
  }

  @Override public int getColumnDisplaySize(int column) throws SQLException {
    int type = this.getColumnType(column);
    int value = 0;
    if (type == Types.VARCHAR) {
      value = 40;
    } else if (type == Types.INTEGER) {
      value = 10;
    } else if (type == Types.BOOLEAN) {
      value = 5;
    } else if (type == Types.FLOAT) {
      value = 15;
    } else if (type == Types.JAVA_OBJECT) {
      value = 60;
    }
    return value;
  }

  @Override public boolean isAutoIncrement(int column) throws SQLException {
    return false;
  }

  @Override public boolean isSearchable(int column) throws SQLException {
    if (column <= 0 || column > this.getColumnCount()) {
      return false;
    }
    return true;
  }

  @Override public boolean isCurrency(int column) throws SQLException {
    return false;
  }

  @Override public int isNullable(int column) throws SQLException {
    return columnNoNulls;
  }

  @Override public boolean isSigned(int column) throws SQLException {
    return false;
  }

  @Override public int getPrecision(int column) throws SQLException {
    return 0;
  }

  @Override public int getScale(int column) throws SQLException {
    return 0;
  }

  @Override public int getColumnType(int column) throws SQLException {
    return Types.VARCHAR;
  }
  
  @Override public String getColumnTypeName(int column) throws SQLException {
    return "String";
  }

  /**
   * By default, every field are string ...
   */
  @Override public String getColumnClassName(int column) throws SQLException {
    return String.class.getName();
  }
  
  @Override public String getTableName(int column) throws SQLException {
    return "";
  }

  @Override public String getSchemaName(int column) throws SQLException {
    return "";
  }

  @Override public boolean isCaseSensitive(int column) throws SQLException {
    return true;
  }

  @Override public boolean isReadOnly(int column) throws SQLException {
    return false;
  }

  /*---------------------------------*/
  /*       Not implemented yet       */
  /*---------------------------------*/

  @Override public boolean isWritable(int column) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public boolean isDefinitelyWritable(int column) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public <T> T unwrap(Class<T> iface) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

}

