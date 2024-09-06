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
import com.tigergraph.spark.log.LoggerFactory;
import com.tigergraph.spark.util.Options;
import com.tigergraph.spark.util.Utils;
import java.math.BigInteger;
import java.util.List;
import org.apache.spark.sql.connector.read.Batch;
import org.apache.spark.sql.connector.read.InputPartition;
import org.slf4j.Logger;

/**
 * A physical representation of a data source scan for batch queries. This interface is used to
 * provide physical information, like how many partitions the scanned data has, and how to read
 * records from the partitions.
 */
public class TigerGraphBatch implements Batch {
  private static final Logger logger = LoggerFactory.getLogger(TigerGraphBatch.class);

  private final TigerGraphConnection conn;
  private final TigerGraphResultAccessor accessor;

  public TigerGraphBatch(TigerGraphConnection connection, TigerGraphResultAccessor accessor) {
    logger.info(
        "Initializing TigerGraph batch query, query type: {}", connection.getOpts().getQueryType());
    this.conn = connection;
    this.accessor = accessor;
  }

  @Override
  public InputPartition[] planInputPartitions() {
    Options opts = conn.getOpts();
    return planInputPartitions(
        opts.getInt(Options.QUERY_PARTITION_NUM),
        opts.getString(Options.QUERY_PARTITION_KEY),
        opts.getString(Options.QUERY_PARTITION_LOWER_BOUND),
        opts.getString(Options.QUERY_PARTITION_UPPER_BOUND));
  }

  /* Visible for testing */
  protected static InputPartition[] planInputPartitions(
      Integer partitionNum, String partitionKey, String lower, String upper) {
    // If given partition key, auto generate the range for each partition
    if (!Utils.isEmpty(partitionKey)) {
      BigInteger lo = new BigInteger(lower);
      BigInteger up = new BigInteger(upper);
      List<BigInteger> splitPoints =
          TigerGraphRangeInputPartition.calculatePartitions(partitionNum, lo, up);
      logger.info(
          "Partition number: {}, split on partitioning key '{}': {}",
          splitPoints.size() + 1,
          partitionKey,
          splitPoints);
      // Use split points to build the partition ranges
      // add null to the head and tail to represent -∞ and +∞
      InputPartition[] partitions = new InputPartition[splitPoints.size() + 1];
      splitPoints.add(0, null);
      splitPoints.add(null);
      for (int i = 0; i < splitPoints.size() - 1; i++) {
        partitions[i] =
            new TigerGraphRangeInputPartition(
                i, partitionKey, splitPoints.get(i), splitPoints.get(i + 1));
      }
      return partitions;
    } else {
      // Non-partitioned query
      return new InputPartition[] {new TigerGraphRangeInputPartition()};
    }
  }

  @Override
  public TigerGraphPartitionReaderFactory createReaderFactory() {
    return new TigerGraphPartitionReaderFactory(conn, accessor);
  }
}
