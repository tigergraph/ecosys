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
package com.tigergraph.spark;

import org.apache.spark.sql.connector.catalog.SupportsRead;
import org.apache.spark.sql.connector.catalog.SupportsWrite;
import org.apache.spark.sql.connector.catalog.TableCapability;
import org.apache.spark.sql.connector.write.LogicalWriteInfo;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;
import java.util.HashSet;
import java.util.Set;
import com.tigergraph.spark.read.TigerGraphResultAccessor;
import com.tigergraph.spark.read.TigerGraphScanBuilder;
import com.tigergraph.spark.write.TigerGraphWriteBuilder;

/** The representation of logical structured data set of a TG, with supported capabilities. */
public class TigerGraphTable implements SupportsWrite, SupportsRead {

  private static final String TABLE_NAME = "TigerGraphTable";
  private final TigerGraphResultAccessor accessor;
  private final TigerGraphConnection conn;

  public TigerGraphTable(TigerGraphResultAccessor accessor, TigerGraphConnection conn) {
    this.accessor = accessor;
    this.conn = conn;
  }

  @Override
  public String name() {
    return TABLE_NAME;
  }

  @Override
  public StructType schema() {
    return accessor.getSchema();
  }

  @Override
  public Set<TableCapability> capabilities() {
    return new HashSet<TableCapability>(2) {
      {
        add(TableCapability.BATCH_WRITE);
        add(TableCapability.STREAMING_WRITE);
        add(TableCapability.BATCH_READ);
      }
    };
  }

  @Override
  public TigerGraphWriteBuilder newWriteBuilder(LogicalWriteInfo info) throws RuntimeException {
    return new TigerGraphWriteBuilder(info, conn);
  }

  @Override
  public TigerGraphScanBuilder newScanBuilder(CaseInsensitiveStringMap options) {
    return new TigerGraphScanBuilder(conn, accessor);
  }
}
