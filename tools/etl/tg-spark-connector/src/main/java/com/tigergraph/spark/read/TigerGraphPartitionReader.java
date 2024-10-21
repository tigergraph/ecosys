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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigergraph.spark.TigerGraphConnection;
import com.tigergraph.spark.client.Query;
import com.tigergraph.spark.client.common.RestppErrorException;
import com.tigergraph.spark.client.common.RestppStreamResponse;
import com.tigergraph.spark.log.LoggerFactory;
import com.tigergraph.spark.util.Options;
import com.tigergraph.spark.util.Utils;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.expressions.GenericInternalRow;
import org.apache.spark.sql.connector.read.InputPartition;
import org.apache.spark.sql.connector.read.PartitionReader;
import org.apache.spark.sql.connector.read.PartitionReaderFactory;
import org.slf4j.Logger;

/**
 * A partition reader returned by {@link PartitionReaderFactory#createReader(InputPartition)}. It's
 * responsible for outputting data for a RDD partition.
 *
 * <p>Note that, Currently the type `T` can only be {@link
 * org.apache.spark.sql.catalyst.InternalRow} for TG
 */
public class TigerGraphPartitionReader implements PartitionReader<InternalRow> {
  private static final Logger logger = LoggerFactory.getLogger(TigerGraphPartitionReader.class);

  private final RestppStreamResponse resp;
  private final TigerGraphJsonConverter converter;
  private final TigerGraphRangeInputPartition partition;
  private InternalRow currentRow = new GenericInternalRow();

  public TigerGraphPartitionReader(
      TigerGraphConnection conn,
      TigerGraphResultAccessor accessor,
      TigerGraphRangeInputPartition partition) {
    Query query = conn.getQuery();
    Options opts = conn.getOpts();
    converter = new TigerGraphJsonConverter(accessor, opts.getQueryType());
    this.partition = partition;
    logger.info("Initializing partition reader of query type {}", opts.getQueryType());

    Map<String, Object> queryOps = new HashMap<>();
    if (opts.containsOption(Options.QUERY_OP_SELECT))
      queryOps.put("select", opts.getString(Options.QUERY_OP_SELECT));
    if (opts.containsOption(Options.QUERY_OP_LIMIT))
      queryOps.put("limit", opts.getLong(Options.QUERY_OP_LIMIT));
    if (opts.containsOption(Options.QUERY_OP_SORT))
      queryOps.put("sort", opts.getString(Options.QUERY_OP_SORT));
    // add filters based on user input and partition range
    String filters =
        Stream.of(opts.getString(Options.QUERY_OP_FILTER), partition.getPartitionRange())
            .filter(s -> !Utils.isEmpty(s))
            .collect(Collectors.joining(","));
    if (!Utils.isEmpty(filters)) queryOps.put("filter", filters);

    long startTime = System.currentTimeMillis();
    logger.info("Start executing the query.");
    List<String> fields;
    switch (opts.getQueryType()) {
      // Vertex query
      case GET_VERTICES:
        resp = query.getVertices(conn.getGraph(), opts.getString(Options.QUERY_VERTEX), queryOps);
        break;
      case GET_VERTEX:
        fields =
            Utils.extractQueryFields(
                opts.getString(Options.QUERY_VERTEX),
                opts.getString(Options.QUERY_FIELD_SEPARATOR));
        resp = query.getVertex(conn.getGraph(), fields.get(0), fields.get(1), queryOps);
        break;
      // Edge query
      case GET_EDGES_BY_SRC_VERTEX:
        fields =
            Utils.extractQueryFields(
                opts.getString(Options.QUERY_EDGE), opts.getString(Options.QUERY_FIELD_SEPARATOR));
        resp = query.getEdgesBySrcVertex(conn.getGraph(), fields.get(0), fields.get(1), queryOps);
        break;
      case GET_EDGES_BY_SRC_VERTEX_EDGE_TYPE:
        fields =
            Utils.extractQueryFields(
                opts.getString(Options.QUERY_EDGE), opts.getString(Options.QUERY_FIELD_SEPARATOR));
        resp =
            query.getEdgesBySrcVertexEdgeType(
                conn.getGraph(), fields.get(0), fields.get(1), fields.get(2), queryOps);
        break;
      case GET_EDGES_BY_SRC_VERTEX_EDGE_TYPE_TGT_TYPE:
        fields =
            Utils.extractQueryFields(
                opts.getString(Options.QUERY_EDGE), opts.getString(Options.QUERY_FIELD_SEPARATOR));
        resp =
            query.getEdgesBySrcVertexEdgeTypeTgtType(
                conn.getGraph(),
                fields.get(0),
                fields.get(1),
                fields.get(2),
                fields.get(3),
                queryOps);
        break;
      case GET_EDGE_BY_SRC_VERTEX_EDGE_TYPE_TGT_VERTEX:
        fields =
            Utils.extractQueryFields(
                opts.getString(Options.QUERY_EDGE), opts.getString(Options.QUERY_FIELD_SEPARATOR));
        resp =
            query.getEdgeBySrcVertexEdgeTypeTgtVertex(
                conn.getGraph(),
                fields.get(0),
                fields.get(1),
                fields.get(2),
                fields.get(3),
                fields.get(4),
                queryOps);
        break;
      case INSTALLED:
        // precheck
        if (!Utils.isEmpty(opts.getString(Options.QUERY_PARAMS))) {
          try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(opts.getString(Options.QUERY_PARAMS));
          } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                "Failed to parse 'query.params', please check if it is a valid JSON.", e);
          }
        }
        resp =
            query.installedQuery(
                conn.getGraph(),
                opts.getString(Options.QUERY_INSTALLED),
                opts.getString(Options.QUERY_PARAMS),
                null);
        break;
      case INTERPRETED:
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> queryParams = new HashMap<>();
        if (!Utils.isEmpty(opts.getString(Options.QUERY_PARAMS))) {
          try {
            queryParams =
                mapper.readValue(
                    opts.getString(Options.QUERY_PARAMS),
                    new TypeReference<Map<String, Object>>() {});
          } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                "Failed to parse query.params, please check if it is a valid JSON.", e);
          }
        }
        resp =
            query.interpretedQuery(
                conn.getVersion(), opts.getString(Options.QUERY_INTERPRETED), queryParams);
        break;
      default:
        throw new IllegalArgumentException("Invalid query type: " + opts.getQueryType());
    }
    long duration = System.currentTimeMillis() - startTime;
    logger.info("Query finished in {}ms, start to process the results.", duration);
    resp.panicOnFail();
    if (accessor.extractObj()) {
      try {
        resp.reinitCursor(accessor.getRowNumber(), accessor.getObjKey());
      } catch (IOException e) {
        throw new RestppErrorException("Failed to extract the first object", e);
      }
    }
  }

  @Override
  public void close() throws IOException {
    logger.info("Closing partition reader of {}", partition.toString());
    resp.close();
  }

  /** Proceed to next record, returns false if there is no more records. */
  @Override
  public boolean next() throws IOException {
    boolean hasNext = resp.next();
    if (hasNext) {
      currentRow = converter.convert(resp.readRow());
    } else {
      currentRow = new GenericInternalRow();
    }
    return hasNext;
  }

  /**
   * Return the current record. This method should return same value until `next` is called. To make
   * it more efficient when being called multiple times, the data conversion is performed in
   * `next()`, and it only directly return the value.
   */
  @Override
  public InternalRow get() {
    return currentRow;
  }
}
