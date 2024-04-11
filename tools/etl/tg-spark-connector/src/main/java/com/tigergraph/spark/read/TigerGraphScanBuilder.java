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

import org.apache.spark.sql.connector.read.ScanBuilder;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;
import com.tigergraph.spark.TigerGraphConnection;
import com.tigergraph.spark.util.Options;

/**
 * An interface for building the {@link TigerGraphScan}. Implementations can mixin
 * SupportsPushDownXYZ interfaces to do operator pushdown, and keep the operator pushdown result in
 * the returned {@link TigerGraphScan}. When pushing down operators, Spark pushes down filters
 * first, then pushes down aggregates or applies column pruning.
 */
public class TigerGraphScanBuilder implements ScanBuilder {
  private final TigerGraphConnection conn;
  private final StructType schema;

  public TigerGraphScanBuilder(CaseInsensitiveStringMap info, StructType schema) {
    Options opts = new Options(info.asCaseSensitiveMap(), Options.OptionType.READ, false);
    this.conn = new TigerGraphConnection(opts);
    this.schema = schema;
  }

  @Override
  public TigerGraphScan build() {
    return new TigerGraphScan(conn, schema);
  }
}
