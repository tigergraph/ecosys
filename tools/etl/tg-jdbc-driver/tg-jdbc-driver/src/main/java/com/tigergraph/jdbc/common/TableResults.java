package com.tigergraph.jdbc.common;

import java.util.List;
import java.util.Map;

/** Represent a tabular result. */
public class TableResults {
  /**
   * Each row may have multiple columns, each column has its name and value. This information is
   * stored in a map. So the list of map represents a table.
   */
  private List<Map<String, Object>> results;
  // List of column attributes.
  private List<Attribute> attributeList;
  private String table_name;

  public TableResults(
      List<Map<String, Object>> results, List<Attribute> attributeList, String table_name) {
    this.results = results;
    this.attributeList = attributeList;
    this.table_name = table_name;
  }

  public List<Map<String, Object>> getResults() {
    return this.results;
  }

  public String getTableName() {
    return this.table_name;
  }

  public List<Attribute> getAttrList() {
    return this.attributeList;
  }
}
