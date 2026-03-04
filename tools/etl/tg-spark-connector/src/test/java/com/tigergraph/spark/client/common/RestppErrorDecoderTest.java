package com.tigergraph.spark.client.common;

import java.nio.charset.Charset;
import java.util.Collections;
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
    // 403 forbidden (auth-related)
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
    // 403 with mTLS cert verification failure should not be retryable
    assertFalse(
        decoder.decode(
                "GET",
                Response.builder()
                    .request(req)
                    .status(HttpStatus.SC_FORBIDDEN)
                    .body(
                        "{\"message\":\""
                            + RestppErrorDecoder.CLIENT_CERT_VERIFY_FAILED_MSG
                            + "\"}",
                        Charset.forName("UTF-8"))
                    .build())
            instanceof RetryableException);
    // 401 unauthorized (auth-related)
    assertTrue(
        decoder.decode(
                "GET",
                Response.builder()
                    .request(req)
                    .status(HttpStatus.SC_UNAUTHORIZED)
                    .body(
                        "{\"code\":\"" + ErrorCode.TOKEN_EXPIRATION + "\"}",
                        Charset.forName("UTF-8"))
                    .build())
            instanceof RetryableException);
  }

  @Test
  public void testDecodeWithoutAuthRetryCode() {
    RestppErrorDecoder decoder =
        new RestppErrorDecoder(
            RestppDecoder.INSTANCE,
            RestppErrorDecoder.DEFAULT_SERVER_RETRYABLE_CODE,
            Collections.emptyList());
    Request req =
        Request.create(HttpMethod.GET, "", new HashMap<>(), null, Charset.defaultCharset(), null);
    // Auth-related codes should not be retryable when auth retry codes are empty
    assertFalse(
        decoder.decode(
                "GET",
                Response.builder()
                    .request(req)
                    .status(HttpStatus.SC_UNAUTHORIZED)
                    .body(
                        "{\"code\":\"" + ErrorCode.TOKEN_EXPIRATION + "\"}",
                        Charset.forName("UTF-8"))
                    .build())
            instanceof RetryableException);
    assertFalse(
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
