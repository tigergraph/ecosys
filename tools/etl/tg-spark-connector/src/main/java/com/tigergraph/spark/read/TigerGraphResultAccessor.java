/**
 * Copyright (c) 2024 TigerGraph Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tigergraph.spark.read;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.tigergraph.spark.TigerGraphConnection;
import com.tigergraph.spark.log.LoggerFactory;
import com.tigergraph.spark.util.Utils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;

/**
 * An extension of Spark dataframe schema. It can tell how to map the dataframe column to the exact
 * json path of the TG query results.
 */
public class TigerGraphResultAccessor implements Serializable {

  private static final Logger logger = LoggerFactory.getLogger(TigerGraphConnection.class);
  private static final String EXTRACT_FIRST_OBJ =
      "0:"; // extract first obj of first PRING statement

  private List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
  private StructType schema = new StructType();
  // For extracting obj from query results
  private Boolean extractObj = false;
  private Integer rowNumber;
  private String objKey;

  /**
   * User provided schema, the column name will be used to searched in the query results JSON
   * (recursively if not present in the first level)
   */
  public static TigerGraphResultAccessor fromExternalSchema(
      StructType schema, String resultsExtract) {
    TigerGraphResultAccessor accessor = new TigerGraphResultAccessor(resultsExtract);
    accessor.schema = schema;
    StructField[] fields = schema.fields();
    for (StructField field : fields) {
      accessor.addAccessor(field.name(), "/".concat(field.name()), true);
    }
    return accessor;
  }

  /**
   * Input meta format: { "Config": {...}, "Attributes": [...], "PrimaryId": {...}, "Name": "person"
   * }
   * https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_show_graph_schema_metadata
   *
   * <p>Parsed schema: v_id | attribute 1 | attribute 2 | ... | attribute n
   */
  public static TigerGraphResultAccessor fromVertexMeta(JsonNode meta, Set<String> columnPrune) {
    TigerGraphResultAccessor accessor = new TigerGraphResultAccessor();
    // Parse primary id type
    String vIdType = meta.path("PrimaryId").path("AttributeType").path("Name").asText("STRING");
    accessor.add("v_id", "/v_id", vIdType, false);
    // Parse attributes type
    Iterator<JsonNode> attrIter = meta.path("Attributes").elements();
    while (attrIter.hasNext()) {
      JsonNode attr = attrIter.next();
      String attrName = attr.path("AttributeName").asText();
      String attrType = attr.path("AttributeType").path("Name").asText("STRING");
      if (!Utils.isEmpty(attrName) && (columnPrune == null || columnPrune.contains(attrName))) {
        accessor.add(attrName, "/attributes/".concat(attrName), attrType, false);
      }
    }
    return accessor;
  }

  /**
   * Input meta format: { "IsDirected": false, "ToVertexTypeName": "company", "Config": {},
   * "Attributes": [...], "FromVertexTypeName": "person", "Name": "worksFor" }
   * https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_show_graph_schema_metadata
   *
   * <p>Parsed schema: from_type | from_id | to_type | to_id | attribute 1 | attribute 2 | ... |
   * attribute n
   */
  public static TigerGraphResultAccessor fromEdgeMeta(JsonNode meta, Set<String> columnPrune) {
    TigerGraphResultAccessor accessor = new TigerGraphResultAccessor();
    // Fixed schema
    accessor
        .add("from_type", "/from_type", "STRING", false)
        .add("from_id", "/from_id", "STRING", false)
        .add("to_type", "/to_type", "STRING", false)
        .add("to_id", "/to_id", "STRING", false);

    // Parse attributes type
    Iterator<JsonNode> attrIter = meta.path("Attributes").elements();
    while (attrIter.hasNext()) {
      JsonNode attr = attrIter.next();
      String attrName = attr.path("AttributeName").asText();
      String attrType = attr.path("AttributeType").path("Name").asText("STRING");
      if (!Utils.isEmpty(attrName) && (columnPrune == null || columnPrune.contains(attrName))) {
        accessor.add(attrName, "/attributes/".concat(attrName), attrType, false);
      }
    }
    return accessor;
  }

