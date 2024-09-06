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

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.spark.sql.connector.catalog.TableProvider;
import org.apache.spark.sql.connector.expressions.Transform;
import org.apache.spark.sql.sources.DataSourceRegister;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;
import org.slf4j.Logger;

import com.tigergraph.spark.client.Misc;
import com.tigergraph.spark.client.Misc.QueryMetaResponse;
import com.tigergraph.spark.client.common.RestppResponse;
import com.tigergraph.spark.log.LoggerFactory;
import com.tigergraph.spark.read.TigerGraphResultAccessor;
import com.tigergraph.spark.util.Options;
import com.tigergraph.spark.util.Utils;

/**
 * A pure implementation of Spark Data Source V2 that apply data operations to existing TG objects,
 * e.g., loading job, pre-installed query, vertex or edge. DDL is unsupported.
 */
public class TigerGraphTableProvider implements TableProvider, DataSourceRegister {

  private static final Logger logger = LoggerFactory.getLogger(TigerGraphConnection.class);
  private static final String SHORT_NAME = "tigergraph";
  private final long creationTime = Instant.now().toEpochMilli();

  private TigerGraphConnection conn;
  private TigerGraphResultAccessor accessor;

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
    accessor = inferSchema(conn);
    return accessor.getSchema();
  }

  @Override
  public TigerGraphTable getTable(
      StructType schema, Transform[] partitioning, Map<String, String> properties) {
    tryConnect(properties);
    if (accessor == null) {
      accessor =
          TigerGraphResultAccessor.fromExternalSchema(
              schema, conn.getOpts().getString(Options.QUERY_RESULTS_EXTRACT));
    }
    return new TigerGraphTable(accessor, conn);
  }

  @Override
  public String shortName() {
    return SHORT_NAME;
  }

  private void tryConnect(Map<String, String> properties) {
    if (this.conn == null) {
      Options opts = new Options(properties, false);
      // If explicitly set log level, then use the default JUL logger
      if (opts.containsOption(Options.LOG_LEVEL)) {
        LoggerFactory.initJULLogger(
            opts.getInt(Options.LOG_LEVEL), opts.getString(Options.LOG_FILE));
      }
      this.conn = new TigerGraphConnection(opts, creationTime);
    }
  }

  protected static TigerGraphResultAccessor inferSchema(TigerGraphConnection conn) {
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
        RestppResponse vSchema = misc.graphSchema(conn.getVersion(), conn.getGraph(), vType);
        vSchema.panicOnFail();
        return TigerGraphResultAccessor.fromVertexMeta(vSchema.results, columnPrune);
      case GET_EDGES_BY_SRC_VERTEX:
        // There can be different edge types with different attributes, so we don't flatten the
        // attributes
        StructType edgeSchema =
            StructType.fromDDL(
                "from_type STRING, from_id STRING, to_type STRING, to_id STRING, e_type STRING,"
                    + " attributes STRING");
        return TigerGraphResultAccessor.fromExternalSchema(edgeSchema, "");
      case GET_EDGES_BY_SRC_VERTEX_EDGE_TYPE:
      case GET_EDGES_BY_SRC_VERTEX_EDGE_TYPE_TGT_TYPE:
      case GET_EDGE_BY_SRC_VERTEX_EDGE_TYPE_TGT_VERTEX:
        String eType =
            Utils.extractQueryFields(
                    opts.getString(Options.QUERY_EDGE),
                    opts.getString(Options.QUERY_FIELD_SEPARATOR))
                .get(2);
        RestppResponse eSchema = misc.graphSchema(conn.getVersion(), conn.getGraph(), eType);
        eSchema.panicOnFail();
        return TigerGraphResultAccessor.fromEdgeMeta(eSchema.results, columnPrune);
      case INSTALLED:
        try {
          QueryMetaResponse queryMeta =
              misc.queryMeta(
                  conn.getVersion(), conn.getGraph(), opts.getString(Options.QUERY_INSTALLED));
          queryMeta.panicOnFail();
          return TigerGraphResultAccessor.fromQueryMeta(
              queryMeta.output, opts.getString(Options.QUERY_RESULTS_EXTRACT));
        } catch (Exception e) {
          logger.warn(
              "Failed to parse metadata of query '"
                  + opts.getString(Options.QUERY_INSTALLED)
                  + "'. Failback to default schema 'results STRING'. "
                  + e.getMessage());
          return TigerGraphResultAccessor.fromUnknownMeta(
              opts.getString(Options.QUERY_RESULTS_EXTRACT));
        }
      case INTERPRETED:
        // It's unsupported to get meta of interpreted query
        return TigerGraphResultAccessor.fromUnknownMeta(
            opts.getString(Options.QUERY_RESULTS_EXTRACT));
      default:
        break;
    }
    return null;
  }
}
