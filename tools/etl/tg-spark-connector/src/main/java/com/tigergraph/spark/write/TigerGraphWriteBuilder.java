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

import com.tigergraph.spark.TigerGraphConnection;
import com.tigergraph.spark.log.LoggerFactory;
import org.apache.spark.sql.connector.write.LogicalWriteInfo;
import org.apache.spark.sql.connector.write.WriteBuilder;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;

/** Builder for Batch Write or Streaming Write */
public class TigerGraphWriteBuilder implements WriteBuilder {
  private static final Logger logger = LoggerFactory.getLogger(TigerGraphWriteBuilder.class);
  private final StructType schema;
  private final TigerGraphConnection conn;

  public TigerGraphWriteBuilder(LogicalWriteInfo info, TigerGraphConnection conn) {
    logger.info("Start to build TigerGraph data writer with queryId {}", info.queryId());
    this.schema = info.schema();
    this.conn = conn;
    if (conn.getLoadingJobId() != null) {
      logger.info("Loading job ID: {}", conn.getLoadingJobId());
    }
  }

  @Override
  public TigerGraphBatchWrite buildForBatch() {
    return new TigerGraphBatchWrite(schema, conn);
  }

  @Override
  public TigerGraphStreamingWrite buildForStreaming() {
    return new TigerGraphStreamingWrite(schema, conn);
  }
}
