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

import java.util.Map;
import org.apache.spark.sql.connector.catalog.TableProvider;
import org.apache.spark.sql.connector.expressions.Transform;
import org.apache.spark.sql.sources.DataSourceRegister;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;

/**
 * A pure implementation of Spark Data Source V2 that apply data operations to existing TG objects,
 * e.g., loading job, pre-installed query, vertex or edge. DDL is unsupported.
 */
public class TigerGraphTableProvider implements TableProvider, DataSourceRegister {

  private static final String SHORT_NAME = "tigergraph";

  /**
   * For Write operation, the schema will be the schema of input dataframe; For Read operation, it
   * will be the user given schema.
   */
  @Override
  public boolean supportsExternalMetadata() {
    return true;
  }

  @Override
  public StructType inferSchema(CaseInsensitiveStringMap options) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'inferSchema'");
  }

  @Override
  public TigerGraphTable getTable(
      StructType schema, Transform[] partitioning, Map<String, String> properties) {
    return new TigerGraphTable(schema);
  }

  @Override
  public String shortName() {
    return SHORT_NAME;
  }
}
