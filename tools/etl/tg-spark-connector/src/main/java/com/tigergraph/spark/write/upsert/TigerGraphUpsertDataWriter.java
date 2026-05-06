/**
 * Copyright (c) 2025 TigerGraph Inc.
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
package com.tigergraph.spark.write.upsert;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.connector.write.DataWriter;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigergraph.spark.TigerGraphConnection;
import com.tigergraph.spark.client.Misc;
import com.tigergraph.spark.client.Upsert;
import com.tigergraph.spark.client.Upsert.UpsertResponse;
import com.tigergraph.spark.client.common.RestppResponse;
import com.tigergraph.spark.log.LoggerFactory;
import com.tigergraph.spark.util.Options;
import com.tigergraph.spark.util.Utils;
import com.tigergraph.spark.write.TigerGraphWriterCommitMessage;
import com.tigergraph.spark.write.upsert.UpsertJsonBuilder.EdgeJsonBuilder;
import com.tigergraph.spark.write.upsert.UpsertJsonBuilder.VertexJsonBuilder;

/**
 * The data writer of an executor responsible for writing data for an input RDD partition using
 * direct upsert.
 */
public class TigerGraphUpsertDataWriter implements DataWriter<InternalRow> {
  private static final Logger logger = LoggerFactory.getLogger(TigerGraphUpsertDataWriter.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  private final StructType schema;
  private final int partitionId;
  private final long taskId;
  private final long epochId;

  private final Upsert upsert;
  private final String graph;
  private final int batchSizeRows;
  private final Options options;
  private final Map<String, Object> queryMap;
  private final UpsertJsonBuilder jsonBuilder;
  private final TigerGraphConnection conn;

  // Upsert operation type enum
  private enum UpsertType {
    VERTEX,
    EDGE
  }

  private final UpsertType upsertType;
  private int batchSize = 0;
  private long totalRecords = 0;

  /** Constructor for TigerGraphUpsertDataWriter */
  public TigerGraphUpsertDataWriter(
      StructType schema, TigerGraphConnection conn, int partitionId, long taskId, long epochId) {
    this.schema = schema;
    this.partitionId = partitionId;
    this.taskId = taskId;
    this.epochId = epochId;
    this.upsert = conn.getUpsert();
    this.conn = conn;

    this.options = conn.getOpts();
    this.graph = options.getString(Options.GRAPH);
    this.batchSizeRows = options.getInt(Options.UPSERT_BATCH_SIZE_ROWS);

    // Determine upsert type
    String vertexType = options.getString(Options.UPSERT_VERTEX_TYPE);
    String edgeType = options.getString(Options.UPSERT_EDGE_TYPE);
    if (!Utils.isEmpty(vertexType)) {
      this.upsertType = UpsertType.VERTEX;
    } else if (!Utils.isEmpty(edgeType)) {
      this.upsertType = UpsertType.EDGE;
    } else {
      throw new IllegalArgumentException(
          "Either vertex type or edge type must be specified for upsert operations");
    }

    String attributeMapping = options.getString(Options.UPSERT_ATTRIBUTE_MAPPING);
    String attributeOp = options.getString(Options.UPSERT_ATTRIBUTE_OP);

    // Initialize the appropriate JSON builder based on upsert type
    if (upsertType == UpsertType.VERTEX) {
      String vertexIdField = options.getString(Options.UPSERT_VERTEX_ID_FIELD);
      List<String> vertexIdFields = Utils.parseCommaDelimitedKeys(vertexIdField);
      this.jsonBuilder =
          new VertexJsonBuilder(
              vertexType, vertexIdFields, attributeMapping, attributeOp, this.schema);
    } else {
      String sourceType = options.getString(Options.UPSERT_EDGE_SOURCE_TYPE);
      String sourceIdField = options.getString(Options.UPSERT_EDGE_SOURCE_ID_FIELD);
      List<String> sourceIdFields = Utils.parseCommaDelimitedKeys(sourceIdField);
      String targetType = options.getString(Options.UPSERT_EDGE_TARGET_TYPE);
      String targetIdField = options.getString(Options.UPSERT_EDGE_TARGET_ID_FIELD);
      List<String> targetIdFields = Utils.parseCommaDelimitedKeys(targetIdField);

      // Get discriminators for edge type
      Set<String> discriminators = getDiscriminators(edgeType);

      this.jsonBuilder =
          new EdgeJsonBuilder(
              edgeType,
              sourceType,
              sourceIdFields,
              targetType,
              targetIdFields,
              attributeMapping,
              attributeOp,
              this.schema,
              discriminators);
    }

    // Setup query parameters
    this.queryMap = new HashMap<>();
    String ack = options.getString(Options.UPSERT_ACK);
    if (!Utils.isEmpty(ack)) {
      queryMap.put("ack", ack);
    }

    // Add edge-specific parameters if this is an edge upsert
    if (upsertType == UpsertType.EDGE) {
      queryMap.put("vertex_must_exist", options.getBoolean(Options.UPSERT_EDGE_VERTEX_MUST_EXIST));
      queryMap.put(
          "source_vertex_must_exist",
          options.getBoolean(Options.UPSERT_EDGE_SOURCE_VERTEX_MUST_EXIST));
      queryMap.put(
          "target_vertex_must_exist",
          options.getBoolean(Options.UPSERT_EDGE_TARGET_VERTEX_MUST_EXIST));
    }

    // Add vertex-specific parameters if this is a vertex upsert
    if (upsertType == UpsertType.VERTEX) {
      queryMap.put("new_vertex_only", options.getBoolean(Options.UPSERT_VERTEX_NEW_ONLY));
      queryMap.put("update_vertex_only", options.getBoolean(Options.UPSERT_VERTEX_UPDATE_ONLY));
    }

    logger.info(
        "Created upsert data writer for partition {}, task {}, epochId {}",
        partitionId,
        taskId,
        epochId);
  }

  public TigerGraphUpsertDataWriter(
      StructType schema, TigerGraphConnection conn, int partitionId, long taskId) {
    this(schema, conn, partitionId, taskId, (long) -1);
  }

  @Override
  public void write(InternalRow row) throws IOException {
    // Use the JSON builder to add the row to the batch
    jsonBuilder.add(row);

    batchSize++;

    // Check if we need to flush the batch
    if (batchSize >= batchSizeRows) {
      postToUpsert();
    }
  }

  @Override
  public TigerGraphWriterCommitMessage commit() throws IOException {
    if (batchSize > 0) {
      postToUpsert();
    }
    logger.info(
        "Finished upserting {} records to TigerGraph. Partition {}, task {}, epoch {}.",
        totalRecords,
        partitionId,
        taskId,
        epochId);
    return new TigerGraphWriterCommitMessage(totalRecords, partitionId, taskId);
  }

  @Override
  public void abort() throws IOException {
    logger.info(
        "Aborted upserting to TigerGraph. Partition {}, task {}, epoch {}.",
        partitionId,
        taskId,
        epochId);
  }

  @Override
  public void close() throws IOException {
    // no-op
  }

  /** Process the batch by sending it to TigerGraph */
  private void postToUpsert() {
    if (batchSize == 0) {
      return;
    }

    try {
      // Get the JSON payload
      String jsonPayload = jsonBuilder.build().toString();
      UpsertResponse resp = upsert.upsert(graph, jsonPayload, queryMap);
      resp.panicOnFail();
      String results = resp.getResultsAsString();

      logger.info("Upsert results: {}", results);
      totalRecords += batchSize;

      // Reset batch
      jsonBuilder.clear();
      batchSize = 0;
    } catch (Exception e) {
      logger.error("Failed to upsert batch: {}", e.getMessage());
      throw new RuntimeException("Failed to upsert batch", e);
    }
  }

  /**
   * Get discriminator attribute names for the specified edge type. Calls the GraphSchema API to
   * retrieve edge schema information and extracts discriminator attributes.
   *
   * @param edgeType The edge type to get discriminators for
   * @return Set of discriminator attribute names, empty if no discriminators or on error
   */
  private Set<String> getDiscriminators(String edgeType) {
    Set<String> discriminators = new HashSet<>();

    try {
      Misc misc = conn.getMisc();
      RestppResponse response = misc.graphSchema(conn.getVersion(), graph, edgeType);

      if (response.error) {
        logger.warn("Failed to get schema for edge type {}: {}", edgeType, response.message);
        return discriminators;
      }

      // Parse the response JSON
      JsonNode resultsNode = response.results;
      if (resultsNode != null && resultsNode.has("CompositeDiscriminator")) {
        JsonNode discriminatorArray = resultsNode.get("CompositeDiscriminator");
        if (discriminatorArray.isArray()) {
          for (JsonNode discriminatorNode : discriminatorArray) {
            if (discriminatorNode.isTextual()) {
              discriminators.add(discriminatorNode.asText());
            }
          }
        }
      }

      logger.info(
          "Found {} discriminators for edge type {}: {}",
          discriminators.size(),
          edgeType,
          discriminators);

    } catch (Exception e) {
      logger.warn(
          "Exception while getting discriminators for edge type {}: {}", edgeType, e.getMessage());
    }

    return discriminators;
  }
}
