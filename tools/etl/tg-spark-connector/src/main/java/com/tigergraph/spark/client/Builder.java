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
package com.tigergraph.spark.client;

import com.tigergraph.spark.client.common.RestppDecoder;
import com.tigergraph.spark.client.common.RestppEncoder;
import com.tigergraph.spark.client.common.RestppErrorDecoder;
import com.tigergraph.spark.client.common.RestppRetryer;
import com.tigergraph.spark.client.common.RestppTokenManager;
import com.tigergraph.spark.log.LoggerFactory;
import com.tigergraph.spark.util.Options;
import com.tigergraph.spark.util.Utils;
import feign.*;
import feign.Target.HardCodedTarget;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.hc5.ApacheHttp5Client;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import java.util.stream.Collectors;
import javax.net.ssl.HostnameVerifier;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.spark.SparkFiles;

/** Builder for all client, with custom client settings. */
public class Builder {
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Builder.class);

  private Feign.Builder builder = new Feign.Builder();
  // default client settings
  private HttpClientBuilder hc5builder = HttpClientBuilder.create();
  private PoolingHttpClientConnectionManagerBuilder connMgrBuilder =
      PoolingHttpClientConnectionManagerBuilder.create();
  private Encoder encoder = RestppEncoder.INSTANCE;
  private Decoder decoder = RestppDecoder.INSTANCE;
  private ErrorDecoder errDecoder = new RestppErrorDecoder(RestppDecoder.INSTANCE);
  private Retryer retryer = new Retryer.Default();
  private List<RequestInterceptor> customInterceptors = new ArrayList<>();
  private Request.Options reqOpts = new Request.Options();

  public Builder setRequestOptions(int connectTimeoutMs, int readTimeoutMs) {
    this.reqOpts =
        new Request.Options(
            connectTimeoutMs, TimeUnit.MILLISECONDS, readTimeoutMs, TimeUnit.MILLISECONDS, false);
    return this;
  }

  /** Set response error decoder with the HTTP error codes that will be retried. */
  public Builder setRetryableCode(Integer... code) {
    this.errDecoder = new RestppErrorDecoder(this.decoder, code);
    return this;
  }

  /**
   * Disable auth-related retries (e.g. 401/403) and keep default server-related retryable codes.
   *
   * <p>This is the recommended setting for Auth client used by initial auth probe.
   */
  public Builder disableAuthRetry() {
    if (this.errDecoder instanceof RestppErrorDecoder) {
      this.errDecoder = ((RestppErrorDecoder) this.errDecoder).withoutAuthRetry();
    } else {
      // fallback: keep default server retryable codes and disable auth retry
      this.errDecoder =
          new RestppErrorDecoder(
              this.decoder,
              RestppErrorDecoder.DEFAULT_SERVER_RETRYABLE_CODE,
              Collections.emptyList());
    }
    return this;
  }

  /** Set custom request encoder. */
  public Builder setEncoder(Encoder encoder) {
    this.encoder = encoder;
    return this;
  }

  /** Set custom RESTPP response decoder. */
  public Builder setDecoder(Decoder decoder) {
    this.decoder = decoder;
    return this;
  }

  /** Set retryer for token expiration, io exception and server errors */
  public Builder setRetryer(
      RestppTokenManager tokenMgr,
      int ioPeriod,
      int ioMaxPeriod,
      int ioMaxAttempts,
      int serverPeriod,
      int serverMaxPeriod,
      int serverMaxAttempts) {
    this.retryer =
        new RestppRetryer(
            tokenMgr,
            ioPeriod,
            ioMaxPeriod,
            ioMaxAttempts,
            serverPeriod,
            serverMaxPeriod,
            serverMaxAttempts);
    return this;
  }

  /** Set retryer for io exception and server errors */
  public Builder setRetryerWithoutAuth(
      int ioPeriod,
      int ioMaxPeriod,
      int ioMaxAttempts,
      int serverPeriod,
      int serverMaxPeriod,
      int serverMaxAttempts) {
    this.retryer =
        new RestppRetryer(
            ioPeriod, ioMaxPeriod, ioMaxAttempts, serverPeriod, serverMaxPeriod, serverMaxAttempts);
    return this;
  }

  /** Add a custom request interceptor */
  public Builder addInterceptor(RequestInterceptor interceptor) {
    if (interceptor != null) {
      this.customInterceptors.add(interceptor);
    }
    return this;
  }

  /** Set SSL context for the client */
  public Builder setSSL(
      String mode, String trustStoreFile, String trustStoreType, String password) {
    HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;
    SSLContextBuilder sslContextBuilder = SSLContexts.custom();
    try {
      switch (mode) {
        case Options.SSL_MODE_BASIC:
          sslContextBuilder.loadTrustMaterial(null, new TrustAllStrategy());
          break;
        case Options.SSL_MODE_VERIFY_HOSTNAME:
          hostnameVerifier = new DefaultHostnameVerifier();
        // the security level of hostname verification is higher than
        // CA verification, so need to continue to the next case
        case Options.SSL_MODE_VERIFY_CA:
          if (Utils.isEmpty(trustStoreFile)) {
            throw new IllegalArgumentException("\"ssl.truststore\" is required for mode " + mode);
          }
          String path = SparkFiles.get(trustStoreFile);
          final InputStream in = new FileInputStream(new File(path));
          final KeyStore truststore = KeyStore.getInstance(trustStoreType);
          if (Utils.isEmpty(password)) {
            truststore.load(in, new char[0]);
          } else {
            truststore.load(in, password.toCharArray());
          }
          sslContextBuilder.loadTrustMaterial(truststore, null);
          break;
        default:
          throw new IllegalArgumentException("Invalid SSL mode: " + mode);
      }
      connMgrBuilder.setSSLSocketFactory(
          new SSLConnectionSocketFactory(sslContextBuilder.build(), hostnameVerifier));
    } catch (Exception e) {
      throw new RuntimeException("Failed to configure SSL", e);
    }

    return this;
  }

  public <T> T build(Class<T> apiType, String url) {
    builder
        .encoder(encoder)
        .decoder(decoder)
        .errorDecoder(errDecoder)
        .retryer(retryer)
        .options(reqOpts)
        .client(
            new ApacheHttp5Client(hc5builder.setConnectionManager(connMgrBuilder.build()).build()));
    List<RequestInterceptor> interceptorChain = new ArrayList<>();
    interceptorChain.add(new UAInterceptor());
    interceptorChain.addAll(customInterceptors);
    builder.requestInterceptors(interceptorChain);

    // Required to fetch the iterator after the response is processed, need to be close
    if (Query.class.equals(apiType)) builder.doNotCloseAfterDecode();

    return builder.target(new LoadBalanceTarget<T>(apiType, url));
  }

  // The target that support load balancing
  public static class LoadBalanceTarget<T> extends HardCodedTarget<T> {

    private final List<String> urls;
    private final Random rand = new Random();

    public LoadBalanceTarget(Class<T> type, String url) {
      super(type, url);
      urls =
          Arrays.stream(url.split(","))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .distinct()
              .collect(Collectors.toList());
    }

    // Randomly pick an address to build the HTTP request
    @Override
    public String url() {
      return urls.get(rand.nextInt(urls.size()));
    }

    @Override
    public Request apply(RequestTemplate input) {
      // Randomize URLs on every request, including on retries
      input.target(url());
      return input.request();
    }
  }

  // For audit logging
  public static class UAInterceptor implements RequestInterceptor {
    private static final String PRODUCT;

    static {
      Properties properties = new Properties();
      try {
        InputStream stream =
            UAInterceptor.class.getResourceAsStream(
                "/META-INF/maven/com.tigergraph/tigergraph-spark-connector/pom.properties");
        properties.load(stream);
      } catch (Exception e) {
        // no-op
      }
      String version = properties.getProperty("version", "");
      PRODUCT = "tigergraph-spark-connector/".concat(version);
    }

    @Override
    public void apply(RequestTemplate template) {
      template.header("User-Agent", PRODUCT);
    }
  }
}
