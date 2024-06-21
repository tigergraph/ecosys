/**
 * Copyright (c) 2023 TigerGraph Inc.
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
package com.tigergraph.spark;

import com.fasterxml.jackson.databind.JsonNode;
import com.tigergraph.spark.client.Misc;
import com.tigergraph.spark.client.Misc.QueryMetaResponse;
import com.tigergraph.spark.client.common.RestppErrorException;
import com.tigergraph.spark.client.common.RestppResponse;
import com.tigergraph.spark.util.Options;
import com.tigergraph.spark.util.Utils;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.spark.sql.connector.catalog.TableProvider;
import org.apache.spark.sql.connector.expressions.Transform;
import org.apache.spark.sql.sources.DataSourceRegister;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A pure implementation of Spark Data Source V2 that apply data operations to existing TG objects,
 * e.g., loading job, pre-installed query, vertex or edge. DDL is unsupported.
 */
public class TigerGraphTableProvider implements TableProvider, DataSourceRegister {

  private static final Logger logger = LoggerFactory.getLogger(TigerGraphConnection.class);
  private static final String SHORT_NAME = "tigergraph";
  private final long creationTime = Instant.now().toEpochMilli();

  private TigerGraphConnection conn;

  /**
   * For Write operation, the schema will be the schema of input dataframe; For Read operation, it
   * will be the user given schema.
   */
  @Override
  public boolean supportsExternalMetadata() {
    return true;
  }

  @Override
  public StructType inferSchema(CaseInsensitiveStringMap options) {
    tryConnect(options.asCaseSensitiveMap());
    return inferSchema(conn);
  }

  @Override
  public TigerGraphTable getTable(
      StructType schema, Transform[] partitioning, Map<String, String> properties) {
    tryConnect(properties);
    return new TigerGraphTable(schema, conn);
  }

  @Override
  public String shortName() {
    return SHORT_NAME;
  }

  private void tryConnect(Map<String, String> properties) {
    if (this.conn == null) {
      Options opts = new Options(properties, false);
      this.conn = new TigerGraphConnection(opts, creationTime);
    }
  }

  protected static StructType inferSchema(TigerGraphConnection conn) {
    Options opts = conn.getOpts();
    Misc misc = conn.getMisc();
    // Only keep the expected attributes if the `select` operator is given
    Set<String> columnPrune = null;
    if (opts.containsOption(Options.QUERY_OP_SELECT)) {
      columnPrune =
          Arrays.stream(opts.getString(Options.QUERY_OP_SELECT).split(","))
              .collect(Collectors.toSet());
    }
    switch (opts.getQueryType()) {
      case GET_VERTICES:
      case GET_VERTEX:
        String vType =
            Utils.extractQueryFields(
                    opts.getString(Options.QUERY_VERTEX),
                    opts.getString(Options.QUERY_FIELD_SEPARATOR))
                .get(0);
        RestppResponse vSchema = misc.graphSchema(conn.getGraph(), vType);
        vSchema.panicOnFail();
        return parseVertexSchema(vSchema.results, columnPrune);
      case GET_EDGES_BY_SRC_VERTEX:
        // There can be different edge types with different attributes, so we don't flatten the
        // attributes
        return StructType.fromDDL(
            "from_type STRING, from_id STRING, to_type STRING, to_id STRING, e_type STRING,"
                + " attributes STRING");
      case GET_EDGES_BY_SRC_VERTEX_EDGE_TYPE:
      case GET_EDGES_BY_SRC_VERTEX_EDGE_TYPE_TGT_TYPE:
      case GET_EDGE_BY_SRC_VERTEX_EDGE_TYPE_TGT_VERTEX:
        String eType =
            Utils.extractQueryFields(
                    opts.getString(Options.QUERY_EDGE),
                    opts.getString(Options.QUERY_FIELD_SEPARATOR))
                .get(2);
        RestppResponse eSchema = misc.graphSchema(conn.getGraph(), eType);
        eSchema.panicOnFail();
        return parseEdgeSchema(eSchema.results, columnPrune);
      case INSTALLED:
        QueryMetaResponse queryMeta =
            misc.queryMeta(conn.getGraph(), opts.getString(Options.QUERY_INSTALLED));
        try {
          queryMeta.panicOnFail();
        } catch (RestppErrorException e) {
          logger.warn(
              "Failed to parse metadata of query '"
                  + opts.getString(Options.QUERY_INSTALLED)
                  + "'. Failback to default schema 'results STRING'. "
                  + e.getMessage());
          return StructType.fromDDL("results STRING");
        }
        return parseQuerySchema(queryMeta.output);
      case INTERPRETED:
        // It's unsupported to get meta of interpreted query
        return StructType.fromDDL("results STRING");
      default:
        break;
    }
    return null;
  }

