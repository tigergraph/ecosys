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
package com.tigergraph.spark;

import java.io.Serializable;
import java.time.Instant;
import java.util.Base64;
import com.tigergraph.spark.client.Builder;
import com.tigergraph.spark.client.Auth;
import com.tigergraph.spark.client.Misc;
import com.tigergraph.spark.client.Query;
import com.tigergraph.spark.client.Write;
import com.tigergraph.spark.client.Auth.AuthResponse;
import com.tigergraph.spark.client.common.RestppResponse;
import com.tigergraph.spark.client.common.RestppStreamDecoder;
import com.tigergraph.spark.util.Options;
import com.tigergraph.spark.util.Utils;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initalize TG connection including: <br>
 * 1. init authentication; <br>
 * 2. init the clients needed for corresponding operations.
 *
 * <p>Note, it is not a real DB connection, no network connection will be cached.
 *
 * <p>This connection will be inited in driver, then be serialized and sent to executors. Transient
 * variables will be rebuilt in executors.
 */
public class TigerGraphConnection implements Serializable {
  private static final Logger logger = LoggerFactory.getLogger(TigerGraphConnection.class);

  private Options opts;
  // Common connection variables
  private final String graph;
  private final String url;
  private final long creationTime;
  private String version;
  private transient Misc misc;
  // Authentication variables
  private String basicAuth;
  private String secret;
  private String token;
  private boolean restAuthEnabled;
  private boolean restAuthInited;
  private transient Auth auth;
  // Loading job variables/consts
  // spark job type is supported for [3.10.0,), [3.9.4,)
  static final String JOB_IDENTIFIER = "spark";
  static final String JOB_MACHINE = "all";
  private String loadingJobId = null;
  private transient Write write;
  // Query variables
  private transient Query query;
  private static final int DEFAULT_QUERY_READ_TIMEOUT_MS = 1800000; // 30 min

  /**
   * Only be called in driver, serialized and sent to executors. <br>
   * 1. build http client, set SSLSocketFactory if SSL enbled <br>
   * 2. based on 1, build {@link Auth} client <br>
   * 3. based on 2, detect if auth is enabled and request token if not given <br>
   * 4. based on 3, we can build requestInterceptor(add auth header) and retryer(refresh token) for
   * other clients <br>
   * 5. init for specific operations, e.g., loading job id
   *
   * @param opts
   */
  public TigerGraphConnection(Options opts, long creationTime) {
    this.opts = opts;
    this.creationTime = creationTime;
    graph = opts.getString(Options.GRAPH);
    url = opts.getString(Options.URL);
    initAuth();
    // get TG version
    version = opts.getString(Options.VERSION);
    if (Utils.isEmpty(version)) {
      RestppResponse verResp = getMisc().version();
      verResp.panicOnFail();
      version = Utils.extractVersion(verResp.message);
    }
    if (Utils.versionCmp(version, "3.6.0") <= 0) {
      throw new UnsupportedOperationException(
          "TigerGraph version under 3.6.0 is unsupported, current version: " + version);
    }
    logger.info("TigerGraph version: {}", version);

    if (Options.OptionType.WRITE.equals(opts.getOptionType())
        && Utils.versionCmp(version, "3.9.4") >= 0) {
      loadingJobId = generateJobId(graph, opts.getString(Options.LOADING_JOB), creationTime);
    }
  }

  public TigerGraphConnection(Options opts) {
    this(opts, Instant.now().toEpochMilli());
  }

