package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.common.Connection;
import com.tigergraph.jdbc.common.DatabaseMetaData;
import com.tigergraph.jdbc.common.PreparedStatement;
import com.tigergraph.jdbc.log.TGLoggerFactory;
import com.tigergraph.jdbc.restpp.driver.QueryParser;
import com.tigergraph.jdbc.restpp.driver.RestppResponse;
import com.tigergraph.jdbc.common.Array;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.spark.SparkFiles;
import org.json.JSONObject;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.sql.SQLException;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import org.slf4j.Logger;

public class RestppConnection extends Connection {

  private static final Logger logger = TGLoggerFactory.getLogger(RestppConnection.class);
  private static final String DEFAULT_TG_VERSION = "3.9.0";

  private String host;
  private Integer port;
  private Boolean closed = Boolean.FALSE;
  private Boolean secure = Boolean.FALSE;
  private String token = null;
  private String username = null;
  private String password = null;
  private String graph = null;
  private String filename = null;
  private String basicAuth = null;
  private String sep = null;
  private String eol = null;
  private String jobid = null;
  private String max_num_error = null;
  private String max_percent_error = null;
  private String limit = null;
  private String source = null;
  private String src_vertex_type = null;
  private Integer atomic = 0;
  private Integer queryTimeout = -1; // the timeout setting for gsql query
  private Integer connectTimeout = 30;
  private Integer socketTimeout = 3600; // 1h by default
  private Integer batchsizeInBytes = 50 * 1024 * 1024; // 50M by default
  private Integer maxRetryCount = 10;
  private Integer level = 1;
  private String[] ipArray = null;
  private ComparableVersion tg_version = new ComparableVersion(DEFAULT_TG_VERSION);

  private CloseableHttpClient httpClient;

