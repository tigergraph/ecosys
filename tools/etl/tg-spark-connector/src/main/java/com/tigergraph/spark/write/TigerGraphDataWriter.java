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
import com.tigergraph.spark.client.Write;
import com.tigergraph.spark.client.Write.LoadingResponse;
import com.tigergraph.spark.log.LoggerFactory;
import com.tigergraph.spark.util.Options;
import com.tigergraph.spark.util.Utils;
import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.connector.write.DataWriter;
import org.apache.spark.sql.types.BooleanType;
import org.apache.spark.sql.types.ByteType;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DateType;
import org.apache.spark.sql.types.DecimalType;
import org.apache.spark.sql.types.DoubleType;
import org.apache.spark.sql.types.FloatType;
import org.apache.spark.sql.types.IntegerType;
import org.apache.spark.sql.types.LongType;
import org.apache.spark.sql.types.ShortType;
import org.apache.spark.sql.types.StringType;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.types.TimestampNTZType;
import org.apache.spark.sql.types.TimestampType;
import org.slf4j.Logger;

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
  private final List<BiFunction<InternalRow, Integer, String>> converters;

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
    // Set converters for each field according to the data type
    converters = getConverters(schema);
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
            .mapToObj(i -> record.isNullAt(i) ? "" : converters.get(i).apply(record, i))
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

  /** Pre-generate the converter for each field of the source dataframe's schema */
  protected static List<BiFunction<InternalRow, Integer, String>> getConverters(StructType schema) {
    return Stream.of(schema.fields())
        .map((f) -> getConverter(f.dataType()))
        .collect(Collectors.toList());
  }

  /**
   * Get converter for the specific Spark type for converting it to string, which then can be used
   * to build delimited data for loading job
   */
  private static BiFunction<InternalRow, Integer, String> getConverter(DataType dt) {
    if (dt instanceof IntegerType) {
      return (row, idx) -> String.valueOf(row.getInt(idx));
    } else if (dt instanceof LongType) {
      return (row, idx) -> String.valueOf(row.getLong(idx));
    } else if (dt instanceof DoubleType) {
      return (row, idx) -> String.valueOf(row.getDouble(idx));
    } else if (dt instanceof FloatType) {
      return (row, idx) -> String.valueOf(row.getFloat(idx));
    } else if (dt instanceof ShortType) {
      return (row, idx) -> String.valueOf(row.getShort(idx));
    } else if (dt instanceof ByteType) {
      return (row, idx) -> String.valueOf(row.getByte(idx));
    } else if (dt instanceof BooleanType) {
      return (row, idx) -> String.valueOf(row.getBoolean(idx));
    } else if (dt instanceof StringType) {
      return (row, idx) -> row.getString(idx);
    } else if (dt instanceof TimestampType || dt instanceof TimestampNTZType) {
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
      // TG DATETIME is essentially a timestamp_ntz, need to format the time in UTC
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      // Spark timestamp is stored as the number of microseconds
      return (row, idx) -> dateFormat.format(new Timestamp(row.getLong(idx) / 1000));
    } else if (dt instanceof DateType) {
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      // TG DATETIME is essentially a timestamp_ntz, need to format the time in UTC
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      // Spark date is stored as the number of days counting from 1970-01-01
      return (row, idx) -> dateFormat.format(new Date(row.getInt(idx) * 24 * 60 * 60 * 1000L));
    } else if (dt instanceof DecimalType) {
      return (row, idx) ->
          row.getDecimal(idx, ((DecimalType) dt).precision(), ((DecimalType) dt).scale())
              .toString();
    } else {
      throw new UnsupportedOperationException(
          "Unsupported Spark type: "
              + dt.typeName()
              + ", please convert it to a string that matches the LOAD statement defined in the"
              + " loading job:"
              + " https://docs.tigergraph.com/gsql-ref/current/ddl-and-loading/creating-a-loading-job#_more_complex_attribute_expressions");
    }
  }
}