  /**
   * Input meta format: { "Config": {...}, "Attributes": [...], "PrimaryId": {...}, "Name": "person"
   * }
   *
   * @param meta
   * @return v_id | attribute 1 | attribute 2 | ... | attribute n
   */
  protected static StructType parseVertexSchema(JsonNode meta, Set<String> columnPrune) {
    StructType schema = new StructType();
    // Parse primary id type
    String vIdType = meta.path("PrimaryId").path("AttributeType").path("Name").asText("STRING");
    schema = schema.add("v_id", mapTGTypeToSparkType(vIdType));
    // Parse attributes type
    Iterator<JsonNode> attrIter = meta.path("Attributes").elements();
    while (attrIter.hasNext()) {
      JsonNode attr = attrIter.next();
      String attrName = attr.path("AttributeName").asText();
      String attrType = attr.path("AttributeType").path("Name").asText("STRING");
      if (!Utils.isEmpty(attrName) && (columnPrune == null || columnPrune.contains(attrName)))
        schema = schema.add(attrName, mapTGTypeToSparkType(attrType));
    }
    return schema;
  }

  /**
   * Input meta format: { "IsDirected": false, "ToVertexTypeName": "company", "Config": {},
   * "Attributes": [...], "FromVertexTypeName": "person", "Name": "worksFor" }
   *
   * @param meta
   * @return from_type | from_id | to_type | to_id | attribute 1 | attribute 2 | ... | attribute n
   */
  protected static StructType parseEdgeSchema(JsonNode meta, Set<String> columnPrune) {
    StructType fixedSchema =
        StructType.fromDDL("from_type STRING, from_id STRING, to_type STRING, to_id STRING");
    StructType attrSchema = new StructType();
    // Parse attributes type
    Iterator<JsonNode> attrIter = meta.path("Attributes").elements();
    while (attrIter.hasNext()) {
      JsonNode attr = attrIter.next();
      String attrName = attr.path("AttributeName").asText();
      String attrType = attr.path("AttributeType").path("Name").asText("STRING");
      if (!Utils.isEmpty(attrName) && (columnPrune == null || columnPrune.contains(attrName)))
        attrSchema = attrSchema.add(attrName, mapTGTypeToSparkType(attrType));
    }
    return fixedSchema.merge(attrSchema);
  }

  /**
   * Parse the query result schema if and only if the schema is unique. Input meta format:
   * {"output": [{"a":"string", "b":"int"}, {"c":"string", "d":"int"}]} <br>
   * => | results | <br>
   * Input meta format: {"output": [{"a":"string", "b":"int"}, {"b":"int", "a":"string"}]} <br>
   * => | a | b |
   *
   * @param meta
   * @return
   */
  protected static StructType parseQuerySchema(List<JsonNode> meta) {
    // The Jackson can compare JsonNode via `equal(obj)` regardless of the order of the fields
    List<JsonNode> dedupMeta = meta.stream().distinct().collect(Collectors.toList());
    if (dedupMeta.size() != 1) {
      // different lines have different schemas, so we don't flatten it
      return StructType.fromDDL("results STRING");
    } else {
      StructType schema = new StructType();
      JsonNode uniqueMeta = dedupMeta.get(0);
      Iterator<Entry<String, JsonNode>> iter = uniqueMeta.fields();
      while (iter.hasNext()) {
        Entry<String, JsonNode> field = iter.next();
        schema =
            schema.add(field.getKey(), mapTGTypeToSparkType(field.getValue().asText("STRING")));
      }
      return schema;
    }
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
    tgType = tgType.toLowerCase();
    switch (tgType) {
      case "int":
      case "uint":
        return DataTypes.createDecimalType(38, 0);
      case "float":
        return DataTypes.FloatType;
      case "double":
        return DataTypes.DoubleType;
      case "bool":
      case "boolean":
        return DataTypes.BooleanType;
      case "fixed_binary":
        return DataTypes.BinaryType;
      default:
        return DataTypes.StringType;
    }
  }
}
