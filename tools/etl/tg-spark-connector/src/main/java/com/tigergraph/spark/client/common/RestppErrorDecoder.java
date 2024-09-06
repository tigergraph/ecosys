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

import com.tigergraph.spark.constant.ErrorCode;
import com.tigergraph.spark.log.LoggerFactory;
import feign.Response;
import feign.RetryableException;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import java.util.Arrays;
import java.util.List;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;

/**
 * Responsible for checking the HTTP status code to determine whether the request is retryable,
 * throw a {@link RetryableException} or not.
 */
public class RestppErrorDecoder implements ErrorDecoder {
  private static final Logger logger = LoggerFactory.getLogger(RestppErrorDecoder.class);

  static final List<Integer> DEFAULT_RETRYABLE_CODE =
      Arrays.asList(
          HttpStatus.SC_REQUEST_TIMEOUT,
          HttpStatus.SC_BAD_GATEWAY,
          HttpStatus.SC_SERVICE_UNAVAILABLE,
          HttpStatus.SC_GATEWAY_TIMEOUT);
  final List<Integer> retryableCode;
  final Decoder decoder;
  final ErrorDecoder errDecoder = new ErrorDecoder.Default();

  public RestppErrorDecoder(Decoder decoder) {
    this.decoder = decoder;
    this.retryableCode = DEFAULT_RETRYABLE_CODE;
  }

  public RestppErrorDecoder(Decoder decoder, Integer... retryableCode) {
    this.decoder = decoder;
    this.retryableCode = Arrays.asList(retryableCode);
  }

  /**
   * Wrap the exception from default decoder into the {@link RetryableException} or directly throw
   * it depending on HTTP status code.
   */
  @Override
  public Exception decode(String methodKey, Response response) {
    Exception e = errDecoder.decode(methodKey, response);
    if (!(e instanceof RetryableException)) {
      boolean shouldRetry = false;
      // Retry on Server Timeout 408, 502, 503 and 504
      // or token expiration
      if (response.status() == HttpStatus.SC_FORBIDDEN) {
        try {
          // Retrieve the body from exception and decode to get the RESTPP error code.
          // Can't directly decode the response because the inputstream has been closed.
          byte[] body = ((feign.FeignException) e).responseBody().get().array();
          RestppResponse resp =
              (RestppResponse)
                  decoder.decode(response.toBuilder().body(body).build(), RestppResponse.class);
          if (ErrorCode.TOKEN_EXPIRATION.equals(resp.code)) {
            logger.info("{} token expiration, attempt to refresh and retry.", resp.code);
            shouldRetry = true;
          }
        } catch (Exception ex) {
          // no-op if failed to decode body of 403 response, let it fail fast.
        }
      } else if (retryableCode.contains(response.status())) {
        shouldRetry = true;
      }
      if (shouldRetry) {
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