  private void initAuth() {
    if (!restAuthInited) {
      this.secret = opts.getString(Options.SECRET);
      this.token = opts.getString(Options.TOKEN);
      // 1. encode username:password to basic auth
      if (!Utils.isEmpty(opts.getString(Options.USERNAME))
          && !Utils.isEmpty(opts.getString(Options.PASSWORD))) {
        this.basicAuth =
            new String(
                Base64.getEncoder()
                    .encode(
                        (opts.getString(Options.USERNAME) + ":" + opts.getString(Options.PASSWORD))
                            .getBytes()));
      }
      // 2. init Auth client
      getAuth();
      // 3. check if restpp auth is enabled
      restAuthEnabled = true;
      try {
        auth.checkAuthEnabled();
      } catch (FeignException e) {
        if (e.status() == 404) {
          restAuthEnabled = false;
          logger.warn(
              "RESTPP authentication is not enabled, you can enable it via `gadmin config set"
                  + " RESTPP.Factory.EnableAuth true`");
        } else {
          throw e;
        }
      }
      // 4. request token if username/password or secret is provided but token is empty
      if (restAuthEnabled && Utils.isEmpty(token)) {
        AuthResponse resp;
        if (!Utils.isEmpty(basicAuth)) {
          resp = auth.requestTokenWithUserPass(graph, basicAuth, Auth.TOKEN_LIFETIME_SEC);
          resp.panicOnFail();
          token = resp.results.get("token").asText();
        } else if (!Utils.isEmpty(secret)) {
          resp = auth.requestTokenWithSecret(secret, Auth.TOKEN_LIFETIME_SEC);
          resp.panicOnFail();
          token = resp.token;
        } else {
          throw new IllegalArgumentException(
              "Restpp authentication is enabled, please provide at least one of the 'token',"
                  + " 'secret' or 'username/password' pair.");
        }
        logger.info(
            "Requested new token {} for RESTPP authentication, expiration: {}",
            Utils.maskString(token, 2),
            resp.expiration);
      }
      restAuthInited = true;
    }
  }

  /** Get auth client for requesting/refreshing token */
  private Auth getAuth() {
    if (auth == null) {
      Builder builder =
          new Builder()
              .setRequestOptions(
                  opts.getInt(Options.IO_CONNECT_TIMEOUT_MS),
                  opts.getInt(Options.IO_READ_TIMEOUT_MS))
              .setRetryerWithoutAuth(
                  opts.getInt(Options.IO_RETRY_INTERVAL_MS),
                  opts.getInt(Options.IO_MAX_RETRY_INTERVAL_MS),
                  opts.getInt(Options.IO_MAX_RETRY_ATTEMPTS),
                  opts.getInt(Options.IO_RETRY_INTERVAL_MS),
                  opts.getInt(Options.IO_MAX_RETRY_INTERVAL_MS),
                  opts.getInt(Options.IO_MAX_RETRY_ATTEMPTS));
      if (url.trim().toLowerCase().startsWith("https://")) {
        builder.setSSL(
            opts.getString(Options.SSL_MODE),
            opts.getString(Options.SSL_TRUSTSTORE),
            opts.getString(Options.SSL_TRUSTSTORE_TYPE),
            opts.getString(Options.SSL_TRUSTSTORE_PASSWORD));
      }
      auth = builder.build(Auth.class, url);
    }
    return auth;
  }

  public Misc getMisc() {
    if (!restAuthInited) {
      initAuth();
    }

    if (misc == null) {
      Builder builder =
          new Builder()
              .setRequestOptions(
                  opts.getInt(Options.IO_CONNECT_TIMEOUT_MS),
                  opts.getInt(Options.IO_READ_TIMEOUT_MS))
              .setRetryer(
                  getAuth(),
                  basicAuth,
                  secret,
                  token,
                  opts.getInt(Options.IO_RETRY_INTERVAL_MS),
                  opts.getInt(Options.IO_MAX_RETRY_INTERVAL_MS),
                  opts.getInt(Options.IO_MAX_RETRY_ATTEMPTS),
                  opts.getInt(Options.IO_RETRY_INTERVAL_MS),
                  opts.getInt(Options.IO_MAX_RETRY_INTERVAL_MS),
                  opts.getInt(Options.IO_MAX_RETRY_ATTEMPTS))
              .setAuthInterceptor(basicAuth, token, restAuthEnabled);
      if (url.trim().toLowerCase().startsWith("https://")) {
        builder.setSSL(
            opts.getString(Options.SSL_MODE),
            opts.getString(Options.SSL_TRUSTSTORE),
            opts.getString(Options.SSL_TRUSTSTORE_TYPE),
            opts.getString(Options.SSL_TRUSTSTORE_PASSWORD));
      }
      misc = builder.build(Misc.class, url);
    }
    return misc;
  }