  /*
   * Parse the schema of the query output based on query signature(including output format).
   *
   * 1) multi-print query: each print will be a row
   * 2) vertex expression set: each vertex will be a row
   * 3) MapAccum: both key and value are meaningful, will have 2 columns: key and value
   * 4) non-map accum: each element will be a row
   * 5) unknown/hard-to-determine schema: read the entire JSON object as a row
   *
   * <p>Note: can use resultsExtract to extract one obj from multi-print query and flatten
   * it.
   */
  public static TigerGraphResultAccessor fromQueryMeta(JsonNode meta, String resultsExtract) {
    TigerGraphResultAccessor accessor = new TigerGraphResultAccessor(resultsExtract);
    JsonNode objMeta = null;
    if (accessor.extractObj()) {
      // explicitly extract an obj from query output
      if (meta.get(accessor.getRowNumber()) != null
          && meta.get(accessor.getRowNumber()).get(accessor.getObjKey()) != null) {
        objMeta = meta.get(accessor.getRowNumber()).get(accessor.getObjKey());
      } else {
        return fromUnknownMeta(resultsExtract);
      }
    } else if (meta.isArray() && meta.size() == 1 && meta.get(0).size() == 1) {
      // Only single PRINT statement and only PRINT one object
      objMeta = meta.get(0).elements().next();
      resultsExtract = EXTRACT_FIRST_OBJ;
    }

    if (objMeta != null) {
      // Infer the results type
      if (objMeta.isTextual()) {
        String format = objMeta.asText();
        List<String> eleType = extractElementType(format);
        if (format.startsWith("ListAccum")
            || format.startsWith("SetAccum")
            || format.startsWith("BagAccum")) {
          return fromCollectionAccumQueryMeta(eleType, resultsExtract);
        } else if (format.startsWith("ArrayAccum")) {
          return fromArrayAccumQueryMeta(eleType, resultsExtract);
        } else if (format.startsWith("HeapAccum")) {
          return fromHeapAccumQueryMeta(eleType, resultsExtract);
        } else if (format.startsWith("GroupByAccum")) {
          return fromGroupByAccumQueryMeta(eleType, resultsExtract);
        } else if (format.startsWith("MapAccum")) {
          return fromMapAccumQueryMeta(eleType, resultsExtract);
        }
        // o/w, go to the buttom, non-extractable query result
      } else if (objMeta.isArray()) {
        // typed vertex/edge set
        if (objMeta.size() == 1) {
          JsonNode ele = objMeta.elements().next();
          if ((ele.has("v_id") && ele.has("attributes"))) {
            return fromVertexSetQueryMeta(ele, resultsExtract);
          }
        }
        return fromNormalQueryMeta(objMeta, resultsExtract);
      }
    }
    return fromNormalQueryMeta(meta, null);
  }

  /**
   * When failed to get the schema, or no common schema for each row, and no external schema
   * provided, then use the default schema "results STRING" to parse the entire JSON object.
   *
   * <pre>
   * E.g.: [{"a":123},{"b":456,"c":789}] =>
   * |           results |
   * |         {"a":123} |
   * | {"b":456,"c":789} |
   * </pre>
   */
  public static TigerGraphResultAccessor fromUnknownMeta(String resultsExtract) {
    logger.warn(
        "Failed to infer schema, use default schema 'results STRING'. You can set custom schema"
            + " manually based on the output JSON keys.");
    TigerGraphResultAccessor accessor = new TigerGraphResultAccessor(resultsExtract);
    accessor.add("results", "", "STRING", false);
    return accessor;
  }

  public List<FieldMeta> getFieldMetas() {
    return this.fieldMetas;
  }

  /**
   * Parse the query result schema if and only if the schema is unique. <br>
   *
   * <p>Input meta format: {"output": [{"a":"string", "b":"int"}, {"c":"string", "d":"int"}]} => |
   * results |
   *
   * <p>Input meta format: {"output": [{"a":"string", "b":"int"}, {"b":"int", "a":"string"}]} => | a
   * | b |
   */
  private static TigerGraphResultAccessor fromNormalQueryMeta(
      JsonNode meta, String resultsExtract) {
    // The Jackson can compare JsonNode via `equal(obj)` regardless of the order of the fields
    Set<JsonNode> dedupMeta = new HashSet<>();
    meta.iterator().forEachRemaining((ele) -> dedupMeta.add(ele));
    if (dedupMeta.size() != 1) {
      // different lines have different schemas, so we don't flatten it
      return fromUnknownMeta(resultsExtract);
    } else {
      TigerGraphResultAccessor accessor = new TigerGraphResultAccessor(resultsExtract);
      JsonNode uniqueMeta = dedupMeta.iterator().next();
      Iterator<Entry<String, JsonNode>> iter = uniqueMeta.fields();
      while (iter.hasNext()) {
        Entry<String, JsonNode> field = iter.next();
        accessor.add(
            field.getKey(), "/".concat(field.getKey()), field.getValue().asText("STRING"), false);
      }
      return accessor;
    }
  }

