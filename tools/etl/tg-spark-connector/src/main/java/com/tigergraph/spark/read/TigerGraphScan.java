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

import org.apache.spark.sql.connector.catalog.TableCapability;
import org.apache.spark.sql.connector.read.Scan;
import org.apache.spark.sql.types.StructType;
import com.tigergraph.spark.TigerGraphConnection;

/**
 * A logical representation of a data source scan. This interface is used to provide logical
 * information, like what the actual read schema is.
 *
 * <p>This logical representation is shared between batch scan, micro-batch streaming scan and
 * continuous streaming scan. Data sources must implement the corresponding methods in this
 * interface, to match what the table promises to support. For example, {@link #toBatch()} must be
 * implemented, if the {@link Table} that creates this {@link Scan} returns {@link
 * TableCapability#BATCH_READ} support in its {@link Table#capabilities()}.
 */
public class TigerGraphScan implements Scan {
  private final TigerGraphConnection conn;
  private final StructType schema;

  public TigerGraphScan(TigerGraphConnection connection, StructType schema) {
    this.conn = connection;
    this.schema = schema;
  }

  /** Get the processed schema after column pruning (select) */
  @Override
  public StructType readSchema() {
    // todo: with schema inference
    return schema;
  }

  @Override
  public TigerGraphBatch toBatch() {
    return new TigerGraphBatch(conn, schema);
  }
}
