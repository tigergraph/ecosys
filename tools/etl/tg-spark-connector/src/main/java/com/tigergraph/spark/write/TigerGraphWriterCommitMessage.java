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

import org.apache.spark.sql.connector.write.WriterCommitMessage;

/**
 * A commit message returned by TigerGraphDataWriter.commit() and will be sent back to the driver
 * side as the input parameter of TigerGraphBatchWrite.commit(WriterCommitMessage []) or
 * TigerGraphStreamingWrite.commit(long, WriterCommitMessage []).
 */
public class TigerGraphWriterCommitMessage implements WriterCommitMessage {
  private final long loadedRows;
  private final int partitionId;
  private final long taskId;

  TigerGraphWriterCommitMessage(long loadedRows, int partitionId, long taskId) {
    this.loadedRows = loadedRows;
    this.partitionId = partitionId;
    this.taskId = taskId;
  }

  public String toString() {
    return String.format(
        "PartitionId: %,d, taskId: %,d, loaded rows: %,d", partitionId, taskId, loadedRows);
  }

  public long getLoadedRows() {
    return this.loadedRows;
  }

  public int getPartitionId() {
    return this.partitionId;
  }

  public long getTaskId() {
    return this.taskId;
  }
}
