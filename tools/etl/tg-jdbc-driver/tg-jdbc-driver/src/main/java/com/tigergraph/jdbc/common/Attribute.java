package com.tigergraph.jdbc.common;

/** Used to describe attribute of each column in a table. */
public class Attribute {
  private String name;
  private String type;
  private Boolean is_primary_key = Boolean.FALSE;
  private int scale;
  private int precision;

  public Attribute(String name, String type, Boolean is_primary_key) {
    this.name = name;
    this.type = type;
    this.is_primary_key = is_primary_key;
    this.scale = 0;
    this.precision = 0;
  }

  public String getName() {
    return this.name;
  }

  public String getType() {
    return this.type;
  }

  public Boolean isPrimaryKey() {
    return this.is_primary_key;
  }

  public int getScale() {
    return this.scale;
  }

  public void setScale(int scale) {
    this.scale = scale;
  }

  public int getPrecision() {
    return this.precision;
  }

  public void setPrecision(int precision) {
    this.precision = precision;
  }
}
