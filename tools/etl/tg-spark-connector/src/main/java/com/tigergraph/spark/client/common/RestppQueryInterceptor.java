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

/** Request interceptor for adding common RESTPP query headers */
public class RestppQueryInterceptor implements RequestInterceptor {

  private Integer queryTimeout;
  private Long responseLimit;

  /**
   * Request interceptor for adding query headers
   *
   * @param queryTimeout GSQL query timeout in ms, 0 means default value
   * @param responseLimit response size limit of an HTTP request in byte, 0 means default value
   */
  public RestppQueryInterceptor(Integer queryTimeout, Long responseLimit) {
    this.queryTimeout = queryTimeout;
    this.responseLimit = responseLimit;
  }

  @Override
  public void apply(RequestTemplate template) {
    if (queryTimeout > 0) {
      template.header("GSQL-TIMEOUT", String.valueOf(queryTimeout));
    }
    if (responseLimit > 0) {
      template.header("RESPONSE-LIMIT", String.valueOf(responseLimit));
    }
  }
}
