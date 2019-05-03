package com.tigergraph.jdbc.restpp;

import org.json.JSONArray;
import org.json.JSONObject;
import com.tigergraph.jdbc.ResultSet;
import com.tigergraph.jdbc.Statement;

import java.math.BigDecimal;
import java.io.InputStream;
import java.sql.*;
import java.sql.SQLException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RestppResultSet extends ResultSet {

  List<JSONObject> results;
  private int row = -1;
  private Statement statement;
  private boolean wasNull = false;

  public RestppResultSet(Statement statement, List<JSONObject> resultList) {
    this.statement = statement;
    this.results = new ArrayList<>();
    this.results.addAll(resultList);
  }

  public Boolean addResult(JSONObject result) {
    return this.results.add(result);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected boolean innerNext() throws SQLException {
    row++;
    return (row < results.size());
  }

  @Override
  public void close() throws SQLException {
    results = null;
    row = -1;
  }

  @Override
  public boolean wasNull() throws SQLException {
    return this.wasNull;
  }

  /**
   * There is only one column, which is a JSONObject.
   */
  public Object get(int column) throws SQLDataException {
    if (column != 0) {
      throw new SQLDataException("Column " + column + " is invalid, expect 0.");
    }

    Object value = results.get(row);

    wasNull = (value == null);
    return value;
  }

  @Override
  public Object getObject(int columnIndex) throws SQLException {
    return get(0);
  }

  @Override
  public Object getObject(String columnLabel) throws SQLException {
    return get(0);
  }

  /**
   * Methods not implemented yet.
   */

  @Override public int getType() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getConcurrency() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getHoldability() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public com.tigergraph.jdbc.ResultSetMetaData getMetaData() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public InputStream getUnicodeStream(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public InputStream getUnicodeStream(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int findColumn(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getString(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public String getString(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean getBoolean(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean getBoolean(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Array getArray(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Array getArray(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public short getShort(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public short getShort(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getInt(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getInt(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public long getLong(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public long getLong(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public float getFloat(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public float getFloat(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public double getDouble(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public double getDouble(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

}

