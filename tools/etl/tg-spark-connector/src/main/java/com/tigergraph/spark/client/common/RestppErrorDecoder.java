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

import com.tigergraph.spark.log.LoggerFactory;
import feign.Response;
import feign.RetryableException;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;

/**
 * Responsible for checking the HTTP status code to determine whether the request is retryable,
 * throw a {@link RetryableException} or not.
 */
public class RestppErrorDecoder implements ErrorDecoder {
  private static final Logger logger = LoggerFactory.getLogger(RestppErrorDecoder.class);
  static final String CLIENT_CERT_VERIFY_FAILED_MSG = "Client certificate verification failed";

  /** Retryable codes for transient service/network issues. */
  public static final List<Integer> DEFAULT_SERVER_RETRYABLE_CODE =
      Arrays.asList(
          HttpStatus.SC_REQUEST_TIMEOUT,
          HttpStatus.SC_BAD_GATEWAY,
          HttpStatus.SC_SERVICE_UNAVAILABLE,
          HttpStatus.SC_GATEWAY_TIMEOUT,
          HttpStatus.SC_TOO_MANY_REQUESTS);

  /** Retryable codes for auth-related failures (e.g. token expiration). */
  public static final List<Integer> DEFAULT_AUTH_RETRYABLE_CODE =
      Arrays.asList(HttpStatus.SC_FORBIDDEN, HttpStatus.SC_UNAUTHORIZED);

  private final List<Integer> serverRetryableCode;
  private final List<Integer> authRetryableCode;
  private final Decoder decoder;
  final ErrorDecoder errDecoder = new ErrorDecoder.Default();

  public RestppErrorDecoder(Decoder decoder) {
    this.decoder = decoder;
    this.serverRetryableCode = DEFAULT_SERVER_RETRYABLE_CODE;
    this.authRetryableCode = DEFAULT_AUTH_RETRYABLE_CODE;
  }

  /**
   * @param serverRetryableCode retryable service/network HTTP codes (auth codes still use {@link
   *     #DEFAULT_AUTH_RETRYABLE_CODE}).
   */
  public RestppErrorDecoder(Decoder decoder, Integer... serverRetryableCode) {
    this.decoder = decoder;
    this.serverRetryableCode = Arrays.asList(serverRetryableCode);
    this.authRetryableCode = DEFAULT_AUTH_RETRYABLE_CODE;
  }

  public RestppErrorDecoder(
      Decoder decoder, List<Integer> serverRetryableCode, List<Integer> authRetryableCode) {
    this.decoder = decoder;
    this.serverRetryableCode = serverRetryableCode;
    this.authRetryableCode = authRetryableCode;
  }

  /** Return a new decoder with the same server retryable codes but no auth-related retries. */
  public RestppErrorDecoder withoutAuthRetry() {
    return new RestppErrorDecoder(this.decoder, this.serverRetryableCode, Collections.emptyList());
  }

  /**
   * Wrap the exception from default decoder into the {@link RetryableException} or directly throw
   * it depending on HTTP status code.
   */
  @Override
  public Exception decode(String methodKey, Response response) {
    Exception e = errDecoder.decode(methodKey, response);
    if (!(e instanceof RetryableException)) {
      if (e.getMessage() != null && e.getMessage().contains(CLIENT_CERT_VERIFY_FAILED_MSG)) {
        return e;
      }
      int status = response.status();
      boolean shouldRetry =
          serverRetryableCode.contains(status) || authRetryableCode.contains(status);
      if (shouldRetry) {
        logger.info("{}, attempt to retry.", e.getMessage());
        return new RetryableException(
            response.status(),
            e.getMessage(),
            response.request().httpMethod(),
            e,
            null,
            response.request());
      }
    }
    return e;
  }
}
