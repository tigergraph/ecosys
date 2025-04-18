package com.tigergraph.spark.client.common;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.tigergraph.spark.TigerGraphConnection.AuthType;
import com.tigergraph.spark.client.Auth;
import com.tigergraph.spark.client.Auth.AuthResponse;
import com.tigergraph.spark.client.Auth.OAuth2Response;
import com.tigergraph.spark.util.Utils;

/**
 * Manage the RESTPP token lifetime, specifically, refresh it when it expires: - refresh token by
 * secret or username/password - refresh legacy token or JW token(tg 4.1.0)
 */
public class RestppTokenManager {

  private final Auth auth;
  private final AuthType authType;
  private final String graph;
  private final String version;
  private final AtomicReference<String> token;
  private final String secret;
  private final String basicAuth;
  private final Map<String, Object> oauth2Parameters;

  public RestppTokenManager(
      Auth auth,
      AuthType authType,
      String graph,
      String version,
      AtomicReference<String> token,
      String secret,
      String basicAuth,
      Map<String, Object> oauth2Parameters) {
    this.auth = auth;
    this.authType = authType;
    this.graph = graph;
    this.version = version;
    this.token = token;
    this.secret = secret;
    this.basicAuth = basicAuth;
    this.oauth2Parameters = oauth2Parameters;
  }

  public String getToken() {
    return token.get();
  }

  public AtomicReference<String> getSharedToken() {
    return token;
  }

  public void refreshToken() {
    try {
      switch (authType) {
        case USERNAME_PASSWORD:
          refreshTokenWithUserPass();
          break;
        case SECRET:
          refreshTokenWithSecret();
          break;
        case OAUTH2:
          refreshOAuth2Token();
          break;
        default:
          // Don't support refresh token, throw it directly
          throw new RestppErrorException(
              "Failed to refresh token because none of the username/password, secret or oauth2"
                  + " config is provided");
      }
    } catch (Exception e) {
      throw new RestppErrorException("Failed to refresh token", e);
    }
  }

  private void refreshTokenWithUserPass() {
    AuthResponse resp;
    if (Utils.isEmpty(version) || Utils.versionCmp(version, "4.1.0") >= 0) {
      resp = auth.requestTokenWithUserPass(version, graph, basicAuth, Auth.TOKEN_LIFETIME_SEC);
      resp.panicOnFail();
      token.set(resp.getToken());
    } else {
      // extend existing token
      auth.refreshTokenWithUserPass(token.get(), basicAuth, Auth.TOKEN_LIFETIME_SEC).panicOnFail();
    }
  }

  private void refreshTokenWithSecret() {
    AuthResponse resp;
    if (Utils.isEmpty(version) || Utils.versionCmp(version, "4.1.0") >= 0) {
      resp = auth.requestTokenWithSecret(version, secret, Auth.TOKEN_LIFETIME_SEC);
      resp.panicOnFail();
      token.set(resp.getToken());
    } else {
      // extend existing token
      auth.refreshTokenWithSecrect(token.get(), secret, Auth.TOKEN_LIFETIME_SEC).panicOnFail();
    }
  }

  /** For OAuth2 token, like azure, aws and gcp ... */
  private void refreshOAuth2Token() {
    OAuth2Response resp = auth.refreshOAuth2Token(oauth2Parameters);
    resp.panicOnFail();
    token.set(resp.getToken());
  }
}
