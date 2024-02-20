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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.connector.write.DataWriter;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tigergraph.spark.TigerGraphConnection;
import com.tigergraph.spark.client.Write;
import com.tigergraph.spark.client.Write.LoadingResponse;
import com.tigergraph.spark.util.Options;
import com.tigergraph.spark.util.Utils;

/** The data writer of an executor responsible for writing data for an input RDD partition. */
public class TigerGraphDataWriter implements DataWriter<InternalRow> {
  private static final Logger logger = LoggerFactory.getLogger(TigerGraphDataWriter.class);

  private final StructType schema;
  private final int partitionId;
  private final long taskId;
  private final long epochId;

  private final Write write;
  private final String version;
  private final String jobId;
  private final String graph;
  private final String sep;
  private final String eol;
  private final int maxBatchSizeInBytes;
  private final Map<String, Object> queryMap;

  private final StringBuilder sb = new StringBuilder();
  private int sbOffset = 0;
  // Metrics
  private long totalLines = 0;

  /** For Streaming write */
  TigerGraphDataWriter(
      StructType schema, TigerGraphConnection conn, int partitionId, long taskId, long epochId) {
    this.schema = schema;
    this.partitionId = partitionId;
    this.taskId = taskId;
    this.epochId = epochId;
    this.write = conn.getWrite();
    this.version = conn.getVersion();
    this.jobId = conn.getLoadingJobId();

    Options opts = conn.getOpts();
    this.graph = opts.getString(Options.GRAPH);
    this.maxBatchSizeInBytes = opts.getInt(Options.LOADING_BATCH_SIZE_BYTES);
    this.sep = opts.getString(Options.LOADING_SEPARATOR);
    this.eol = opts.getString(Options.LOADING_EOL);

    queryMap = new HashMap<>();
    queryMap.put("tag", opts.getString(Options.LOADING_JOB));
    queryMap.put("filename", opts.getString(Options.LOADING_FILENAME));
    queryMap.put("sep", opts.getString(Options.LOADING_SEPARATOR));
    queryMap.put("eol", opts.getString(Options.LOADING_EOL));
    queryMap.put("timeout", opts.getInt(Options.LOADING_TIMEOUT_MS));
    if (Utils.versionCmp(version, "3.9.4") >= 0) {
      queryMap.put("jobid", jobId);
      if (opts.containsOption(Options.LOADING_MAX_NUM_ERROR)) {
        queryMap.put("max_num_error", opts.getInt(Options.LOADING_MAX_NUM_ERROR));
      }
      if (opts.containsOption(Options.LOADING_MAX_PERCENT_ERROR)) {
        queryMap.put("max_percent_error", opts.getInt(Options.LOADING_MAX_PERCENT_ERROR));
      }
    }
    logger.info(
        "Created data writer for partition {}, task {}, epochId {}", partitionId, taskId, epochId);
  }

  /** For Batch write */
  TigerGraphDataWriter(StructType schema, TigerGraphConnection conn, int partitionId, long taskId) {
    this(schema, conn, partitionId, taskId, (long) -1);
  }

  @Override
  public void close() throws IOException {
    // no-op
  }

  @Override
  public void write(InternalRow record) throws IOException {
    String line =
        IntStream.range(0, record.numFields())
            .mapToObj(i -> record.isNullAt(i) ? "" : record.getString(i))
            .collect(Collectors.joining(sep));
    if (sb.length() + line.length() + eol.length() > maxBatchSizeInBytes) {
      postToDDL();
    }
    sb.append(line).append(eol);
    sbOffset++;
  }

  private void postToDDL() {
    LoadingResponse resp = write.ddl(graph, sb.toString(), queryMap);
    logger.info("Upsert {} rows to TigerGraph graph {}", sbOffset, graph);
    resp.panicOnFail();
    Utils.removeUserData(resp.results);
    // process stats
    if (resp.hasInvalidRecord()) {
      logger.error("Found rejected rows, it won't abort the loading: ");
      logger.error(resp.results.toPrettyString());
    } else {
      logger.debug(resp.results.toPrettyString());
    }
    totalLines += sbOffset;
    sbOffset = 0;
    sb.setLength(0);
  }

  @Override
  public TigerGraphWriterCommitMessage commit() throws IOException {
    if (sb.length() > 0) {
      postToDDL();
    }
    logger.info(
        "Finished writing {} rows to TigerGraph. Partition {}, task {}, epoch {}.",
        totalLines,
        partitionId,
        taskId,
        epochId);
    return new TigerGraphWriterCommitMessage(totalLines, partitionId, taskId);
  }

  @Override
  public void abort() throws IOException {
    logger.error(
        "Write aborted with {} records loaded. Partition {}, task {}, epoch {}",
        totalLines,
        partitionId,
        taskId,
        epochId);
  }
}
