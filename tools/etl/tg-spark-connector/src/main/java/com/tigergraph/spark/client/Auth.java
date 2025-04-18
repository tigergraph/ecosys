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

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.tigergraph.spark.client.common.RestppErrorException;
import com.tigergraph.spark.util.Utils;

import feign.Body;
import feign.FeignException;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

/** APIs for RESTPP authentication */
public interface Auth {
  public static final long TOKEN_LIFETIME_SEC = 6 * 60 * 60; // 6h

  /**
   * A helper function to check whether RESTPP auth is enabled, if not, an exception of 401/403
   * error will be thrown.
   */
  @RequestLine("GET /restpp/version")
  RestppAuthResponse getVersionWithoutToken();

  default Boolean checkAuthEnabled() {
    try {
      getVersionWithoutToken();
    } catch (FeignException e) {
      // The code is different in different RESTPP versions
      if (e.status() == 401 || e.status() == 403) {
        return true;
      }
    }
    return false;
  }

  /** Prior to TG 4.1.0 */
  @RequestLine("POST /restpp/requesttoken")
  @Headers({"Content-Type: application/json", "Authorization: Basic {basicAuth}"})
  @Body("%7B\"graph\": \"{graph}\", \"lifetime\": \"{lifetime}\"%7D")
  RestppAuthResponse requestTokenWithUserPassV0(
      @Param("graph") String graph,
      @Param("basicAuth") String basicAuth,
      @Param("lifetime") long lifetime);

  /** Since TG 4.1.0 */
  @RequestLine("POST /gsql/v1/tokens")
  @Headers({"Content-Type: application/json", "Authorization: Basic {basicAuth}"})
  @Body("%7B\"graph\": \"{graph}\", \"lifetime\": \"{lifetime}\"%7D")
  RestppAuthResponse requestTokenWithUserPassV1(
      @Param("graph") String graph,
      @Param("basicAuth") String basicAuth,
      @Param("lifetime") long lifetime);

  default RestppAuthResponse requestTokenWithUserPass(
      String version, String graph, String basicAuth, long lifetime) {
    if (Utils.versionCmp(version, "4.1.0") >= 0) {
      return requestTokenWithUserPassV1(graph, basicAuth, lifetime);
    } else {
      return requestTokenWithUserPassV0(graph, basicAuth, lifetime);
    }
  }

  /** Prior to TG 4.1.0 */
  @RequestLine("POST /restpp/requesttoken")
  @Headers({"Content-Type: application/json"})
  @Body("%7B\"secret\": \"{secret}\", \"lifetime\": \"{lifetime}\"%7D")
  RestppAuthResponse requestTokenWithSecretV0(
      @Param("secret") String secret, @Param("lifetime") long lifetime);

  /** Since TG 4.1.0 */
  @RequestLine("POST /gsql/v1/tokens")
  @Headers({"Content-Type: application/json"})
  @Body("%7B\"secret\": \"{secret}\", \"lifetime\": \"{lifetime}\"%7D")
  RestppAuthResponse requestTokenWithSecretV1(
      @Param("secret") String secret, @Param("lifetime") long lifetime);

  default RestppAuthResponse requestTokenWithSecret(String version, String secret, long lifetime) {
    if (Utils.versionCmp(version, "4.1.0") >= 0) {
      return requestTokenWithSecretV1(secret, lifetime);
    } else {
      return requestTokenWithSecretV0(secret, lifetime);
    }
  }

  /** Prior to TG 4.1.0 */
  @RequestLine("PUT /restpp/requesttoken")
  @Headers({"Content-Type: application/json", "Authorization: Basic {basicAuth}"})
  @Body("%7B\"token\": \"{token}\", \"lifetime\": \"{lifetime}\"%7D")
  RestppAuthResponse refreshTokenWithUserPass(
      @Param("token") String token,
      @Param("basicAuth") String basicAuth,
      @Param("lifetime") long lifetime);

  /** Prior to TG 4.1.0 */
  @RequestLine("PUT /restpp/requesttoken")
  @Headers({"Content-Type: application/json"})
  @Body("%7B\"secret\": \"{secret}\", \"token\": \"{token}\", \"lifetime\": \"{lifetime}\"%7D")
  RestppAuthResponse refreshTokenWithSecrect(
      @Param("token") String token,
      @Param("secret") String secret,
      @Param("lifetime") long lifetime);

  @RequestLine("POST")
  @Headers("Content-Type: application/x-www-form-urlencoded")
  OAuth2Response refreshOAuth2Token(Map<String, ?> formParams);

  public class OAuth2Response implements AuthResponse {
    public String access_token;
    public int expires_in;
    public String error;
    public String error_description;

    @Override
    public String getToken() {
      if (!Utils.isEmpty(access_token)) {
        return access_token;
      } else {
        return "";
      }
    }

    @Override
    public String getExpiration() {
      return String.valueOf(expires_in);
    }

    @Override
    public void panicOnFail() {
      if (!Utils.isEmpty(error)) {
        throw new RestppErrorException(error, error_description);
      }
    }
  }

  public class RestppAuthResponse implements AuthResponse {
    public String expiration;
    public String token;
    public String code;
    public boolean error;
    public String message;
    public JsonNode results;

    /**
     * RESTPP has different response format in different versions, we need to try to parse token
     * from different places.
     */
    @Override
    public String getToken() {
      if (!Utils.isEmpty(token)) {
        return token;
      } else if (results != null) {
        return results.path("token").asText();
      } else {
        return "";
      }
    }

    @Override
    public String getExpiration() {
      if (!Utils.isEmpty(expiration)) {
        return expiration;
      } else if (results != null) {
        return results.path("expiration").asText();
      } else {
        return "";
      }
    }

    @Override
    public void panicOnFail() {
      if (error) {
        throw new RestppErrorException(code, message);
      }
    }
  }

  public interface AuthResponse {
    public String getToken();

    public String getExpiration();

    void panicOnFail();
  }
}
