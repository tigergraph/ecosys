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
import org.apache.spark.sql.connector.read.InputPartition;
import org.apache.spark.sql.connector.read.PartitionReaderFactory;
import org.slf4j.Logger;

/**
 * A factory used to create {@link TigerGraphPartitionReader} instances.
 *
 * <p>If Spark fails to execute any methods in the implementations of this interface or in the
 * returned {@link TigerGraphPartitionReader} (by throwing an exception), corresponding Spark task
 * would fail and get retried until hitting the maximum retry times.
 */
public class TigerGraphPartitionReaderFactory implements PartitionReaderFactory {
  private static final Logger logger =
      LoggerFactory.getLogger(TigerGraphPartitionReaderFactory.class);

  private final TigerGraphConnection conn;
  private final TigerGraphResultAccessor accessor;

  public TigerGraphPartitionReaderFactory(
      TigerGraphConnection connection, TigerGraphResultAccessor accessor) {
    this.conn = connection;
    this.accessor = accessor;
  }

  @Override
  public TigerGraphPartitionReader createReader(InputPartition partition) {
    // re-init logger for spark executors
    Options opts = conn.getOpts();
    if (opts.containsOption(Options.LOG_LEVEL)) {
      LoggerFactory.initJULLogger(opts.getInt(Options.LOG_LEVEL), opts.getString(Options.LOG_FILE));
    }
    logger.info("Creating partition reader for partition: {}", partition.toString());
    return new TigerGraphPartitionReader(conn, accessor, (TigerGraphRangeInputPartition) partition);
  }
}
