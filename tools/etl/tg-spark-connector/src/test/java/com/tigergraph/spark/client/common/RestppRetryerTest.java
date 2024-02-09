package com.tigergraph.spark.client.common;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Test;
import feign.Request;
import feign.RetryableException;
import feign.Request.HttpMethod;

public class RestppRetryerTest {
  @Test
  public void testMixedRetryer() {
    // Token exception: sleep 3s, retry 1 times
    // IO exception: sleep 7s, retry 2 times
    // Server exception: sleep 12s, retry 2 times
    RestppRetryer retryer = new RestppRetryer(7000, 7000, 2, 12000, 12000, 2);
    Request req =
        Request.create(HttpMethod.GET, "", new HashMap<>(), null, Charset.defaultCharset(), null);
    RetryableException ioEx = new RetryableException(0, null, null, new IOException(), null, req);
    RetryableException tokenEx =
        new RetryableException(HttpStatus.SC_FORBIDDEN, null, null, null, req);
    RetryableException serverEx =
        new RetryableException(HttpStatus.SC_SERVICE_UNAVAILABLE, null, null, null, req);
    // 1. attempt 1 on ioEx
    long duration = getRetryDuration(retryer, ioEx);
    assertTrue(duration <= 10000 && duration >= 5000);
    // 2. attempt 1 on serverEx
    duration = getRetryDuration(retryer, serverEx);
    assertTrue(duration <= 16000 && duration >= 8000);
    // 3. attempt 1 on tokenEx
    duration = getRetryDuration(retryer, tokenEx);
    assertTrue(duration <= 5000 && duration >= 2000);
    // 4. attempt 2 on ioEx
    duration = getRetryDuration(retryer, ioEx);
    assertTrue(duration <= 10000 && duration >= 5000);
    // 5. attempt 2 on serverEx
    duration = getRetryDuration(retryer, serverEx);
    assertTrue(duration <= 16000 && duration >= 8000);
    // 6. attempt 2 on tokenEx, should throw exception as attemps exceeds
    assertThrows(RetryableException.class, () -> retryer.continueOrPropagate(tokenEx));
    // 7. attempt 3 on ioEx, should throw exception as attemps exceeds
    assertThrows(RetryableException.class, () -> retryer.continueOrPropagate(ioEx));
    // 8. attempt 3 on serverEx, should throw exception as attemps exceeds
    assertThrows(RetryableException.class, () -> retryer.continueOrPropagate(serverEx));
  }

  private long getRetryDuration(RestppRetryer retryer, RetryableException e) {
    long start = System.currentTimeMillis();
    retryer.continueOrPropagate(e);
    long duration = System.currentTimeMillis() - start;
    return duration;
  }

  @Test
  public void testJitter() {
    for (int i = 0; i < 10000; i++) {
      long next = RestppRetryer.jitter(10000);
      assertTrue(next >= 7500 && next <= 12500, () -> String.valueOf(next));
    }
  }
}