  /** Default constructor. */
  public RestppConnection(
      String host, Integer port, Boolean secure, Properties properties, String url)
      throws SQLException {
    super(properties, url);
    this.secure = secure;
    this.host = host;
    this.port = port;
    SSLContext sslContext = null;
    Boolean hasSSLContext = Boolean.FALSE;

    if (null != properties) {
      if (logger.isDebugEnabled()) {
        // Hide password and token
        Properties redacted_properties = new Properties();
        redacted_properties.putAll(properties);
        if (redacted_properties.containsKey("password")) {
          redacted_properties.setProperty("password", "[REDACTED]");
        }
        if (redacted_properties.containsKey("token")) {
          redacted_properties.setProperty("token", "[REDACTED]");
        }
        if (redacted_properties.containsKey("token")) {
          redacted_properties.setProperty("token", "[REDACTED]");
        }
        if (redacted_properties.containsKey("trustStorePassword")) {
          redacted_properties.setProperty("trustStorePassword", "[REDACTED]");
        }
        if (redacted_properties.containsKey("keyStorePassword")) {
          redacted_properties.setProperty("keyStorePassword", "[REDACTED]");
        }
        logger.debug(
            "Properties: {}",
            redacted_properties
                .toString()
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\r", "\\r"));
      }

      // Get restpp version. If not given, use the default version.
      if (properties.containsKey("version")) {
        this.tg_version = new ComparableVersion(properties.getProperty("version"));
      }

      if (properties.containsKey("atomic")) {
        this.atomic = Integer.valueOf(properties.getProperty("atomic"));
      }

      if (properties.containsKey("queryTimeout")) {
        this.queryTimeout = Integer.valueOf(properties.getProperty("queryTimeout"));
      }

      if (properties.containsKey("connectTimeout")) {
        this.connectTimeout = Integer.valueOf(properties.getProperty("connectTimeout"));
      }

      // Get token for authentication.
      if (properties.containsKey("token")) {
        this.token = properties.getProperty("token");
      }

      // Get username
      if (properties.containsKey("username")) {
        this.username = properties.getProperty("username");
      } else if (properties.containsKey("user")) {
        this.username = properties.getProperty("user");
      }

      // Get password
      if (properties.containsKey("password")) {
        this.password = properties.getProperty("password");
      }

      // Get graph name.
      if (properties.containsKey("graph")) {
        this.graph = properties.getProperty("graph");
      }

      // Get filename for loading jobs.
      if (properties.containsKey("filename")) {
        this.filename = properties.getProperty("filename");
      }

      // Get separator between columns (for loading jobs).
      if (properties.containsKey("sep")) {
        this.sep = properties.getProperty("sep");
      }

      // Get eol (i.e., End of Line) for loading jobs.
      if (properties.containsKey("eol")) {
        this.eol = properties.getProperty("eol");
      }

      // Get max batch size for loading job, i.e., max payload size for /ddl request
      if (properties.containsKey("batchsizeInBytes")) {
        this.batchsizeInBytes = Integer.valueOf(properties.getProperty("batchsizeInBytes"));
      }

      // Get max retry count for loading job when hitting timeout or Kafka protection
      if (properties.containsKey("maxRetryCount")) {
        this.maxRetryCount = Integer.valueOf(properties.getProperty("maxRetryCount"));
      }

      // Starting from v3.9.0, TG supports providing loading job statistics based on
      // jobid
      if (properties.containsKey("jobid")
          && this.tg_version.compareTo(new ComparableVersion("3.9.0")) >= 0) {
        this.jobid = properties.getProperty("jobid");
        logger.info("The loading job ID of current connection: {}", this.jobid);
      }

      // Starting from v3.9.0, TG supports terminating loading jobs based on the max
      // number of
      // errors.
      if (properties.containsKey("max_num_error")
          && this.tg_version.compareTo(new ComparableVersion("3.9.0")) >= 0) {
        this.max_num_error = properties.getProperty("max_num_error");
        if (Long.valueOf(max_num_error) < 0) {
          throw new SQLException("Property 'max_num_error' cannot be negative");
        }
      }

      // Starting from v3.9.0, TG supports terminating loading jobs based on the max
      // percentage of
      // errors.
      if (properties.containsKey("max_percent_error")
          && this.tg_version.compareTo(new ComparableVersion("3.9.0")) >= 0) {
        this.max_percent_error = properties.getProperty("max_percent_error");
        if (Long.valueOf(max_percent_error) < 0 || Long.valueOf(max_percent_error) > 100) {
          throw new SQLException("Property 'max_percent_error' should range from 0 to 100");
        }
      }

      // Number of vertices/edges to retrieve.
      if (properties.containsKey("limit")) {
        this.limit = properties.getProperty("limit");
      }

      // Source vertex id for edge to be retrieved.
      if (properties.containsKey("source")) {
        this.source = properties.getProperty("source");
      }

      // Source vertex type for edge to be retrieved.
      if (properties.containsKey("src_vertex_type")) {
        this.src_vertex_type = properties.getProperty("src_vertex_type");
      }

      if (properties.containsKey("ip_list")) {
        String ip_list = properties.getProperty("ip_list");
        String[] tokens = ip_list.trim().split(",");
        List<String> strList = new ArrayList<>();
        for (int i = 0; i < tokens.length; i++) {
          String ip = tokens[i].trim();
          if (ip != null && !ip.equals("")) {
            strList.add(ip);
          }
        }
        this.ipArray = strList.toArray(new String[strList.size()]);
      }

      SSLContextBuilder sslBuilder = SSLContexts.custom();
      String trustStorePassword = "";
      if (properties.containsKey("trustStorePassword")) {
        trustStorePassword = properties.getProperty("trustStorePassword");
      }

      String trustStoreType = "JKS";
      if (properties.containsKey("trustStoreType")) {
        trustStoreType = properties.getProperty("trustStoreType");
      }

      String keyStorePassword = "";
      if (properties.containsKey("keyStorePassword")) {
        keyStorePassword = properties.getProperty("keyStorePassword");
      }

      String keyStoreType = "JKS";
      if (properties.containsKey("keyStoreType")) {
        keyStoreType = properties.getProperty("keyStoreType");
      }

      try {
        if (properties.containsKey("trustStore")) {
          hasSSLContext = Boolean.TRUE;
          String trustFilename = properties.getProperty("trustStore");
          File tempFile = new File(trustFilename);
          if (!tempFile.exists()) {
            try {
              trustFilename = SparkFiles.get(trustFilename);
              logger.debug("SparkFiles: {}", trustFilename);
              // As the spark-core dependency is provided, this exception means the jdbc is
              // not being used with spark, so apparently the path is wrong.
            } catch (NoClassDefFoundError e) {
              logger.error("{} does not exist, please check this path.", trustFilename);
              throw new SQLException(trustFilename + " does not exist, please check this path.");
            }
          }
          final KeyStore truststore = KeyStore.getInstance(trustStoreType);
          try (final InputStream in = new FileInputStream(new File(trustFilename))) {
            truststore.load(in, trustStorePassword.toCharArray());
          }
          // sslBuilder = sslBuilder.loadTrustMaterial(truststore, new
          // TrustSelfSignedStrategy());
          sslBuilder = sslBuilder.loadTrustMaterial(truststore, null);
        }

        if (properties.containsKey("keyStore")) {
          hasSSLContext = Boolean.TRUE;
          String keyFilename = properties.getProperty("keyStore");
          File tempFile = new File(keyFilename);
          if (!tempFile.exists()) {
            try {
              keyFilename = SparkFiles.get(keyFilename);
              logger.debug("SparkFiles: {}", keyFilename);
              // As the spark-core dependency is provided, this exception means the jdbc is
              // not being used with spark, so apparently the path is wrong.
            } catch (NoClassDefFoundError e) {
              logger.error("{} does not exist, please check this path.", keyFilename);
              throw new SQLException(keyFilename + " does not exist, please check this path.");
            }
          }
          final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
          try (final InputStream in = new FileInputStream(new File(keyFilename))) {
            keyStore.load(in, keyStorePassword.toCharArray());
          }
          sslBuilder = sslBuilder.loadKeyMaterial(keyStore, keyStorePassword.toCharArray());
        }

        if (hasSSLContext) {
          sslContext = sslBuilder.build();
        }
      } catch (Exception e) {
        logger.error("Failed to build SSL context", e);
        throw new SQLException(e);
      }

      // If give url jdbc:tg:https:// but don't provide a certificate, connection will
      // fail.
      if (this.secure && !hasSSLContext) {
        logger.error(
            "Build SSL context failed, please provide a self-signed certificate, or use HTTP"
                + " instead."
                + " https://github.com/tigergraph/ecosys/tree/master/tools/etl/tg-jdbc-driver#support-ssl");
        throw new SQLException(
            "Build SSL context failed, please provide a self-signed certificate, or use HTTP"
                + " instead."
                + " https://github.com/tigergraph/ecosys/tree/master/tools/etl/tg-jdbc-driver#support-ssl");
      }
    }

    /**
     * Need to provide username/password when getting schema, even when authentication is not
     * enabled.
     */
    String userCredentials = "tigergraph:tigergraph";
    if (username != null && password != null) {
      userCredentials = username + ":" + password;
    } else {
      logger.warn(
          "No username/password provided, use tigergraph:tigergraph by default. If running schema"
              + " query or interpreted query, please make sure the username/password is correct."
              + " For security purposes, please change the default password");
    }
    this.basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));

