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

import com.tigergraph.spark.TigerGraphConnection;
import com.tigergraph.spark.client.Query;
import com.tigergraph.spark.client.common.RestppStreamResponse;
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
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
      TigerGraphConnection conn, StructType schema, TigerGraphRangeInputPartition partition) {
    Query query = conn.getQuery();
    Options opts = conn.getOpts();
    converter = new TigerGraphJsonConverter(schema, opts.getQueryType());
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
    switch (opts.getQueryType()) {
      case GET_VERTICES:
        resp = query.getVertices(conn.getGraph(), opts.getString(Options.QUERY_VERTEX), queryOps);
        break;
      case GET_VERTEX:
        List<String> fields = Utils.extractQueryFields(opts.getString(Options.QUERY_VERTEX));
        resp = query.getVertex(conn.getGraph(), fields.get(0), fields.get(1), queryOps);
        break;
        // TODO: support edge/install/interpreted query
      default:
        throw new IllegalArgumentException("Invalid query type: " + opts.getQueryType());
    }
    long duration = System.currentTimeMillis() - startTime;
    logger.info("Query finished in {}ms, start to process the results.", duration);
    resp.panicOnFail();
  }

  @Override
  public void close() throws IOException {
    logger.info("Closing partition reader {}", partition.toString());
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
