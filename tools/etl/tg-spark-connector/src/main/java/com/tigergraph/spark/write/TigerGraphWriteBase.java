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
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.spark.sql.connector.write.WriterCommitMessage;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;

/** Base class for {@link TigerGraphBatchWrite} and {@link TigerGraphStreamingWrite}. */
public class TigerGraphWriteBase {
  private static final Logger logger = LoggerFactory.getLogger(TigerGraphWriteBase.class);

  protected final StructType schema;
  protected final TigerGraphConnection conn;

  public TigerGraphWriteBase(StructType schema, TigerGraphConnection conn) {
    this.schema = schema;
    this.conn = conn;
  }

  protected RestppResponse getLoadingStatistics() {
    if (Utils.versionCmp(conn.getVersion(), "3.9.4") >= 0) {
      // HACK: call a restpp endpoint to automatically refresh token if expires
      try {
        conn.getMisc().version();
      } catch (Exception e) {
        // no-op
      }

      try {
        RestppResponse resp =
            conn.getMisc()
                .loadingProgress(conn.getVersion(), conn.getGraph(), conn.getLoadingJobId());
        resp.panicOnFail();
        return resp;
      } catch (Exception e) {
        logger.info(
            "Failed to query loading statistics of job {}: {}, it won't block the loading",
            conn.getLoadingJobId(),
            e.getMessage());
      }
    }
    return null;
  }

  protected long getTotalProcessedRows(WriterCommitMessage[] messages) {
    return Arrays.stream(messages)
        .filter(msg -> msg != null && msg instanceof TigerGraphWriterCommitMessage)
        .map(msg -> ((TigerGraphWriterCommitMessage) msg).getLoadedRows())
        .reduce(0L, (a, b) -> a + b);
  }

  protected String getTaskSummury(WriterCommitMessage[] messages) {
    return Arrays.stream(messages)
        .filter(msg -> msg != null && msg instanceof TigerGraphWriterCommitMessage)
        .map(msg -> msg.toString())
        .collect(Collectors.joining("\n"));
  }
}