    // Create the http client builder.
    HttpClientBuilder builder = HttpClients.custom();
    if (hasSSLContext) {
      this.secure = Boolean.TRUE;
      HostnameVerifier hostnameVerifier =
          properties.getProperty("sslHostnameVerification", "true").equalsIgnoreCase("true")
              ? new DefaultHostnameVerifier()
              : NoopHostnameVerifier.INSTANCE;
      SSLConnectionSocketFactory sslConnectionFactory =
          new SSLConnectionSocketFactory(
              sslContext, new String[] {"TLSv1.2", "TLSv1.1"}, null, hostnameVerifier);
      builder.setSSLSocketFactory(sslConnectionFactory);
      Registry<ConnectionSocketFactory> registry =
          RegistryBuilder.<ConnectionSocketFactory>create()
              .register("https", sslConnectionFactory)
              .register("http", new PlainConnectionSocketFactory())
              .build();
      HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
      builder.setConnectionManager(ccm);
    }
    if (null != properties && properties.containsKey("useragent")) {
      String userAgent = properties.getProperty("useragent");
      builder.setUserAgent(userAgent);
    }
    // Set the timeout for establishing a connection
    // The socketTimeout should be longer then queryTimeout to avoid timeout before
    // query finishes
    this.socketTimeout = Math.max(this.socketTimeout, this.queryTimeout + 5 * 60);
    builder.setDefaultRequestConfig(
        RequestConfig.custom()
            .setConnectTimeout(connectTimeout * 1000)
            .setSocketTimeout(socketTimeout * 1000)
            .build());