  /** Parse schema for vertex expression set */
  private static TigerGraphResultAccessor fromVertexSetQueryMeta(
      JsonNode meta, String resultsExtract) {
    TigerGraphResultAccessor accessor = new TigerGraphResultAccessor(resultsExtract);
    Iterator<Entry<String, JsonNode>> iter = meta.fields();
    Iterator<Entry<String, JsonNode>> attrIter = null;
    while (iter.hasNext()) {
      Entry<String, JsonNode> field = iter.next();
      String key = field.getKey();
      // extract the attributes to top level
      if ("attributes".equals(key)) {
        attrIter = field.getValue().fields();
        continue;
      }
      String typeStr = field.getValue().asText("STRING");
      // HACK: GSQL incorrectly return the v_id type, mandatorily set it to STRING to avoid error.
      if ("v_id".equals(key)) {
        typeStr = "STRING";
      }
      accessor.add(key, "/".concat(key), typeStr, false);
    }
    if (attrIter != null) {
      while (attrIter.hasNext()) {
        Entry<String, JsonNode> attrField = attrIter.next();
        String attrKey = attrField.getKey();
        String attrTypeStr = attrField.getValue().asText("STRING");
        accessor.add(attrKey, "/attributes/".concat(attrKey), attrTypeStr, false);
      }
    }
    return accessor;
  }

  // General collection types like List, Set, Bag and Array
  private static TigerGraphResultAccessor fromCollectionAccumQueryMeta(
      List<String> eleType, String resultsExtract) {
    if (eleType == null || eleType.size() != 1) {
      eleType = Arrays.asList("STRING");
    }
    TigerGraphResultAccessor accessor = new TigerGraphResultAccessor(resultsExtract);
    accessor.add("results", "", eleType.get(0), false);
    return accessor;
  }

  // Use column "key" and "value" to place the JSON keys and values
  private static TigerGraphResultAccessor fromMapAccumQueryMeta(
      List<String> eleType, String resultsExtract) {
    if (eleType == null || eleType.size() != 2) {
      eleType = Arrays.asList("STRING", "STRING");
    }
    TigerGraphResultAccessor accessor = new TigerGraphResultAccessor(resultsExtract);
    accessor.add("key", "/key", eleType.get(0), false);
    accessor.add("value", "/value", eleType.get(1), false);
    return accessor;
  }

  private static TigerGraphResultAccessor fromArrayAccumQueryMeta(
      List<String> eleTypes, String resultsExtract) {
    // Currently the query signature API can't tell the dimension of the ArrayAccum,
    // thus we don't know the element is a primitive type or an array
    return fromUnknownMeta(resultsExtract);
  }

  private static TigerGraphResultAccessor fromGroupByAccumQueryMeta(
      List<String> eleType, String resultsExtract) {
    if (eleType == null || eleType.size() == 0) {
      return fromUnknownMeta(resultsExtract);
    }
    TigerGraphResultAccessor accessor = new TigerGraphResultAccessor(resultsExtract);
    for (String ele : eleType) {
      // The GroupByAccum's element type is followed by alias, e.g.:
      // GroupByAccum<int a, string b, MaxAccum<int> maxa, ListAccum<ListAccum<int>> lists>
      int lastSpaceIndex = ele.lastIndexOf(' ');
      if (lastSpaceIndex == -1) {
        return fromUnknownMeta(resultsExtract);
      }
      String type = ele.substring(0, lastSpaceIndex).trim();
      String alias = ele.substring(lastSpaceIndex + 1);
      accessor.add(alias, "/".concat(alias), type, false);
    }
    return accessor;
  }

  private static TigerGraphResultAccessor fromHeapAccumQueryMeta(
      List<String> eleType, String resultsExtract) {
    // So far we are not able to detect the tuple format of a heap accum
    return fromUnknownMeta(resultsExtract);
  }

  public StructType getSchema() {
    return schema;
  }

  public Boolean extractObj() {
    return extractObj;
  }

  public Integer getRowNumber() {
    return rowNumber;
  }

  public String getObjKey() {
    return objKey;
  }

  /**
   * Map TigerGraph data types to Spark types. Currently all the complex data types are just mapped
   * to String.
   *
   * @param tgType
   * @return
   */
  protected static DataType mapTGTypeToSparkType(String tgType) {
    if (Utils.isEmpty(tgType)) return DataTypes.StringType;
    // Single value accumulators
    if (tgType.startsWith("SumAccum")
        || tgType.startsWith("MinAccum")
        || tgType.startsWith("MaxAccum")) {
      List<String> ele = extractElementType(tgType);
      if (ele != null && ele.size() == 1) tgType = ele.get(0);
      else tgType = "string";
    }
    tgType = tgType.toLowerCase();
    switch (tgType) {
      case "int":
      case "uint":
        return DataTypes.createDecimalType(38, 0);
      case "float":
        return DataTypes.FloatType;
      case "double":
      case "avgaccum":
        return DataTypes.DoubleType;
      case "bool":
      case "boolean":
      case "andaccum":
      case "oraccum":
        return DataTypes.BooleanType;
      case "fixed_binary":
        return DataTypes.BinaryType;
      case "string":
      default:
        return DataTypes.StringType;
    }
  }

