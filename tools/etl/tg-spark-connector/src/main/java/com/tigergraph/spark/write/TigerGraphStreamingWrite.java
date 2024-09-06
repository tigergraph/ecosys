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
import com.tigergraph.spark.client.common.RestppResponse;
import com.tigergraph.spark.log.LoggerFactory;
import com.tigergraph.spark.util.Utils;
import org.apache.spark.sql.connector.write.DataWriter;
import org.apache.spark.sql.connector.write.PhysicalWriteInfo;
import org.apache.spark.sql.connector.write.WriterCommitMessage;
import org.apache.spark.sql.connector.write.streaming.StreamingWrite;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;

/**
 * Defines how to write the data to TG in streaming queries.
 *
 * <p>The writing procedure is:
 *
 * <ol>
 *   <li>Create a writer factory by {@link #createStreamingWriterFactory(PhysicalWriteInfo)},
 *       serialize and send it to all the partitions of the input data(RDD).
 *   <li>For each epoch in each partition, create the data writer, and write the data of the epoch
 *       in the partition with this writer. If all the data are written successfully, call {@link
 *       DataWriter#commit()}. If exception happens during the writing, call {@link
 *       DataWriter#abort()}.
 *   <li>If writers in all partitions of one epoch are successfully committed, call {@link
 *       #commit(long, WriterCommitMessage[])}. If some writers are aborted, or the job failed with
 *       an unknown reason, call {@link #abort(long, WriterCommitMessage[])}.
 * </ol>
 */
public class TigerGraphStreamingWrite extends TigerGraphWriteBase implements StreamingWrite {
  private static final Logger logger = LoggerFactory.getLogger(TigerGraphStreamingWrite.class);

  TigerGraphStreamingWrite(StructType schema, TigerGraphConnection conn) {
    super(schema, conn);
  }

  @Override
  public TigerGraphStreamWriterFactory createStreamingWriterFactory(PhysicalWriteInfo info) {
    return new TigerGraphStreamWriterFactory(schema, conn);
  }

  @Override
  public void commit(long epochId, WriterCommitMessage[] messages) {
    logger.info(
        "Finished writing streaming updates({}) to TigerGraph {}",
        epochId,
        conn.getLoadingJobId() == null ? "" : ", Job ID: " + conn.getLoadingJobId());
    logger.info("Total processed rows by this update: {}", getTotalProcessedRows(messages));
    logger.info("Processed rows of each task by this update:\n{}", getTaskSummury(messages));
    RestppResponse resp = getLoadingStatistics();
    if (resp != null) {
      Utils.removeUserData(resp.results);
      logger.info("The up-to-date overall loading statistics: {}", resp.results.toPrettyString());
    }
  }

  @Override
  public void abort(long epochId, WriterCommitMessage[] messages) {
    logger.error(
        "Aborted when writing streaming updates({}) to TigerGraph {}",
        epochId,
        conn.getLoadingJobId() == null ? "" : ", Job ID: " + conn.getLoadingJobId());
    logger.info("Total processed rows by this update: {}", getTotalProcessedRows(messages));
    logger.info("Processed rows of each task by this update:\n{}", getTaskSummury(messages));
    RestppResponse resp = getLoadingStatistics();
    if (resp != null) {
      Utils.removeUserData(resp.results);
      logger.info("The overall loading statistics: {}", resp.results.toPrettyString());
    }
  }
}