    // Http Client retry handler for transport exceptions:
    // - retry 5 times, with 5s backoff
    // - exclude some unretriable exceptions, e.g., connection refused
    // - all requests are considered idempotent
    HttpRequestRetryHandler transportRetryHandler =
        (exception, executionCount, context) -> {
          if (executionCount >= 5) {
            return false;
          }
          if (exception instanceof InterruptedIOException
              && !(exception instanceof SocketTimeoutException)
              && !(exception instanceof ConnectTimeoutException)) {
            // Connection interrupted except for connect/read timeout
            return false;
          }
          if (exception instanceof ConnectException) {
            // Connection refused
            return false;
          }
          if (exception instanceof UnknownHostException) {
            // Unknown host
            return false;
          }
          if (exception instanceof SSLException) {
            // SSL handshake exception
            return false;
          }
          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          logger.debug("Retry for exception: {}, retry count: {}", exception, executionCount);
          return true;
        };

    // Http Client retry handler for protocol exceptions:
    // - retry 10 times, with exponential backoffs
    // - status code 408, 500, 502, 503, 504 are retriable
    // - all requests are considered idempotent
    ServiceUnavailableRetryStrategy protocolRetryStrategy =
        new ServiceUnavailableRetryStrategy() {
          private long retryInterval = 5000; // 5s
          private int count = 1;
          private String cause;
          private static final long maxRetryInterval = 10 * 60 * 1000; // 10min
          private final List<Integer> retriableCode = Arrays.asList(408, 500, 502, 503, 504);

          @Override
          public boolean retryRequest(
              HttpResponse response, int executionCount, HttpContext context) {
            count = executionCount;
            cause =
                response.getStatusLine().getStatusCode()
                    + " - "
                    + response.getStatusLine().getReasonPhrase();
            return retriableCode.contains(response.getStatusLine().getStatusCode())
                && executionCount <= maxRetryCount;
          }

          @Override
          public long getRetryInterval() {
            long interval = retryInterval + jitter(retryInterval);
            retryInterval = Math.min(retryInterval * 2, maxRetryInterval);
            logger.error(
                "Retrying for the failed request: {}. Retry count: {}, retry interval: {}"
                    + " milliseconds",
                cause,
                count,
                interval);
            return interval;
          }

          private long jitter(long i) {
            return (long) Math.round(i * Math.random() / 5);
          }
        };
    builder.setRetryHandler(transportRetryHandler);
    builder.setServiceUnavailableRetryStrategy(protocolRetryStrategy);
    this.httpClient = builder.build();

