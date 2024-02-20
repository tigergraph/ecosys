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
package com.tigergraph.spark.client;

import feign.*;
import com.tigergraph.spark.client.common.RestppResponse;

/** APIs for RESTPP authentication */
public interface Auth {
  public static final long TOKEN_LIFETIME_SEC = 6 * 60 * 60; // 6h

  /**
   * A helper function to check whether RESTPP auth is enabled, if not, an exception of 404 error
   * will be thrown.
   */
  @RequestLine("GET /restpp/requesttoken")
  AuthResponse checkAuthEnabled();

  @RequestLine("POST /restpp/requesttoken")
  @Headers({"Content-Type: application/json", "Authorization: Basic {basicAuth}"})
  @Body("%7B\"graph\": \"{graph}\", \"lifetime\": \"{lifetime}\"%7D")
  AuthResponse requestTokenWithUserPass(
      @Param("graph") String graph,
      @Param("basicAuth") String basicAuth,
      @Param("lifetime") long lifetime);

  @RequestLine("POST /restpp/requesttoken")
  @Headers({"Content-Type: application/json"})
  @Body("%7B\"secret\": \"{secret}\", \"lifetime\": \"{lifetime}\"%7D")
  AuthResponse requestTokenWithSecret(
      @Param("secret") String secret, @Param("lifetime") long lifetime);

  @RequestLine("PUT /restpp/requesttoken")
  @Headers({"Content-Type: application/json", "Authorization: Basic {basicAuth}"})
  @Body("%7B\"token\": \"{token}\", \"lifetime\": \"{lifetime}\"%7D")
  AuthResponse refreshTokenWithUserPass(
      @Param("token") String token,
      @Param("basicAuth") String basicAuth,
      @Param("lifetime") long lifetime);

  @RequestLine("PUT /restpp/requesttoken")
  @Headers({"Content-Type: application/json"})
  @Body("%7B\"secret\": \"{secret}\", \"token\": \"{token}\", \"lifetime\": \"{lifetime}\"%7D")
  AuthResponse refreshTokenWithSecrect(
      @Param("token") String token,
      @Param("secret") String secret,
      @Param("lifetime") long lifetime);

  public class AuthResponse extends RestppResponse {
    public long expiration;
    public String token;
  }
}
