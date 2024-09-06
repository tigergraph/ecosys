package com.tigergraph.spark.client.common;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.tigergraph.spark.client.Auth;
import com.tigergraph.spark.client.Builder;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest
public class RestppTokenManagerTest {

  private static final String basicAuth =
      new String(Base64.getEncoder().encode(("tigergraph:tigergraph").getBytes()));
  private static final String secret = "i_am_secret";
  private static final AtomicReference<String> token =
      new AtomicReference<String>("original_token");

  private Auth auth;

  @BeforeEach
  void setup(WireMockRuntimeInfo wm) {
    auth = (new Builder()).build(Auth.class, wm.getHttpBaseUrl());
    token.set("original_token");
    // Request new token
    stubFor(
        post("/gsql/v1/tokens")
            .atPriority(1)
            .willReturn(
                okJson(
                    "{\"error\":false,\"token\":\"new_token\",\"results\":{\"token\":\"new_token\"}}")));
    // Refresh token
    stubFor(
        put("/restpp/requesttoken")
            .atPriority(1)
            .willReturn(okJson("{\"error\":false,\"token\":\"new_token\"}")));
  }

  @Test
  public void testRefreshLegacyTokenBySecret() {
    String original_token = token.get();
    RestppTokenManager tokenMgr =
        new RestppTokenManager(auth, "social", "3.10.1", token, secret, "");
    tokenMgr.refreshToken();
    verify(
        exactly(1),
        putRequestedFor(urlEqualTo("/restpp/requesttoken"))
            .withRequestBody(
                equalTo(
                    "{\"secret\": \"i_am_secret\", \"token\": \"original_token\", \"lifetime\":"
                        + " \"21600\"}")));
    assertEquals(original_token, token.get());
  }

  @Test
  public void testRefreshLegacyTokenByUserPass() {
    String original_token = token.get();
    RestppTokenManager tokenMgr =
        new RestppTokenManager(auth, "social", "3.99.99", token, "", basicAuth);
    tokenMgr.refreshToken();
    verify(
        exactly(1),
        putRequestedFor(urlEqualTo("/restpp/requesttoken"))
            .withHeader("Authorization", equalTo("Basic " + basicAuth))
            .withRequestBody(equalTo("{\"token\": \"original_token\", \"lifetime\": \"21600\"}")));
    assertEquals(original_token, token.get());
  }

  @Test
  public void testRefreshJWTokenBySecret() {
    RestppTokenManager tokenMgr =
        new RestppTokenManager(auth, "social", "4.1.0", token, secret, "");
    assertEquals("original_token", token.get());
    tokenMgr.refreshToken();
    verify(
        exactly(1),
        postRequestedFor(urlEqualTo("/gsql/v1/tokens"))
            .withRequestBody(equalTo("{\"secret\": \"i_am_secret\", \"lifetime\": \"21600\"}")));
    assertEquals("new_token", token.get());
  }

  @Test
  public void testRefreshJWTokenByUserPass() {
    RestppTokenManager tokenMgr =
        new RestppTokenManager(auth, "social", "4.1.0", token, "", basicAuth);
    assertEquals("original_token", token.get());
    tokenMgr.refreshToken();
    verify(
        exactly(1),
        postRequestedFor(urlEqualTo("/gsql/v1/tokens"))
            .withHeader("Authorization", equalTo("Basic " + basicAuth))
            .withRequestBody(equalTo("{\"graph\": \"social\", \"lifetime\": \"21600\"}")));
    assertEquals("new_token", token.get());
  }
}
