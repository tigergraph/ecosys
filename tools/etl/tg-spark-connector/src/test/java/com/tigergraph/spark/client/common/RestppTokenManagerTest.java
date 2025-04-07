package com.tigergraph.spark.client.common;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.tigergraph.spark.TigerGraphConnection.AuthType;
import com.tigergraph.spark.client.Auth;
import com.tigergraph.spark.client.Builder;
import feign.form.FormEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
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
  private Auth oauth;
  private Map<String, Object> oauth2Parameters = new HashMap<>();

  @BeforeEach
  void setup(WireMockRuntimeInfo wm) {
    auth = (new Builder()).build(Auth.class, wm.getHttpBaseUrl());
    oauth =
        (new Builder())
            .setEncoder(new FormEncoder(RestppEncoder.INSTANCE))
            .build(Auth.class, wm.getHttpBaseUrl() + "/authorization-server.com");
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
    // Oauth2 token
    // Request new token
    stubFor(
        post("/authorization-server.com")
            .atPriority(1)
            .willReturn(okJson("{\"expires_in\":3599,\"access_token\":\"new_token\"}")));
  }

  @Test
  public void testRefreshLegacyTokenBySecret() {
    String original_token = token.get();
    RestppTokenManager tokenMgr =
        new RestppTokenManager(
            auth, AuthType.SECRET, "social", "3.10.1", token, secret, "", oauth2Parameters);
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
        new RestppTokenManager(
            auth,
            AuthType.USERNAME_PASSWORD,
            "social",
            "3.99.99",
            token,
            "",
            basicAuth,
            oauth2Parameters);
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
        new RestppTokenManager(
            auth, AuthType.SECRET, "social", "4.1.0", token, secret, "", oauth2Parameters);
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
        new RestppTokenManager(
            auth,
            AuthType.USERNAME_PASSWORD,
            "social",
            "4.1.0",
            token,
            "",
            basicAuth,
            oauth2Parameters);
    assertEquals("original_token", token.get());
    tokenMgr.refreshToken();
    verify(
        exactly(1),
        postRequestedFor(urlEqualTo("/gsql/v1/tokens"))
            .withHeader("Authorization", equalTo("Basic " + basicAuth))
            .withRequestBody(equalTo("{\"graph\": \"social\", \"lifetime\": \"21600\"}")));
    assertEquals("new_token", token.get());
  }

  @Test
  public void testRefreshOAuth2Token(WireMockRuntimeInfo wm) {
    oauth2Parameters.put("client_id", "alice");
    oauth2Parameters.put("client_secret", "i_m_secret");
    oauth2Parameters.put("grant_type", "client_credentials");
    oauth2Parameters.put("scope", "read");
    RestppTokenManager tokenMgr =
        new RestppTokenManager(
            oauth, AuthType.OAUTH2, "social", "4.1.0", token, "", basicAuth, oauth2Parameters);
    assertEquals("original_token", token.get());
    tokenMgr.refreshToken();
    verify(
        exactly(1),
        postRequestedFor(urlEqualTo("/authorization-server.com"))
            .withRequestBody(
                equalTo(
                    "grant_type=client_credentials&scope=read&client_secret=i_m_secret&client_id=alice")));
    assertEquals("new_token", token.get());
  }

  @Test
  public void testRefreshOAuth2TokenWithWrongSecret(WireMockRuntimeInfo wm) {
    // Oauth2 token
    // Request new token
    stubFor(
        post("/authorization-server.com")
            .atPriority(1)
            .willReturn(
                jsonResponse(
                    "{\"error\":\"unauthorized_client\",\"error_description\":\"Invalid client"
                        + " secret provided.\"}",
                    400)));
    oauth2Parameters.put("client_id", "alice");
    oauth2Parameters.put("client_secret", "i_m_invalid");
    oauth2Parameters.put("grant_type", "client_credentials");
    oauth2Parameters.put("scope", "read");
    RestppTokenManager tokenMgr =
        new RestppTokenManager(
            oauth, AuthType.OAUTH2, "social", "4.1.0", token, "", basicAuth, oauth2Parameters);
    assertEquals("original_token", token.get());
    assertThrows(RestppErrorException.class, () -> tokenMgr.refreshToken());
  }

  @Test
  public void testNonRefreshableAuthType() throws JsonMappingException, JsonProcessingException {
    RestppTokenManager tokenMgr =
        new RestppTokenManager(
            oauth,
            AuthType.NON_REFRESHABLE,
            "social",
            "4.1.0",
            token,
            "",
            basicAuth,
            oauth2Parameters);
    assertEquals("original_token", token.get());
    assertThrows(RestppErrorException.class, () -> tokenMgr.refreshToken());
  }
}
