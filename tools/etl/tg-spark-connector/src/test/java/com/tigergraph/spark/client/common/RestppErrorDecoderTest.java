package com.tigergraph.spark.client.common;

import java.nio.charset.Charset;
import java.util.HashMap;

import feign.RetryableException;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Test;
import feign.Request;
import feign.Request.HttpMethod;
import feign.Response;
import com.tigergraph.spark.constant.ErrorCode;

import static org.junit.jupiter.api.Assertions.*;

public class RestppErrorDecoderTest {

  @Test
  public void testDecode() {
    // test whether the decoded exception is RetryableException
    RestppErrorDecoder decoder =
        new RestppErrorDecoder(
            RestppDecoder.INSTANCE, HttpStatus.SC_SERVER_ERROR, HttpStatus.SC_BAD_GATEWAY);
    Request req =
        Request.create(HttpMethod.GET, "", new HashMap<>(), null, Charset.defaultCharset(), null);
    // not in the retryable code
    assertFalse(
        decoder.decode(
                "GET",
                Response.builder().request(req).status(HttpStatus.SC_REQUEST_TIMEOUT).build())
            instanceof RetryableException);
    // in retryable code
    assertTrue(
        decoder.decode(
                "GET", Response.builder().request(req).status(HttpStatus.SC_SERVER_ERROR).build())
            instanceof RetryableException);
    // 403 forbidden, but not due to token expiration
    assertFalse(
        decoder.decode(
                "GET",
                Response.builder()
                    .request(req)
                    .status(HttpStatus.SC_FORBIDDEN)
                    .body("{\"code\":\"not_expire\"}", Charset.forName("UTF-8"))
                    .build())
            instanceof RetryableException);
    // 403 forbidden, token expiration
    assertTrue(
        decoder.decode(
                "GET",
                Response.builder()
                    .request(req)
                    .status(HttpStatus.SC_FORBIDDEN)
                    .body(
                        "{\"code\":\"" + ErrorCode.TOKEN_EXPIRATION + "\"}",
                        Charset.forName("UTF-8"))
                    .build())
            instanceof RetryableException);
  }
}
