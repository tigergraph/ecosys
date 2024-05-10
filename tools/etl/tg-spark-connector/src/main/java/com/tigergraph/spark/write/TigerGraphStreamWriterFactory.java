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
package com.tigergraph.spark.write;

import org.apache.spark.sql.connector.write.streaming.StreamingDataWriterFactory;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tigergraph.spark.TigerGraphConnection;

/**
 * A factory of {@link TigerGraphDataWriter} for streaming write, which is responsible for creating
 * and initializing the actual data writer at executor side.
 */
public class TigerGraphStreamWriterFactory implements StreamingDataWriterFactory {
  private static final Logger logger = LoggerFactory.getLogger(TigerGraphStreamWriterFactory.class);

  private final StructType schema;
  private final TigerGraphConnection conn;

  TigerGraphStreamWriterFactory(StructType schema, TigerGraphConnection conn) {
    this.schema = schema;
    this.conn = conn;
    logger.info("Created {} for executor", TigerGraphBatchWriterFactory.class);
  }

  @Override
  public TigerGraphDataWriter createWriter(int partitionId, long taskId, long epochId) {
    logger.info(
        "Create TigerGraph streaming writer for partitionId {}, taskId {}, epochId {}.",
        partitionId,
        taskId,
        epochId);
    return new TigerGraphDataWriter(schema, conn, partitionId, taskId, epochId);
  }
}