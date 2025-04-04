package com.tigergraph.jdbc.common;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Ref;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Array;
import java.sql.RowId;
import java.sql.NClob;
import java.sql.SQLXML;
import java.util.Calendar;
import java.util.HashMap;

import com.tigergraph.jdbc.restpp.RestppArray;

public abstract class PreparedStatement extends Statement implements java.sql.PreparedStatement {

  protected HashMap<Integer, Object> parameters;
  protected String statement;

  /** Default constructor */
  protected PreparedStatement(Connection connection, String preparedStatement) {
    super(connection);
    this.statement = preparedStatement;
    this.parameters = new HashMap<>();
  }

  /** Insert a parameter into the map. */
  private void insertParameter(int index, Object obj) {
    this.parameters.put(index, obj);
  }

  /*
   * Methods with default implementation.
   */

  @Override
  public void setNull(int parameterIndex, int sqlType) throws SQLException {
    this.insertParameter(parameterIndex, null);
  }

  @Override
  public void setBoolean(int parameterIndex, boolean val) throws SQLException {
    this.insertParameter(parameterIndex, val);
  }

  // < short, int, long => TG int (8 bytes) >

  @Override
  public void setShort(int parameterIndex, short val) throws SQLException {
    setLong(parameterIndex, val);
  }

  @Override
  public void setInt(int parameterIndex, int val) throws SQLException {
    setLong(parameterIndex, val);
  }

  @Override
  public void setLong(int parameterIndex, long val) throws SQLException {
    this.insertParameter(parameterIndex, val);
  }

  @Override
  public void setFloat(int parameterIndex, float val) throws SQLException {
    this.insertParameter(parameterIndex, val);
  }

  @Override
  public void setDouble(int parameterIndex, double val) throws SQLException {
    this.insertParameter(parameterIndex, val);
  }

  /**
   * TigerGraph will convert decimal to double which can cause precision loss For spark DecimalType,
   * it can represent a UINT64 or a decimal
   */
  @Override
  public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
    // Default value of Double in TG is 0
    if (x == null) {
      x = new BigDecimal(0);
    }
    // TG can't parse the scientific notation for UINT64, e.g. 1.23E+5 (but works for DOUBLE)
    // So if the decimal is an integer, cast it to BigInteger
    try {
      this.insertParameter(parameterIndex, x.toBigIntegerExact());
    } catch (ArithmeticException e) {
      // x has a nonzero fractional part.
      this.insertParameter(parameterIndex, x.doubleValue());
    }
  }

  // < bytes, timestamp, date, string => string => TG datetime/string >

  @Override
  public void setString(int parameterIndex, String val) throws SQLException {
    // Default value of String in TG is ""
    if (val == null) {
      val = "";
    }
    this.insertParameter(parameterIndex, val);
  }

  /** Only for UTF-8 encoding */
  @Override
  public void setBytes(int parameterIndex, byte[] x) throws SQLException {
    // Default value of String in TG is ""
    if (x == null) {
      x = new byte[] {};
    }
    setString(parameterIndex, new String(x, StandardCharsets.UTF_8));
  }

  /** TigerGraph will drop fractional digits (YYYY-MM-DD hh:mm:ss.fffff --> YYYY-MM-DD hh:mm:ss) */
  @Override
  public void setTimestamp(int parameterIndex, Timestamp val) throws SQLException {
    // Default value of Datetime in TG is "1970-01-01 00:00:00"
    if (val == null) {
      val = new Timestamp(0);
    }
    setString(parameterIndex, val.toString().split("\\.")[0]);
  }

  /** TigerGraph will add time automatically (YYYY-MM-DD --> YYYY-MM-DD 00:00:00) */
  @Override
  public void setDate(int parameterIndex, Date val) throws SQLException {
    // Default value of Datetime in TG is "1970-01-01 00:00:00"
    if (val == null) {
      val = new Date(0);
    }
    setString(parameterIndex, val.toString());
  }

  /**
   * Insert the attributes of the LIST/SET type.
   *
   * <p>Use <code>public Array createArrayOf(String typeName, Object[] elements)</code> to create an
   * Array.
   */
  @Override
  public void setArray(int parameterIndex, Array val) throws SQLException {
    // Default value of Array in TG is []
    if (val == null) {
      val = new RestppArray("ANY", new Object[] {});
    }
    this.insertParameter(parameterIndex, val);
  }

  @Override
  public void setObject(int parameterIndex, Object val) throws SQLException {
    this.insertParameter(parameterIndex, val);
  }

  @Override
  public void clearParameters() throws SQLException {
    this.parameters.clear();
  }

  /*
   * Abstract Methods.
   */

  @Override
  public abstract boolean execute() throws SQLException;

  @Override
  public abstract ResultSet executeQuery() throws SQLException;

  @Override
  public abstract int executeUpdate() throws SQLException;

  @Override
  public abstract ResultSetMetaData getMetaData() throws SQLException;

  @Override
  public abstract ParameterMetaData getParameterMetaData() throws SQLException;

  /*
   * Methods not implemented yet.
   */

  @Override
  public void setUnicodeStream(int parameterIndex, InputStream val, int length)
      throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public boolean execute(String sql) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public boolean execute(String sql, int[] columnIndexes) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public boolean execute(String sql, String[] columnNames) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public ResultSet executeQuery(String sql) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public int executeUpdate(String sql) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public int executeUpdate(String sql, String[] columnNames) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setByte(int parameterIndex, byte val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setTime(int parameterIndex, Time val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream val, int length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream val, int length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setObject(int parameterIndex, Object val, int targetSqlType) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, int length)
      throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setRef(int parameterIndex, Ref val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setBlob(int parameterIndex, Blob val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setClob(int parameterIndex, Clob val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setDate(int parameterIndex, Date val, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setTime(int parameterIndex, Time val, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setTimestamp(int parameterIndex, Timestamp val, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setURL(int parameterIndex, URL val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setRowId(int parameterIndex, RowId val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setNString(int parameterIndex, String value) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value, long length)
      throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setNClob(int parameterIndex, NClob value) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream, long length)
      throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setObject(int parameterIndex, Object val, int targetSqlType, int scaleOrLength)
      throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream val, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream val, long length)
      throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, long length)
      throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setClob(int parameterIndex, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void setNClob(int parameterIndex, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void addBatch() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public void addBatch(String sql) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }
}