  /** Get write client (/restpp/ddl) */
  public Write getWrite() {
    if (!Options.OptionType.WRITE.equals(opts.getOptionType())) {
      throw new UnsupportedOperationException(
          "Can't build write client for OptionType " + opts.getOptionType());
    }

    if (!restAuthInited) {
      initAuth();
    }

    if (write == null) {
      Builder builder =
          new Builder()
              .setRequestOptions(
                  opts.getInt(Options.IO_CONNECT_TIMEOUT_MS),
                  opts.getInt(Options.IO_READ_TIMEOUT_MS))
              .setRetryer(
                  getAuth(),
                  basicAuth,
                  secret,
                  token,
                  opts.getInt(Options.IO_RETRY_INTERVAL_MS),
                  opts.getInt(Options.IO_MAX_RETRY_INTERVAL_MS),
                  opts.getInt(Options.IO_MAX_RETRY_ATTEMPTS),
                  opts.getInt(Options.LOADING_RETRY_INTERVAL_MS),
                  opts.getInt(Options.LOADING_MAX_RETRY_INTERVAL_MS),
                  opts.getInt(Options.LOADING_MAX_RETRY_ATTEMPTS))
              .setAuthInterceptor(basicAuth, token, restAuthEnabled);
      if (url.trim().toLowerCase().startsWith("https://")) {
        builder.setSSL(
            opts.getString(Options.SSL_MODE),
            opts.getString(Options.SSL_TRUSTSTORE),
            opts.getString(Options.SSL_TRUSTSTORE_TYPE),
            opts.getString(Options.SSL_TRUSTSTORE_PASSWORD));
      }
      write = builder.build(Write.class, url);
    }
    return write;
  }

  /** Get query client (restpp built-in queries) */
  public Query getQuery() {
    if (!Options.OptionType.READ.equals(opts.getOptionType())) {
      throw new UnsupportedOperationException(
          "Can't build query client for OptionType " + opts.getOptionType());
    }

    if (!restAuthInited) {
      initAuth();
    }

    if (query == null) {
      int readTimeout =
          Math.max(DEFAULT_QUERY_READ_TIMEOUT_MS, opts.getInt(Options.IO_READ_TIMEOUT_MS));
      // The read timeout should be a bit longer(5 min) than the GSQL query timeout;
      if (opts.containsOption(Options.QUERY_TIMEOUT_MS)) {
        readTimeout = Math.max(readTimeout, opts.getInt(Options.QUERY_TIMEOUT_MS) + 300000);
      }
      Builder builder =
          new Builder()
              .setDecoder(new RestppStreamDecoder())
              .setRequestOptions(opts.getInt(Options.IO_CONNECT_TIMEOUT_MS), readTimeout)
              .setRetryer(
                  getAuth(),
                  basicAuth,
                  secret,
                  token,
                  opts.getInt(Options.IO_RETRY_INTERVAL_MS),
                  opts.getInt(Options.IO_MAX_RETRY_INTERVAL_MS),
                  opts.getInt(Options.IO_MAX_RETRY_ATTEMPTS),
                  opts.getInt(Options.IO_RETRY_INTERVAL_MS),
                  opts.getInt(Options.IO_MAX_RETRY_INTERVAL_MS),
                  opts.getInt(Options.IO_MAX_RETRY_ATTEMPTS))
              .setAuthInterceptor(basicAuth, token, restAuthEnabled)
              .setRetryableCode(502, 503, 504)
              .setQueryInterceptor(
                  opts.getInt(Options.QUERY_TIMEOUT_MS),
                  opts.getLong(Options.QUERY_MAX_RESPONSE_BYTES));
      if (url.trim().toLowerCase().startsWith("https://")) {
        builder.setSSL(
            opts.getString(Options.SSL_MODE),
            opts.getString(Options.SSL_TRUSTSTORE),
            opts.getString(Options.SSL_TRUSTSTORE_TYPE),
            opts.getString(Options.SSL_TRUSTSTORE_PASSWORD));
      }
      query = builder.build(Query.class, url);
    }
    return query;
  }

  /**
   * Generate loading job id: <br>
   * <graph_name>.<job_name>.file.all.<epoch_timestamp>
   *
   * @param graph the graph name
   * @param job the loading job name
   */
  protected static String generateJobId(String graph, String jobname, long creationTime) {
    return new StringBuilder()
        .append(graph)
        .append(".")
        .append(jobname)
        .append(".")
        .append(JOB_IDENTIFIER)
        .append(".")
        .append(JOB_MACHINE)
        .append(".")
        .append(creationTime)
        .toString();
  }

  public Options getOpts() {
    return this.opts;
  }

  public String getVersion() {
    return this.version;
  }

  public String getLoadingJobId() {
    return this.loadingJobId;
  }

  public String getGraph() {
    return this.graph;
  }
}
