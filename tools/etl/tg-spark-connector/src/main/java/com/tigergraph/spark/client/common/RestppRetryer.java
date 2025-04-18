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

import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;

import com.tigergraph.spark.log.LoggerFactory;
import com.tigergraph.spark.util.Utils;

import feign.RetryableException;
import feign.Retryer;

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

  // In case of JWKS rotation, we need to try to refresh the token multiple times
  private static final int TOKEN_MAX_ATTEMPTS = 3;
  private static final int TOKEN_PERIOD_MS = 3000; // 3s
  private final RestppTokenManager tokenMgr;

  private final RetryCounter authRetryCounter; // token expiration
  private final RetryCounter
      ioRetryCounter; // transport exception, e.g. read timeout, connect timeout
  private final RetryCounter serverRetryCounter; // server timeout/busy, e.g. 502, 503, 504

  public RestppRetryer(
      RestppTokenManager tokenMgr,
      int ioPeriod,
      int ioMaxPeriod,
      int ioMaxAttempts,
      int serverPeriod,
      int serverMaxPeriod,
      int serverMaxAttempts) {

    this.tokenMgr = tokenMgr;
    this.authRetryCounter = new RetryCounter(TOKEN_PERIOD_MS, TOKEN_PERIOD_MS, TOKEN_MAX_ATTEMPTS);
    this.ioRetryCounter = new RetryCounter(ioPeriod, ioMaxPeriod, ioMaxAttempts);
    this.serverRetryCounter = new RetryCounter(serverPeriod, serverMaxPeriod, serverMaxAttempts);
  }

  public RestppRetryer(
      int ioPeriod,
      int ioMaxPeriod,
      int ioMaxAttempts,
      int serverPeriod,
      int serverMaxPeriod,
      int serverMaxAttempts) {
    this(
        null,
        ioPeriod,
        ioMaxPeriod,
        ioMaxAttempts,
        serverPeriod,
        serverMaxPeriod,
        serverMaxAttempts);
  }

  public void continueOrPropagate(RetryableException e) {
    RetryCounter retryCounter;
    String reason;

    if (e.getCause() instanceof IOException) {
      retryCounter = ioRetryCounter;
      reason = e.getCause().toString();
    } else if (e.status() == HttpStatus.SC_FORBIDDEN) {
      retryCounter = authRetryCounter;
      reason =
          String.format(
              "Token %s invalid or expired, attempt to retry after refreshing",
              Utils.maskString(tokenMgr == null ? "null" : tokenMgr.getToken(), 2));
    } else {
      retryCounter = serverRetryCounter;
      reason = String.valueOf(e.status());
    }

    if (retryCounter.incrementAttempts() > retryCounter.getMaxAttempts()) {
      throw e;
    }

    long interval;
    if (e.retryAfter() != null) {
      interval = e.retryAfter().getTime() - currentTimeMillis();
      if (interval > retryCounter.getMaxPeriod()) {
        interval = retryCounter.getMaxPeriod();
      }
      if (interval < 0) {
        return;
      }
    } else {
      interval = jitter(retryCounter.nextMaxInterval());
      logger.info("{}, retry in {} ms, attempt {}", reason, interval, retryCounter.getAttempts());
    }

    try {
      Thread.sleep(interval);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
      throw e;
    }

    if (retryCounter == authRetryCounter && tokenMgr != null) {
      tokenMgr.refreshToken();
      logger.info("Successfully refreshed token {}", Utils.maskString(tokenMgr.getToken(), 2));
    }
  }

  @Override
  public RestppRetryer clone() {
    return new RestppRetryer(
        tokenMgr,
        ioRetryCounter.getPeriod(),
        ioRetryCounter.getMaxPeriod(),
        ioRetryCounter.getMaxAttempts(),
        serverRetryCounter.getPeriod(),
        serverRetryCounter.getMaxPeriod(),
        serverRetryCounter.getMaxAttempts());
  }

  protected long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  protected static long jitter(long interval) {
    return (long) (0.75 * interval + 0.5 * interval * rand.nextDouble());
  }

  private class RetryCounter {
    private final int period;
    private final int maxPeriod;
    private final int maxAttempts;
    private int attempts;

    public RetryCounter(int period, int maxPeriod, int maxAttempts) {
      this.period = period;
      this.maxPeriod = maxPeriod;
      this.maxAttempts = maxAttempts;
      this.attempts = 0;
    }

    public int getPeriod() {
      return period;
    }

    public int getMaxPeriod() {
      return maxPeriod;
    }

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public int getAttempts() {
      return attempts;
    }

    public int incrementAttempts() {
      return ++attempts;
    }

    public long nextMaxInterval() {
      long interval = (long) (period * Math.pow(1.5, attempts - 1));
      return interval > maxPeriod ? maxPeriod : interval;
    }
  }
}
