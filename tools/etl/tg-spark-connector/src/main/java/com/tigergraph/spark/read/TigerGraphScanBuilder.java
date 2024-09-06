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
import org.apache.spark.sql.connector.read.ScanBuilder;

/**
 * An interface for building the {@link TigerGraphScan}. Implementations can mixin
 * SupportsPushDownXYZ interfaces to do operator pushdown, and keep the operator pushdown result in
 * the returned {@link TigerGraphScan}. When pushing down operators, Spark pushes down filters
 * first, then pushes down aggregates or applies column pruning.
 */
public class TigerGraphScanBuilder implements ScanBuilder {
  private final TigerGraphConnection conn;
  private final TigerGraphResultAccessor accessor;

  public TigerGraphScanBuilder(TigerGraphConnection conn, TigerGraphResultAccessor accessor) {
    this.conn = conn;
    this.accessor = accessor;
  }

  @Override
  public TigerGraphScan build() {
    return new TigerGraphScan(conn, accessor);
  }
}
