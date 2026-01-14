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
import com.tigergraph.spark.util.Options;
import com.tigergraph.spark.write.loading.TigerGraphLoadingDataWriter;
import com.tigergraph.spark.write.upsert.TigerGraphUpsertDataWriter;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.connector.write.DataWriter;
import org.apache.spark.sql.connector.write.streaming.StreamingDataWriterFactory;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;

/**
 * A factory of {@link TigerGraphLoadingDataWriter} or {@link TigerGraphUpsertDataWriter} for
 * streaming write, which is responsible for creating and initializing the actual data writer at
 * executor side.
 */
public class TigerGraphStreamWriterFactory implements StreamingDataWriterFactory {
  private static final Logger logger = LoggerFactory.getLogger(TigerGraphStreamWriterFactory.class);

  private final StructType schema;
  private final TigerGraphConnection conn;

  public TigerGraphStreamWriterFactory(StructType schema, TigerGraphConnection conn) {
    this.schema = schema;
    this.conn = conn;
  }

  @Override
  public DataWriter<InternalRow> createWriter(int partitionId, long taskId, long epochId) {
    // re-init logger for spark executors
    Options opts = conn.getOpts();
    if (opts.containsOption(Options.LOG_LEVEL)) {
      LoggerFactory.initJULLogger(opts.getInt(Options.LOG_LEVEL), opts.getString(Options.LOG_FILE));
    }

    if (Options.OptionType.LOADING.equals(conn.getOpts().getOptionType())) {
      logger.info(
          "Creating TigerGraph loading streaming writer for partitionId {}, taskId {}, epochId {}.",
          partitionId,
          taskId,
          epochId);
      return new TigerGraphLoadingDataWriter(schema, conn, partitionId, taskId, epochId);
    } else if (Options.OptionType.UPSERT.equals(conn.getOpts().getOptionType())) {
      logger.info(
          "Creating TigerGraph upsert streaming writer for partitionId {}, taskId {}, epochId {}.",
          partitionId,
          taskId,
          epochId);
      return new TigerGraphUpsertDataWriter(schema, conn, partitionId, taskId, epochId);
    } else {
      throw new UnsupportedOperationException(
          "Unsupported option type: " + conn.getOpts().getOptionType());
    }
  }
}
