package com.tigergraph.jdbc;

import com.tigergraph.jdbc.utils.ExceptionBuilder;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static java.sql.Types.*;

public abstract class PreparedStatement extends Statement implements java.sql.PreparedStatement {

  protected String                  statement;
  protected HashMap<String, Object> parameters;
  private int                     parametersNumber;

  /**
   * Default constructor with connection and statement.
   *
   * @param connection   The JDBC connection
   * @param rawStatement The prepared statement
   */
  protected PreparedStatement(Connection connection, String rawStatement) {
    super(connection);
    this.statement = rawStatement;
    /**
     * Number of parameters, i.e., occurrence of char "?"
     */
    this.parametersNumber = statement.length() - statement.replaceAll("\\?", "").length();
    this.parameters = new HashMap<>(this.parametersNumber);
  }

  /*----------------------------------------*/
  /*       Some useful, check method        */
  /*----------------------------------------*/

  /**
   * Check if the connection is closed or not.
   */
  protected void checkClosed() throws SQLException {
    if (this.isClosed()) {
      throw new SQLException("Statement already closed");
    }
  }

  /**
   * Check if the given parameter index is out of bound.
   *
   * @param parameterIndex The index parameter to check
   */
  private void checkParamsNumber(int parameterIndex) throws SQLException {
    if (parameterIndex > this.parametersNumber) {
      throw new SQLException("ParameterIndex does not correspond to a parameter marker in the SQL statement");
    }
  }

  /** Check if the given object is a valid type
   * If it's not we throw an exception.
   *
   * @param obj The object to check
   */
  private void checkValidObject(Object obj) throws SQLException {
    if (!(
        obj == null ||
        obj instanceof Boolean ||
        obj instanceof String ||
        obj instanceof Character ||
        obj instanceof Long ||
        obj instanceof Short ||
        obj instanceof Byte ||
        obj instanceof Integer ||
        obj instanceof Double ||
        obj instanceof Float ||
        obj instanceof List ||
        obj instanceof Iterable ||
        obj instanceof Map ||
        obj instanceof Iterator ||
        obj instanceof boolean[] ||
        obj instanceof String[] ||
        obj instanceof long[] ||
        obj instanceof int[] ||
        obj instanceof double[] ||
        obj instanceof float[] ||
        obj instanceof Object[])) {
      throw new SQLException("Object of type '" + obj.getClass() + "' isn't supported");
    }
  }

  /**
   * Insert a parameter into the map.
   *
   * @param index The index/key of the parameter
   * @param obj The value of the parameter
   */
  private void insertParameter(int index, Object obj) {
    this.parameters.put(Integer.toString(index), obj);
  }

  /*------------------------------------*/
  /*       Default implementation       */
  /*------------------------------------*/

  @Override public void setNull(int parameterIndex, int sqlType) throws SQLException {
    this.checkClosed();
    this.checkParamsNumber(parameterIndex);
    //@formatter:off
    if(  sqlType == ARRAY ||
      sqlType == BLOB ||
      sqlType == CLOB ||
      sqlType == DATALINK ||
      sqlType == JAVA_OBJECT ||
      sqlType == NCHAR ||
      sqlType == NCLOB ||
      sqlType == NVARCHAR ||
      sqlType == LONGNVARCHAR ||
      sqlType == REF ||
      sqlType == ROWID ||
      sqlType == SQLXML ||
      sqlType == STRUCT){
    //@formatter:on
      throw new SQLFeatureNotSupportedException("The Type you specified is not supported");
    }
    this.insertParameter(parameterIndex, null);
  }

  @Override public void setBoolean(int parameterIndex, boolean x) throws SQLException {
    this.checkClosed();
    this.checkParamsNumber(parameterIndex);
    this.insertParameter(parameterIndex, x);
  }

  @Override public void setShort(int parameterIndex, short x) throws SQLException {
    this.checkClosed();
    this.checkParamsNumber(parameterIndex);
    this.insertParameter(parameterIndex, x);
  }

  @Override public void setInt(int parameterIndex, int x) throws SQLException {
    this.checkClosed();
    this.checkParamsNumber(parameterIndex);
    this.insertParameter(parameterIndex, x);
  }

