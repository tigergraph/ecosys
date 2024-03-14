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

import org.apache.spark.sql.connector.read.Batch;
import org.apache.spark.sql.connector.read.InputPartition;

/**
 * A physical representation of a data source scan for batch queries. This interface is used to
 * provide physical information, like how many partitions the scanned data has, and how to read
 * records from the partitions.
 */
public class TigerGraphBatch implements Batch {
  @Override
  public InputPartition[] planInputPartitions() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'planInputPartitions'");
  }

  @Override
  public TigerGraphPartitionReaderFactory createReaderFactory() {
    return new TigerGraphPartitionReaderFactory();
  }
}
