package com.tigergraph.jdbc.restpp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;
import com.tigergraph.jdbc.ResultSet;
import com.tigergraph.jdbc.Statement;
import com.tigergraph.jdbc.restpp.driver.QueryType;
import com.tigergraph.jdbc.Attribute;
import com.tigergraph.jdbc.TableResults;

import java.math.BigDecimal;
import java.io.InputStream;
import java.sql.*;
import java.sql.SQLException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RestppResultSet extends ResultSet {

  // All the tables in the ResultSet.
  private List<TableResults> tableResults_;
  // Current table results.
  private List<Map<String, Object>> resultSets_;
  // Current row results.
  private Map<String, Object> current_resultset_;
  // Attribute list of current table.
  private List<Attribute> attribute_list_;
  private List<Integer> scale_list_ = null;
  private List<Integer> precision_list_ = null;
  // Set of current table's column names.
  private Set<String> key_set_;
  // vertex type or edge type.
  private String table_name_;
  private int table_index_ = 0;
  private int row_ = 0;
  private Statement statement_;
  private boolean wasNull_ = false;
  private boolean isGettingEdge_ = false;
  private QueryType query_type_ = QueryType.QUERY_TYPE_UNKNOWN;

  public RestppResultSet(Statement statement,
      List<JSONObject> resultList, QueryType query_type, boolean isGettingEdge) throws SQLException {
    this.statement_ = statement;
    this.query_type_ = query_type;
    this.isGettingEdge_ = isGettingEdge;
    this.tableResults_ = new ArrayList<>();
    this.resultSets_ = new ArrayList<>();
    this.key_set_ = new HashSet<String>();
    this.attribute_list_ = new ArrayList<>();
    for(int i = 0; i < resultList.size(); i++) {
      parseResult(resultList.get(i), this.query_type_);
    }
    // Add current ResultSet to table list.
    updateTableResults();
    // Move to the first table.
    isLast();
  }

  /**
   * For Spark to get schema for a loading job.
   * lineSchema is the definition of each column of the data file.
   */
  public RestppResultSet(Statement statement,
      String lineSchema) {
    this.statement_ = statement;
    this.resultSets_ = new ArrayList<>();
    this.attribute_list_ = new ArrayList<>();
    String[] tokens = lineSchema.replaceAll("\"", "").trim().split(" |,|\\(|\\)|=");
    for (int i = 0; i < tokens.length; ++i) {
      this.attribute_list_.add(new Attribute(tokens[i], "STRING", Boolean.FALSE));
    }
  }

  /**
   * Parse a vertex's schema definition.
   */
  private void parseSchemaVerex(JSONObject obj) {
    this.table_name_ = obj.getString("Name");
    // Get primary id.
    JSONObject pidObj = (JSONObject)obj.get("PrimaryId");
    String attr_name = pidObj.getString("AttributeName");
    Object value = pidObj.get("AttributeType");
    String attr_type = "";
    if (value instanceof JSONObject) {
      attr_type = ((JSONObject)value).getString("Name");
      this.attribute_list_.add(new Attribute(attr_name, attr_type, Boolean.TRUE));
    }
    // Get attributes.
    JSONArray attributes = obj.getJSONArray("Attributes");
    for(int i = 0; i < attributes.length(); i++){
      attr_name = attributes.getJSONObject(i).getString("AttributeName");
      value = attributes.getJSONObject(i).get("AttributeType");
      if (value instanceof JSONObject) {
        attr_type = ((JSONObject)value).getString("Name");
        this.attribute_list_.add(new Attribute(attr_name, attr_type, Boolean.FALSE));
      }
    }
  }

  /**
   * Parse a edge's schema definition.
   */
  private void parseSchemaEdge(JSONObject obj) {
    this.table_name_ = obj.getString("Name");
    String from_vertex_type = obj.getString("FromVertexTypeName");
    String to_vertex_type = obj.getString("ToVertexTypeName");
    /**
     * Spark will raise exception when found duplicate column names,
     * but TigerGraph edges' source vertex and target vertex can be of the same type.
     * So prefix was added here.
     */
    if (from_vertex_type.equals(to_vertex_type) && this.isGettingEdge_) {
      this.attribute_list_.add(new Attribute("from." + from_vertex_type, "STRING", Boolean.TRUE));
      this.attribute_list_.add(new Attribute("to." + to_vertex_type, "STRING", Boolean.TRUE));
    } else {
      this.attribute_list_.add(new Attribute(from_vertex_type, "STRING", Boolean.TRUE));
      this.attribute_list_.add(new Attribute(to_vertex_type, "STRING", Boolean.TRUE));
    }
    JSONArray attributes = obj.getJSONArray("Attributes");
    for(int i = 0; i < attributes.length(); i++){
      String attr_name = attributes.getJSONObject(i).getString("AttributeName");
      Object value = attributes.getJSONObject(i).get("AttributeType");
      String attr_type = "";
      if (value instanceof JSONObject) {
        attr_type = ((JSONObject)value).getString("Name");
        this.attribute_list_.add(new Attribute(attr_name, attr_type, Boolean.FALSE));
      }
    }
  }

  private void parseKeyValue(ArrayList<Attribute> attribute_list,
      String key, Object value, List<Integer> scale_list, List<Integer> precision_list) {
    String str = String.valueOf(value);
    int length = str.length();
    precision_list.add(length);
    if (value instanceof Integer) {
      attribute_list.add(new Attribute(key, "INT", Boolean.FALSE));
      scale_list.add(0);
    } else if (value instanceof Double) {
      int index = str.indexOf('.');
      if (index < 0) {
        scale_list.add(0);
      } else {
        scale_list.add(length - index - 1);
      }
      attribute_list.add(new Attribute(key, "DOUBLE", Boolean.FALSE));
    } else {
      scale_list.add(0);
      attribute_list.add(new Attribute(key, "STRING", Boolean.FALSE));
    }
  }

  /**
   * Parse a JSONObject that represents a vertex.
   */
  private void parseVertex(JSONObject obj) {
    Map<String, Object> map = new HashMap<String, Object>();
    ArrayList<Attribute> attribute_list = new ArrayList<>();
    ArrayList<Integer> scale_list = new ArrayList<>();
    ArrayList<Integer> precision_list = new ArrayList<>();
    Set<String> key_set = new HashSet<String>();
    String table_name = obj.getString("v_type");
    String id = obj.getString("v_id");
    attribute_list.add(new Attribute("id", "STRING", Boolean.TRUE));
    scale_list.add(0);
    precision_list.add(id.length());
    key_set.add("id");
    map.put("id", id);
    JSONObject attributes = obj.getJSONObject("attributes");
    Iterator<String> keysItr = attributes.keys();
    while (keysItr.hasNext()) {
      String key = keysItr.next();
      Object value = attributes.get(key);
      map.put(key, value);
      key_set.add(key);
      parseKeyValue(attribute_list, key, value, scale_list, precision_list);
    }

    addToTable(map, attribute_list, key_set, table_name, scale_list, precision_list);
  }

  /**
   * Parse a JSONObject that represents an edge.
   */
  private void parseEdge(JSONObject obj) {
    Map<String, Object> map = new HashMap<String, Object>();
    ArrayList<Attribute> attribute_list = new ArrayList<>();
    ArrayList<Integer> scale_list = new ArrayList<>();
    ArrayList<Integer> precision_list = new ArrayList<>();
    Set<String> key_set = new HashSet<String>();
    String table_name = obj.getString("e_type");
    String from_id = obj.getString("from_id");
    String from_type = obj.getString("from_type");
    String to_id = obj.getString("to_id");
    String to_type = obj.getString("to_type");

    if (from_type.equals(to_type)) {
      from_type = "from." + from_type;
      to_type = "to." + to_type;
    }

    attribute_list.add(new Attribute(from_type, "STRING", Boolean.TRUE));
    attribute_list.add(new Attribute(to_type, "STRING", Boolean.TRUE));
    scale_list.add(0);
    scale_list.add(0);
    precision_list.add(from_id.length());
    precision_list.add(to_id.length());
    key_set.add(from_type);
    key_set.add(to_type);
    map.put(from_type, from_id);
    map.put(to_type, to_id);
    JSONObject attributes = obj.getJSONObject("attributes");
    Iterator<String> keysItr = attributes.keys();
    while (keysItr.hasNext()) {
      String key = keysItr.next();
      Object value = attributes.get(key);
      map.put(key, value);
      key_set.add(key);
      parseKeyValue(attribute_list, key, value, scale_list, precision_list);
    }

    addToTable(map, attribute_list, key_set, table_name, scale_list, precision_list);
  }

  /**
   * Add current result (a row) to a table.
   */
  private void addToTable(Map<String, Object> map, ArrayList<Attribute> attribute_list,
      Set<String> key_set, String table_name, List<Integer> scale_list, List<Integer> precision_list) {
    if (this.key_set_.size() == 0) { // It is the first table.
      this.attribute_list_ = attribute_list;
      this.key_set_ = key_set;
      this.table_name_ = table_name;
      this.scale_list_ = scale_list;
      this.precision_list_ = precision_list;
      this.resultSets_.add(map);
    } else {
      if (this.key_set_.equals(key_set)) {
        /**
         * If the schema of current result is the same as current table,
         * just add the result to this table.
         */
        this.resultSets_.add(map);
        if (this.scale_list_.size() == scale_list.size()) {
          for (int i = 0; i < scale_list.size(); ++i) {
            this.scale_list_.set(i,
                Math.max(this.scale_list_.get(i), scale_list.get(i)));
          }
        }
        if (this.precision_list_.size() == precision_list.size()) {
          for (int i = 0; i < precision_list.size(); ++i) {
            this.precision_list_.set(i,
              Math.max(this.precision_list_.get(i), precision_list.get(i)));
          }
        }
      } else {
        /**
         * Otherwise, we need to store the current table,
         * and switch to a new table.
         */
        updateTableResults();
        this.attribute_list_ = attribute_list;
        this.precision_list_ = precision_list;
        this.scale_list_ = scale_list;
        this.key_set_ = key_set;
        this.table_name_ = table_name;
        this.resultSets_ = new ArrayList<>();
        this.resultSets_.add(map);
      }
    }
  }

  /**
   * Add current ResultSet to table list.
   */
  private void updateTableResults() {
    this.tableResults_.add(new TableResults(this.resultSets_,
          this.attribute_list_, this.table_name_));
    if ((this.scale_list_ == null) || (this.precision_list_ == null)) {
      // No result.
      return;
    }
    if (this.scale_list_.size() == this.attribute_list_.size()) {
      for (int i = 0; i < this.scale_list_.size(); ++i) {
        this.attribute_list_.get(i).setScale(this.scale_list_.get(i));
      }
    }
    if (this.precision_list_.size() == this.attribute_list_.size()) {
      for (int i = 0; i < this.precision_list_.size(); ++i) {
        this.attribute_list_.get(i).setPrecision(this.precision_list_.get(i));
      }
    }
  }

  /**
   * Parse an arbitrary JSONObject, which could be a vertex, an edge,
   * JSONArray, or key/value pairs.
   */
  private void parseJSONObject(JSONObject obj) {
    Map<String, Object> map = new HashMap<String, Object>();
    ArrayList<Attribute> attribute_list = new ArrayList<>();
    ArrayList<Integer> scale_list = new ArrayList<>();
    ArrayList<Integer> precision_list = new ArrayList<>();
    Set<String> key_set = new HashSet<String>();
    String table_name = "";
    if (obj.has("v_type") && obj.has("v_id")) { // It is a vertex
      parseVertex(obj);
    } else if (obj.has("e_type") && obj.has("from_id")) { // It is an edge
      parseEdge(obj);
    } else {
      // Otherwise, enumerate all the keys.
      Iterator<String> keysItr = obj.keys();
      while (keysItr.hasNext()) {
        String key = keysItr.next();
        Object value = obj.get(key);
        if (value instanceof JSONObject) {
          parseJSONObject((JSONObject)value);
        } else if (value instanceof JSONArray) {
          for(int i = 0; i < ((JSONArray)value).length(); i++){
            parseJSONObject(((JSONArray)value).getJSONObject(i));
          }
        } else {
          map.put(key, value);
          key_set.add(key);
          parseKeyValue(attribute_list, key, value, scale_list, precision_list);
          // Use the first key as table name.
          if (table_name == "") {
            table_name = key;
          }
        }
      }
    }

    if (map.size() > 0) {
      addToTable(map, attribute_list, key_set, table_name, scale_list, precision_list);
    }
  }

  /**
   * Parse a JSONObject based on query type.
   */
  private void parseResult(JSONObject obj, QueryType query_type) throws SQLException {
    switch (query_type) {
      case QUERY_TYPE_GRAPH_GET_VERTEX:
        parseVertex(obj);
        break;
      case QUERY_TYPE_GRAPH_GET_EDGE:
        parseEdge(obj);
        break;
      case QUERY_TYPE_SCHEMA_EDGE:
        parseSchemaEdge(obj);
        break;
      case QUERY_TYPE_SCHEMA_VERTEX:
        parseSchemaVerex(obj);
        break;
      case QUERY_TYPE_BUILTIN:
      case QUERY_TYPE_INSTALLED:
      case QUERY_TYPE_INTERPRETED:
      case QUERY_TYPE_GRAPH:
      default:
        parseJSONObject(obj);
        break;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  protected boolean innerNext() throws SQLException {
    if (row_ < this.resultSets_.size()) {
      current_resultset_ = this.resultSets_.get(row_);
    }
    row_++;
    return (row_ <= this.resultSets_.size());
  }

  // move to next table
  public boolean isLast() throws SQLException {
    if (table_index_ < this.tableResults_.size()) {
      this.resultSets_ = this.tableResults_.get(table_index_).getResults();
      this.attribute_list_ = this.tableResults_.get(table_index_).getAttrList();
      this.table_name_ = this.tableResults_.get(table_index_).getTableName();
    }
    table_index_++;
    row_ = 0;
    return (table_index_ > this.tableResults_.size());
  }

  @Override
  public void close() throws SQLException {
    if (null != this.tableResults_) {
      this.tableResults_.clear();
      this.tableResults_ = null;
    }
    if (null != this.resultSets_) {
      this.resultSets_.clear();
      this.resultSets_ = null;
    }
    row_ = 0;
  }

  @Override
  public boolean wasNull() throws SQLException {
    return this.wasNull_;
  }

  public Object get(int columnIndex) throws SQLException {
    if (columnIndex > this.attribute_list_.size()) {
      throw new SQLException("Column index out of range.");
    }
    Object value = this.resultSets_.get(columnIndex - 1);

    wasNull_ = (value == null);
    return value;
  }

  @Override
  public Object getObject(int columnIndex) throws SQLException {
    if (columnIndex > this.attribute_list_.size()) {
      throw new SQLException("Column index out of range.");
    }
    String name = this.attribute_list_.get(columnIndex - 1).getName();
    return getObject(name);
  }

  @Override
  public Object getObject(String columnLabel) throws SQLException {
    if (this.current_resultset_.containsKey(columnLabel)) {
      return this.current_resultset_.get(columnLabel);
    }
    return null;
  }

  @Override public String getString(int columnIndex) throws SQLException {
    Object obj = getObject(columnIndex);
    return String.valueOf(obj);
  }

  @Override public String getString(String columnLabel) throws SQLException {
    Object obj = getObject(columnLabel);
    return String.valueOf(obj);
  }

  @Override public int getInt(int columnIndex) throws SQLException {
    Object obj = getObject(columnIndex);
    return Integer.parseInt(obj.toString());
  }

  @Override public int getInt(String columnLabel) throws SQLException {
    Object obj = getObject(columnLabel);
    return Integer.parseInt(obj.toString());
  }

  @Override public short getShort(int columnIndex) throws SQLException {
    Object obj = getObject(columnIndex);
    return Short.parseShort(obj.toString());
  }

  @Override public short getShort(String columnLabel) throws SQLException {
    Object obj = getObject(columnLabel);
    return Short.parseShort(obj.toString());
  }

  @Override public long getLong(int columnIndex) throws SQLException {
    Object obj = getObject(columnIndex);
    return Long.parseLong(obj.toString());
  }

  @Override public long getLong(String columnLabel) throws SQLException {
    Object obj = getObject(columnLabel);
    return Long.parseLong(obj.toString());
  }

  @Override public float getFloat(int columnIndex) throws SQLException {
    Object obj = getObject(columnIndex);
    return Float.parseFloat(obj.toString());
  }

  @Override public float getFloat(String columnLabel) throws SQLException {
    Object obj = getObject(columnLabel);
    return Float.parseFloat(obj.toString());
  }

  @Override public double getDouble(int columnIndex) throws SQLException {
    Object obj = getObject(columnIndex);
    return Double.parseDouble(obj.toString());
  }

  @Override public double getDouble(String columnLabel) throws SQLException {
    Object obj = getObject(columnLabel);
    return Double.parseDouble(obj.toString());
  }

  @Override public boolean getBoolean(int columnIndex) throws SQLException {
    Object obj = getObject(columnIndex);
    return Boolean.parseBoolean(obj.toString());
  }

  @Override public boolean getBoolean(String columnLabel) throws SQLException {
    Object obj = getObject(columnLabel);
    return Boolean.parseBoolean(obj.toString());
  }

  @Override public com.tigergraph.jdbc.ResultSetMetaData getMetaData() throws SQLException {
    return new com.tigergraph.jdbc.ResultSetMetaData(this.table_name_, this.attribute_list_);
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

  @Override public Array getArray(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public Array getArray(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

}