  /**
   * E.g. MapAccum<vertex, ListAccum<vertex>> => [vertex, ListAccum<vertex>] Note: it only extract
   * the first level element type
   *
   * @param accumType
   * @return
   */
  protected static List<String> extractElementType(String accumType) {
    StringBuilder buf = new StringBuilder();
    List<String> res = new ArrayList<>();
    Stack<Character> angleBracketMatch = new Stack<>();
    // HeapAccum parameters, which should be skipped
    // E.g. HeapAccum<My_Tuple<string, int>>(4, last_name ASC) => HeapAccum<My_Tuple<string, int>>
    Boolean inParentheses = false;
    Boolean finished = false;
    for (char ch : accumType.toCharArray()) {
      switch (ch) {
        case '<':
          angleBracketMatch.add(ch);
          if (angleBracketMatch.size() > 1) {
            buf.append(ch);
          }
          break;
        case '>':
          if (!angleBracketMatch.isEmpty()) {
            angleBracketMatch.pop();
          }
          if (!angleBracketMatch.isEmpty()) {
            buf.append(ch);
          } else {
            finished = true;
            res.add(buf.toString().trim());
          }
          break;
        case ',':
          if (inParentheses) break;
          if (angleBracketMatch.size() == 1) {
            res.add(buf.toString().trim());
            buf.setLength(0);
          } else if (angleBracketMatch.size() > 1) {
            buf.append(ch);
          }
          break;
        case '(':
          inParentheses = true;
          break;
        case ')':
          inParentheses = false;
          break;
        default:
          if (angleBracketMatch.size() >= 1 && !inParentheses) {
            buf.append(ch);
          }
          break;
      }
      if (finished) break;
    }
    if (!angleBracketMatch.isEmpty()) {
      // can't match the starting bracket
      // fallback to empty element
      res.clear();
    }
    return res;
  }

  private TigerGraphResultAccessor() {}

  private TigerGraphResultAccessor(String resultExtract) {
    this.parseResultsExtract(resultExtract);
  }

  /**
   * Add accessor(json pointer) for the column.
   *
   * @param colName
   * @param jsPath
   * @param isQueryable whether search recursively
   */
  private TigerGraphResultAccessor addAccessor(String colName, String jsPath, Boolean isQueryable) {
    this.fieldMetas.add(FieldMeta.fromValue(colName, jsPath, isQueryable));
    return this;
  }

  /**
   * Add accessor(json pointer) for the column, as well as Spark df schema.
   *
   * @param colName
   * @param jsPath
   * @param tgType
   * @param isQueryable whether search recursively
   */
  private TigerGraphResultAccessor add(
      String colName, String jsPath, String tgType, Boolean isQueryable) {
    this.fieldMetas.add(FieldMeta.fromValue(colName, jsPath, isQueryable));
    this.schema = schema.add(colName, mapTGTypeToSparkType(tgType));
    return this;
  }

  private void parseResultsExtract(String resultsExtract) {
    if (!Utils.isEmpty(resultsExtract)) {
      extractObj = true;
      Integer colonIndex = resultsExtract.indexOf(":");
      if (colonIndex != -1) {
        // Extract the index and key using substring
        try {
          rowNumber = Integer.parseInt(resultsExtract.substring(0, colonIndex));
          objKey = resultsExtract.substring(colonIndex + 1);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(
              "Failed to parse row number from " + resultsExtract, e);
        }
      } else {
        throw new IllegalArgumentException(
            "The row number and object key should be separated by colon, got " + resultsExtract);
      }
    }
  }

  /** Metadata for a Spark dataframe column, and how to access it from RESTPP JSON response. */
  public static class FieldMeta implements Serializable {
    private String name;
    private JsonPointer jsPtr;
    private Boolean isQueryable;

    /** Retrieve the value by JSON key */
    protected static FieldMeta fromValue(String name, String jsPath, Boolean isQueryable) {
      FieldMeta meta = new FieldMeta();
      meta.name = name;
      meta.jsPtr = JsonPointer.compile(jsPath);
      meta.isQueryable = isQueryable;
      return meta;
    }

    /** Convert meta to lambda to retrieve value from JSON Object */
    public Function<JsonNode, JsonNode> toAccessor() {
      return (node) -> {
        if (node.isValueNode()) {
          return node;
        } else {
          JsonNode tgt = node.at(jsPtr);
          if (tgt.isMissingNode()) {
            if (isQueryable) {
              return node.findValue(name);
            } else {
              return null;
            }
          } else {
            return tgt;
          }
        }
      };
    }

    private FieldMeta() {}
  }
}
