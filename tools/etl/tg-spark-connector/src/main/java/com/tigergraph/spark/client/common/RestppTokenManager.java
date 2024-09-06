package com.tigergraph.spark.client.common;

import com.tigergraph.spark.client.Auth;
import com.tigergraph.spark.client.Auth.AuthResponse;
import com.tigergraph.spark.util.Utils;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manage the RESTPP token lifetime, specifically, refresh it when it expires: - refresh token by
 * secret or username/password - refresh legacy token or JW token(tg 4.1.0)
 */
public class RestppTokenManager {

  private final Auth auth;
  private final String graph;
  private final String version;
  private final AtomicReference<String> token;
  private final String secret;
  private final String basicAuth;

  public RestppTokenManager(
      Auth auth,
      String graph,
      String version,
      AtomicReference<String> token,
      String secret,
      String basicAuth) {
    this.auth = auth;
    this.graph = graph;
    this.version = version;
    this.token = token;
    this.secret = secret;
    this.basicAuth = basicAuth;
  }

  public String getToken() {
    return token.get();
  }

  public AtomicReference<String> getSharedToken() {
    return token;
  }

  public void refreshToken() {
    if (Utils.isEmpty(version) || Utils.versionCmp(version, "4.1.0") >= 0) {
      refreshJWToken();
    } else {
      refreshLegacyToken();
    }
  }

  /**
   * Prior to 4.1.0, the token's lifetime can be extended in-place(no need to request a new one) by
   * PUT /requesttoken, which was deprecated in 4.1.0
   */
  private void refreshLegacyToken() {
    if (!Utils.isEmpty(basicAuth)) {
      auth.refreshTokenWithUserPass(token.get(), basicAuth, Auth.TOKEN_LIFETIME_SEC).panicOnFail();
    } else if (!Utils.isEmpty(secret)) {
      auth.refreshTokenWithSecrect(token.get(), secret, Auth.TOKEN_LIFETIME_SEC).panicOnFail();
    } else {
      // Don't support refresh token, throw it directly
      throw new RestppErrorException(
          "Token expired, failed to refresh token without 'secret' or 'username/password'"
              + " provided.");
    }
  }

  /** For JW token, the only way to refresh it is to **request a new one**.(After 4.1.0) */
  private void refreshJWToken() {
    AuthResponse resp;
    if (!Utils.isEmpty(basicAuth)) {
      resp = auth.requestTokenWithUserPass(version, graph, basicAuth, Auth.TOKEN_LIFETIME_SEC);
    } else if (!Utils.isEmpty(secret)) {
      resp = auth.requestTokenWithSecret(version, secret, Auth.TOKEN_LIFETIME_SEC);
    } else {
      // Don't support refresh token, throw it directly
      throw new RestppErrorException(
          "Token expired, failed to refresh token without 'secret' or 'username/password'"
              + " provided.");
    }
    resp.panicOnFail();
    token.set(resp.getToken());
  }
}
