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
package com.tigergraph.spark.client.common;

import com.tigergraph.spark.util.Utils;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * The request interceptor which is responsible for: <br>
 * 1. attach the basic auth header to the /restpp request 2. attach the bearer auth header to the
 * /gsqlserver request
 */
public class RestppAuthInterceptor implements RequestInterceptor {

  static final String GSQL_ENDPOINT = "/gsqlserver";

  private final String basicAuth;
  private final String token;
  private final boolean restAuthEnabled;

  public RestppAuthInterceptor(String basicAuth, String token, boolean restAuthEnabled) {
    this.basicAuth = basicAuth;
    this.token = token;
    this.restAuthEnabled = restAuthEnabled;
  }

  @Override
  public void apply(RequestTemplate template) {
    // If rest auth enabled, a token should be provided or requested,
    // any requests should have the auth header.
    if (restAuthEnabled) {
      template.header("Authorization", "Bearer " + token);
    } else if (template.path().contains(GSQL_ENDPOINT)) {
      // If restpp auth disabled, /gsqlserver endpoint still need authentication.
      // user/pass pair and token(requested by user/pass, not system token) are equivalent
      if (!Utils.isEmpty(token)) {
        template.header("Authorization", "Bearer " + token);
      } else if (!Utils.isEmpty(basicAuth)) {
        template.header("Authorization", "Basic " + basicAuth);
      } else {
        throw new IllegalArgumentException(
            "Failed to send request to "
                + template.path()
                + ", no username/password or token provided.");
      }
    }
  }
}