    if (this.token == null
        && this.username != null
        && this.password != null
        && this.graph != null) {
      getToken();
    } else if (this.token == null && this.graph == null) {
      logger.warn("No graph name provided, unable to get token");
    }
  }

  public String getBasicAuth() {
    return this.basicAuth;
  }

  public String getSeparator() {
    return this.sep;
  }

  public String getEol() {
    return this.eol;
  }

  public Integer getBatchsizeInBytes() {
    return this.batchsizeInBytes;
  }

  public String getLimit() {
    return this.limit;
  }

  public String getJobId() {
    return this.jobid;
  }

  public String getFilename() {
    return this.filename;
  }

  public String getMaxNumError() {
    return this.max_num_error;
  }

  public String getMaxPercentError() {
    return this.max_percent_error;
  }

  public String getSource() {
    return this.source;
  }

  public String getSrcVertexType() {
    return this.src_vertex_type;
  }

  public RestppResponse executeQueries(final List<QueryParser> queries) throws SQLException {
    RestppResponse result = null;

    if (queries.size() < 1) {
      logger.error("No query specified");
      throw new SQLException("No query specified.");
    }

    result = executeQuery(queries.get(0), "");

    /** Execute the queries one by one, and append their results to the firt RestppResponse. */
    for (int i = 1; i < queries.size(); i++) {
      RestppResponse newResult = executeQuery(queries.get(i), "");
      result.addResults(newResult.getResults());
    }

    return result;
  }

  /**
   * Get token. e.g., curl --user tigergraph:tigergraph -X POST \
   * 'http://localhost:14240/restpp/requesttoken' -d '{"graph": "example_graph"}'
   */
  private void getToken() throws SQLException {
    StringBuilder urlSb = new StringBuilder();
    // If restpp version is under 3.5, pass graph name as a parameter
    if (this.tg_version.compareTo(new ComparableVersion("3.5.0")) < 0) {
      // /gsqlserver/gsql/authtoken is deprecated, it won't return error messages when
      // password is wrong
      urlSb.append("/gsqlserver/gsql/authtoken");
      urlSb.append("?graph=");
      urlSb.append(this.graph);
    } else {
      urlSb.append("/restpp/requesttoken");
    }
    String url = "";
    try {
      if (this.secure) url = new URL("https", host, port, urlSb.toString()).toString();
      else url = new URL("http", host, port, urlSb.toString()).toString();
    } catch (MalformedURLException e) {
      logger.error("Invalid server URL", e);
      throw new SQLException("Invalid server URL", e);
    }

    HttpPost request = new HttpPost(url);
    request.addHeader("Authorization", basicAuth);
    request.addHeader("Accept", ContentType.APPLICATION_JSON.toString());

    // If restpp version is no less than 3.5, pass graph name as payload
    if (this.tg_version.compareTo(new ComparableVersion("3.5.0")) >= 0) {
      StringBuilder payloadSb = new StringBuilder();
      payloadSb.append("{\"graph\":\"");
      payloadSb.append(this.graph);
      payloadSb.append("\"}");
      StringEntity payload = new StringEntity(payloadSb.toString(), "UTF-8");
      payload.setContentType("application/json");
      request.setEntity(payload);
    }
    /**
     * Response example:
     * {"error":false,"message":"","results":{"token":"5r6scnj83963gnfjqtvico1hf2hn394o"}}
     */
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      /**
       * When authentication is turned off, the token request will fail. In this case, just do not
       * use token and print warning instead of panic.
       */
      RestppResponse result = new RestppResponse(response, Boolean.FALSE);
      if (result.hasError() != null && result.hasError()) {
        if (result.getErrCode() != null && result.getErrCode().equals("REST-1000")) {
          logger.warn(
              "RESTPP authentication is not enabled:"
                  + " https://docs.tigergraph.com/tigergraph-server/current/user-access/enabling-user-authentication#_enable_restpp_authentication");
        } else {
          logger.error(
              "Failed to get token: {} The following requests may fail.", result.getErrMsg());
        }
      } else {
        List<JSONObject> jsonList = result.getResults();
        for (int i = 0; i < jsonList.size(); i++) {
          JSONObject obj = jsonList.get(i);
          if (obj.has("token")) {
            this.token = obj.getString("token");
            logger.debug("Got token: [REDACTED]");
            return;
          }
        }
      }
    } catch (Exception e) {
      logger.error("Failed to get token", e);
      throw new SQLException("Failed to get token", e);
    }
  }

  public RestppResponse executeQuery(QueryParser parser, String json) throws SQLException {
    String host = this.host;
    // Load balancing
    if (this.ipArray != null && this.ipArray.length > 1) {
      Random rand = new Random();
      int index = rand.nextInt(this.ipArray.length);
      host = this.ipArray[index];
    }
    HttpRequestBase request =
        parser.buildQuery(
            host,
            port,
            secure,
            graph,
            token,
            json,
            filename,
            sep,
            eol,
            jobid,
            max_num_error,
            max_percent_error);
    logger.debug("Send request: {}", request.toString());
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      return new RestppResponse(response, Boolean.TRUE);
    } catch (Exception e) {
      logger.error(
          "Failed to execute query: request: " + request + ", payload size: " + json.length(), e);
      throw new SQLException(
          "Failed to execute query: request: " + request + ", payload size: " + json.length(), e);
    }
  }

  /**
   * Create an Array object which can be used with <code>
   * public void setArray(int parameterIndex, Array val)</code>
   *
   * @param typeName the SQL name of the type the elements of the array map to. Should be one of
   *     SHORT,INTEGER,BYTE,LONG,DOUBLE,FLOAT,BOOLEAN,TIMESTAMP,DATE,DECIMAL,BINARY,ANY.
   */
  @Override
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    return new RestppArray(typeName, elements);
  }

  @Override
  public boolean isClosed() throws SQLException {
    return closed;
  }

  @Override
  public void close() throws SQLException {
    closed = Boolean.TRUE;
    try {
      httpClient.close();
    } catch (IOException e) {
      throw new SQLException("Failed to close the http client", e);
    }
  }

  @Override
  public PreparedStatement prepareStatement(String query) throws SQLException {
    return new RestppPreparedStatement(
        this, query, this.queryTimeout, this.atomic, this.tg_version);
  }

  @Override
  public PreparedStatement prepareStatement(
      String query, int resultSetType, int resultSetConcurrency) throws SQLException {
    return new RestppPreparedStatement(
        this, query, this.queryTimeout, this.atomic, this.tg_version);
  }

  @Override
  public PreparedStatement prepareStatement(
      String query, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLException {
    return new RestppPreparedStatement(
        this, query, this.queryTimeout, this.atomic, this.tg_version);
  }

  @Override
  public DatabaseMetaData getMetaData() throws SQLException {
    return new DatabaseMetaData(this);
  }

  @Override
  public void setAutoCommit(boolean autoCommit) throws SQLException {
    return;
  }

  @Override
  public boolean getAutoCommit() throws SQLException {
    return Boolean.TRUE;
  }

  @Override
  public java.sql.Statement createStatement() throws SQLException {
    return new RestppStatement(this, this.queryTimeout, this.atomic);
  }

  @Override
  public void commit() throws SQLException {
    // Update on TigerGraph is autoCommit, nothing to do.
    return;
  }

  @Override
  public void rollback() throws SQLException {
    return;
  }

  @Override
  public void setTransactionIsolation(int level) throws SQLException {
    this.level = level;
    return;
  }

  @Override
  public int getTransactionIsolation() throws SQLException {
    return this.level;
  }

  @Override
  public boolean isValid(int timeout) throws SQLException {
    return Boolean.TRUE;
  }

  /** Methods not implemented yet. */
  @Override
  public void setHoldability(int holdability) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public int getHoldability() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency)
      throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public java.sql.Statement createStatement(
      int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }
}
