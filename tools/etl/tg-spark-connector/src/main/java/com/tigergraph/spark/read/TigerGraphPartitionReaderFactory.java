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

import org.apache.spark.sql.connector.read.InputPartition;
import org.apache.spark.sql.connector.read.PartitionReaderFactory;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tigergraph.spark.TigerGraphConnection;

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
  private final StructType schema;

  public TigerGraphPartitionReaderFactory(TigerGraphConnection connection, StructType schema) {
    this.conn = connection;
    this.schema = schema;
  }

  @Override
  public TigerGraphPartitionReader createReader(InputPartition partition) {
    logger.info("Creating partition reader for partition: {}", partition.toString());
    return new TigerGraphPartitionReader(conn, schema, (TigerGraphRangeInputPartition) partition);
  }
}
