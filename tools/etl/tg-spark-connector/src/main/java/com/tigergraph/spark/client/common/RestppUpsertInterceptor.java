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
package com.tigergraph.spark.client.common;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/** Request interceptor for adding common RESTPP upsert headers */
public class RestppUpsertInterceptor implements RequestInterceptor {

  private final boolean atomic;
  private final int timeoutMs;

  /**
   * Request interceptor for adding upsert headers
   *
   * @param atomic Whether to use atomic transactions for upsert operations
   * @param timeoutMs GSQL timeout in ms, 0 means default value
   */
  public RestppUpsertInterceptor(boolean atomic, int timeoutMs) {
    this.atomic = atomic;
    this.timeoutMs = timeoutMs;
  }

  @Override
  public void apply(RequestTemplate template) {
    // Set atomic level header
    template.header("gsql-atomic-level", atomic ? "atomic" : "nonatomic");

    // Set timeout header if specified
    if (timeoutMs > 0) {
      template.header("gsql-timeout", String.valueOf(timeoutMs));
    }
  }
}
