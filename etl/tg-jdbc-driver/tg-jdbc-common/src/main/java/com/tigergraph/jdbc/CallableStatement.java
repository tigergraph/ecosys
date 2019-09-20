package com.tigergraph.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Calendar;
import java.util.Map;

public abstract class CallableStatement implements java.sql.CallableStatement {

  /*
   * Methods not implemented yet.
   */

  @Override public SQLWarning getWarnings() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void clearWarnings() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void registerOutParameter(int parameterIndex,
    int sqlType, int scale) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean wasNull() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getString(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean getBoolean(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public byte getByte(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public short getShort(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getInt(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public long getLong(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public float getFloat(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public double getDouble(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public byte[] getBytes(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Date getDate(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Time getTime(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Timestamp getTimestamp(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Object getObject(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Object getObject(int parameterIndex,
    Map<String, Class<?>> map) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Ref getRef(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Blob getBlob(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Clob getClob(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Array getArray(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void registerOutParameter(int parameterIndex,
    int sqlType, String typeName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void registerOutParameter(String parameterName,
    int sqlType) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void registerOutParameter(String parameterName,
    int sqlType, int scale) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void registerOutParameter(String parameterName,
    int sqlType, String typeName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public URL getURL(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setURL(String parameterName, URL val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNull(String parameterName, int sqlType) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBoolean(String parameterName, boolean val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setByte(String parameterName, byte val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setShort(String parameterName, short val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setInt(String parameterName, int val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setLong(String parameterName, long val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setFloat(String parameterName, float val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setDouble(String parameterName, double val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBigDecimal(String parameterName, BigDecimal val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setString(String parameterName, String val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBytes(String parameterName, byte[] val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setDate(String parameterName, Date val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setTime(String parameterName, Time val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setTimestamp(String parameterName, Timestamp val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setAsciiStream(String parameterName,
    InputStream val, int length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBinaryStream(String parameterName,
    InputStream val, int length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setObject(String parameterName,
    Object val, int targetSqlType, int scale) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setObject(String parameterName,
    Object val, int targetSqlType) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setObject(String parameterName, Object val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setCharacterStream(String parameterName,
    Reader reader, int length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setDate(String parameterName, Date val, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setTime(String parameterName, Time val, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setTimestamp(String parameterName,
    Timestamp val, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNull(String parameterName,
    int sqlType, String typeName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getString(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean getBoolean(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public byte getByte(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public short getShort(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getInt(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public long getLong(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public float getFloat(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public double getDouble(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public byte[] getBytes(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Date getDate(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Time getTime(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Timestamp getTimestamp(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Object getObject(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public BigDecimal getBigDecimal(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Object getObject(String parameterName,
    Map<String, Class<?>> map) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Ref getRef(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Blob getBlob(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Clob getClob(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Array getArray(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Date getDate(String parameterName, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Time getTime(String parameterName, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public URL getURL(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public RowId getRowId(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public RowId getRowId(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setRowId(String parameterName, RowId val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNString(String parameterName, String value) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNCharacterStream(String parameterName,
    Reader value, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNClob(String parameterName, NClob value) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setClob(String parameterName,
    Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBlob(String parameterName,
    InputStream inputStream, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNClob(String parameterName,
    Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public NClob getNClob(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public NClob getNClob(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public SQLXML getSQLXML(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public SQLXML getSQLXML(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getNString(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getNString(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Reader getNCharacterStream(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Reader getNCharacterStream(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Reader getCharacterStream(int parameterIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Reader getCharacterStream(String parameterName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBlob(String parameterName, Blob val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setClob(String parameterName, Clob val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setAsciiStream(String parameterName,
    InputStream val, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBinaryStream(String parameterName,
    InputStream val, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setCharacterStream(String parameterName,
    Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setAsciiStream(String parameterName,
    InputStream val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBinaryStream(String parameterName,
    InputStream val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setCharacterStream(String parameterName,
    Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNCharacterStream(String parameterName,
    Reader value) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setClob(String parameterName, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBlob(String parameterName,
    InputStream inputStream) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNClob(String parameterName, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public ResultSet executeQuery() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int executeUpdate() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNull(int parameterIndex, int sqlType) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBoolean(int parameterIndex, boolean val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setByte(int parameterIndex, byte val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setShort(int parameterIndex, short val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setInt(int parameterIndex, int val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setLong(int parameterIndex, long val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setFloat(int parameterIndex, float val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setDouble(int parameterIndex, double val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBigDecimal(int parameterIndex, BigDecimal val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setString(int parameterIndex, String val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBytes(int parameterIndex, byte[] val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setDate(int parameterIndex, Date val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setTime(int parameterIndex, Time val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setTimestamp(int parameterIndex, Timestamp val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setAsciiStream(int parameterIndex,
    InputStream val, int length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBinaryStream(int parameterIndex,
    InputStream val, int length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void clearParameters() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setObject(int parameterIndex,
    Object val, int targetSqlType) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setObject(int parameterIndex, Object val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean execute() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void addBatch() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setCharacterStream(int parameterIndex,
    Reader reader, int length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setRef(int parameterIndex, Ref val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBlob(int parameterIndex, Blob val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setClob(int parameterIndex, Clob val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setArray(int parameterIndex, java.sql.Array val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public ResultSetMetaData getMetaData() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setDate(int parameterIndex, Date val, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setTime(int parameterIndex, Time val, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setTimestamp(int parameterIndex,
    Timestamp val, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNull(int parameterIndex,
    int sqlType, String typeName) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setURL(int parameterIndex, URL val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public java.sql.ParameterMetaData getParameterMetaData() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setRowId(int parameterIndex, RowId val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNString(int parameterIndex, String value) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNCharacterStream(int parameterIndex,
    Reader value, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNClob(int parameterIndex, NClob value) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setClob(int parameterIndex,
    Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBlob(int parameterIndex,
    InputStream inputStream, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNClob(int parameterIndex,
    Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setObject(int parameterIndex,
    Object val, int targetSqlType, int scaleOrLength) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setAsciiStream(int parameterIndex,
    InputStream val, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBinaryStream(int parameterIndex,
    InputStream val, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setCharacterStream(int parameterIndex,
    Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setAsciiStream(int parameterIndex, InputStream val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBinaryStream(int parameterIndex, InputStream val) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setClob(int parameterIndex, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setNClob(int parameterIndex, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public ResultSet executeQuery(String sql) throws SQLException {
    throw new SQLException("Method executeQuery(String) cannot be called on PreparedStatement");
  }

  @Override public int executeUpdate(String sql) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void close() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getMaxFieldSize() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setMaxFieldSize(int max) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getMaxRows() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setMaxRows(int max) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setEscapeProcessing(boolean enable) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getQueryTimeout() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setQueryTimeout(int seconds) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void cancel() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setCursorName(String name) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean execute(String sql) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public ResultSet getResultSet() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getUpdateCount() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean getMoreResults() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setFetchDirection(int direction) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getFetchDirection() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setFetchSize(int rows) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getFetchSize() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getResultSetConcurrency() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getResultSetType() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void addBatch(String sql) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void clearBatch() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int[] executeBatch() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Connection getConnection() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean getMoreResults(int current) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public ResultSet getGeneratedKeys() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int executeUpdate(String sql, String[] columnNames) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean execute(String sql, int[] columnIndexes) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean execute(String sql, String[] columnNames) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getResultSetHoldability() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isClosed() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  protected void checkClosed() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setPoolable(boolean poolable) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isPoolable() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void closeOnCompletion() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isCloseOnCompletion() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }
}