  @Override public void setLong(int parameterIndex, long x) throws SQLException {
    this.checkClosed();
    this.checkParamsNumber(parameterIndex);
    this.insertParameter(parameterIndex, x);
  }

  @Override public void setFloat(int parameterIndex, float x) throws SQLException {
    this.checkClosed();
    this.checkParamsNumber(parameterIndex);
    this.insertParameter(parameterIndex, x);
  }

  @Override public void setDouble(int parameterIndex, double x) throws SQLException {
    this.checkClosed();
    this.checkParamsNumber(parameterIndex);
    this.insertParameter(parameterIndex, x);
  }

  @Override public void setString(int parameterIndex, String x) throws SQLException {
    this.checkClosed();
    this.checkParamsNumber(parameterIndex);
    this.insertParameter(parameterIndex, x);
  }

  @Override public void setObject(int parameterIndex, Object x) throws SQLException {
    this.checkClosed();
    this.checkParamsNumber(parameterIndex);
    this.checkValidObject(x);
    this.insertParameter(parameterIndex, x);
  }

  @Override public void clearParameters() throws SQLException {
    this.checkClosed();
    this.parameters.clear();
  }

  /*-----------------------------*/
  /*       Abstract method       */
  /*-----------------------------*/

  @Override public abstract boolean execute() throws SQLException;

  @Override public abstract ResultSet executeQuery() throws SQLException;

  @Override public abstract int executeUpdate() throws SQLException;

  @Override public abstract ResultSetMetaData getMetaData() throws SQLException;

  @Override public abstract ParameterMetaData getParameterMetaData() throws SQLException;

  /*---------------------------------*/
  /*       Not implemented yet       */
  /*---------------------------------*/

  @Override public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
    throw new SQLException("Method execute(String, int) cannot be called on PreparedStatement");
  }

  @Override public boolean execute(String sql) throws SQLException {
    throw new SQLException("Method execute(String) cannot be called on PreparedStatement");
  }

  @Override public boolean execute(String sql, int[] columnIndexes) throws SQLException {
    throw new SQLException("Method execute(String, int[]) cannot be called on PreparedStatement");
  }

  @Override public boolean execute(String sql, String[] columnNames) throws SQLException {
    throw new SQLException("Method execute(String, String[]) cannot be called on PreparedStatement");
  }

  @Override public ResultSet executeQuery(String sql) throws SQLException {
    throw new SQLException("Method executeQuery(String) cannot be called on PreparedStatement");
  }

  @Override public int executeUpdate(String sql) throws SQLException {
    throw new SQLException("Method executeUpdate(String) cannot be called on PreparedStatement");
  }

  @Override public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
    throw new SQLException("Method executeUpdate(String, int) cannot be called on PreparedStatement");
  }

  @Override public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
    throw new SQLException("Method executeUpdate(String, int[]) cannot be called on PreparedStatement");
  }

  @Override public int executeUpdate(String sql, String[] columnNames) throws SQLException {
    throw new SQLException("Method executeUpdate(String, String[]) cannot be called on PreparedStatement");
  }

  @Override public void setByte(int parameterIndex, byte x) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setBytes(int parameterIndex, byte[] x) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setDate(int parameterIndex, Date x) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setTime(int parameterIndex, Time x) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setRef(int parameterIndex, Ref x) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setBlob(int parameterIndex, Blob x) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setClob(int parameterIndex, Clob x) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setArray(int parameterIndex, java.sql.Array x) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setURL(int parameterIndex, URL x) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setRowId(int parameterIndex, RowId x) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setNString(int parameterIndex, String value) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setNClob(int parameterIndex, NClob value) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setClob(int parameterIndex, Reader reader) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void setNClob(int parameterIndex, Reader reader) throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void addBatch() throws SQLException {
    throw ExceptionBuilder.buildUnsupportedOperationException();
  }

  @Override public void addBatch(String sql) throws SQLException {
    throw new SQLException("Method addBatch(String sql) cannot be called on PreparedStatement");
  }

}

