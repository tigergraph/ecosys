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

import java.io.IOException;
import java.util.Random;
import com.tigergraph.spark.client.Auth;
import com.tigergraph.spark.util.Utils;
import feign.RetryableException;
import feign.Retryer;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mixed retryer for 3 types of errors: <br>
 * 1. token expiration <br>
 * 2. transport exception, e.g. read timeout, connect timeout <br>
 * 3. server timeout/busy, e.g. 502, 503, 504 <br>
 *
 * <p>Each of them have their own retry interval or max attempts setting.
 */
public class RestppRetryer implements Retryer {
  private static final Logger logger = LoggerFactory.getLogger(RestppRetryer.class);

  private static final Random rand = new Random();
  private static int TYPE_AUTH = 0; // token expiration
  private static int TYPE_IO = 1; // transport exception, e.g. read timeout, connect timeout
  private static int TYPE_SERVER = 2; // server timeout/busy, e.g. 502, 503, 504

  // refresh token
  private static final int REFRESH_MAX_ATTEMPTS = 1;
  private static final int REFRESH_PERIOD_MS = 3000; // 3s
  private final Auth auth;
  private final String basicAuth;
  private final String secret;
  private final String token;
  // arrays to record the retry status for different retry types
  private final int[] period = new int[3];
  private final int[] maxPeriod = new int[3];
  private final int[] maxAttempts = new int[3];
  private final int[] attempts = new int[3];
  private final int[] sleptForMillis = new int[3];

  public RestppRetryer(
      Auth auth,
      String basicAuth,
      String secret,
      String token,
      int ioPeriod,
      int ioMaxPeriod,
      int ioMaxAttempts,
      int serverPeriod,
      int serverMaxPeriod,
      int serverMaxAttempts) {
    this.auth = auth;
    this.basicAuth = basicAuth;
    this.secret = secret;
    this.token = token;

    period[TYPE_AUTH] = REFRESH_PERIOD_MS;
    maxPeriod[TYPE_AUTH] = REFRESH_PERIOD_MS;
    maxAttempts[TYPE_AUTH] = REFRESH_MAX_ATTEMPTS;

    period[TYPE_IO] = ioPeriod;
    maxPeriod[TYPE_IO] = ioMaxPeriod;
    maxAttempts[TYPE_IO] = ioMaxAttempts;

    period[TYPE_SERVER] = serverPeriod;
    maxPeriod[TYPE_SERVER] = serverMaxPeriod;
    maxAttempts[TYPE_SERVER] = serverMaxAttempts;
  }

  /**
   * Shortpath for creating retryer that doesn't support refresh token. E.g., we don't need that
   * when creating {@link Auth} client
   */
  public RestppRetryer(
      int ioPeriod,
      int ioMaxPeriod,
      int ioMaxAttempts,
      int serverPeriod,
      int serverMaxPeriod,
      int serverMaxAttempts) {
    this(
        null,
        null,
        null,
        null,
        ioPeriod,
        ioMaxPeriod,
        ioMaxAttempts,
        serverPeriod,
        serverMaxPeriod,
        serverMaxAttempts);
  }

  public void continueOrPropagate(RetryableException e) {
    // infer the retry type
    int retryType;
    String reason;
    if (e.getCause() instanceof IOException) {
      retryType = TYPE_IO;
      reason = e.getCause().toString();
    } else if (e.status() == HttpStatus.SC_FORBIDDEN) {
      retryType = TYPE_AUTH;
      reason =
          String.format(
              "Token %s expired, attempt to retry after refresh", Utils.maskString(token, 2));
    } else {
      retryType = TYPE_SERVER;
      reason = String.valueOf(e.status());
    }
    if (attempts[retryType]++ >= maxAttempts[retryType]) {
      throw e;
    }

    long interval;
    // Set the interval according to HTTP `Retry-After` header if any
    if (e.retryAfter() != null) {
      interval = e.retryAfter().getTime() - currentTimeMillis();
      if (interval > maxPeriod[retryType]) {
        interval = maxPeriod[retryType];
      }
      if (interval < 0) {
        return;
      }
    } else {
      interval = jitter(nextMaxInterval(retryType));
      logger.info("{}, retry in {} ms, attempt {}", reason, interval, attempts[retryType]);
    }
    try {
      Thread.sleep(interval);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
      throw e;
    }
    sleptForMillis[retryType] += interval;

    if (retryType == TYPE_AUTH && auth != null) {
      if (!Utils.isEmpty(basicAuth)) {
        auth.refreshTokenWithUserPass(token, basicAuth, Auth.TOKEN_LIFETIME_SEC).panicOnFail();
      } else if (!Utils.isEmpty(secret)) {
        auth.refreshTokenWithSecrect(token, secret, Auth.TOKEN_LIFETIME_SEC).panicOnFail();
      } else {
        // Don't support refresh token, throw it directly
        throw e;
      }
      logger.info(
          "Successfully refreshed token {} for {} seconds",
          Utils.maskString(token, 2),
          Auth.TOKEN_LIFETIME_SEC);
    }
  }

  @Override
  public RestppRetryer clone() {
    return new RestppRetryer(
        auth,
        basicAuth,
        secret,
        token,
        period[TYPE_IO],
        maxPeriod[TYPE_IO],
        maxAttempts[TYPE_IO],
        period[TYPE_SERVER],
        maxPeriod[TYPE_SERVER],
        maxAttempts[TYPE_SERVER]);
  }

  protected long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  // visible for testing;
  // 0.75 * interval ~ 1.25 * interval
  protected static long jitter(long interval) {
    return (long) (0.75 * interval + 0.5 * interval * rand.nextDouble());
  }

  /**
   * Calculates the time interval to a retry attempt. <br>
   * The interval increases exponentially with each attempt, at a rate of nextInterval *= 1.5 (where
   * 1.5 is the backoff factor), to the maximum interval.
   *
   * @return time in milliseconds from now until the next attempt.
   */
  private long nextMaxInterval(int retryType) {
    long interval = (long) (period[retryType] * Math.pow(1.5, attempts[retryType] - 1));
    return interval > maxPeriod[retryType] ? maxPeriod[retryType] : interval;
  }
}
