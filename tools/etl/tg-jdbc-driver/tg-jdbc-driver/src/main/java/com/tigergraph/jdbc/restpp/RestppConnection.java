package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.common.Connection;
import com.tigergraph.jdbc.common.DatabaseMetaData;
import com.tigergraph.jdbc.common.PreparedStatement;
import com.tigergraph.jdbc.log.TGLoggerFactory;
import com.tigergraph.jdbc.restpp.driver.QueryParser;
import com.tigergraph.jdbc.restpp.driver.RestppResponse;
import com.tigergraph.jdbc.common.Array;
import org.apache.maven.artifact.versioning.ComparableVersion;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
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
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.spark.SparkFiles;
import org.json.JSONObject;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.security.KeyManagementException;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Random;
import org.slf4j.Logger;

public class RestppConnection extends Connection {

  private static final Logger logger = TGLoggerFactory.getLogger(RestppConnection.class);
  private static final int SECOND = 1000;

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
  private String limit = null;
  private String source = null;
  private String lineSchema = null;
  private String src_vertex_type = null;
  private Integer atomic = 0;
  private Integer timeout = -1; // the timeout setting for gsql query
  private Integer connectTimeout = 5 * SECOND; // the timeout setting for establishing an http connection(5s)
  private Integer level = 1;
  private String[] ipArray = null;
  private ComparableVersion restpp_version = new ComparableVersion("3.5.0");

  private CloseableHttpClient httpClient;

  /**
   * Default constructor.
   */
  public RestppConnection(String host, Integer port, Boolean secure,
      Properties properties, String url) throws SQLException {
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
        logger.debug("Properties: {}",
            redacted_properties.toString().replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r"));
      }

      if (properties.containsKey("atomic")) {
        this.atomic = Integer.valueOf(properties.getProperty("atomic"));
      }

      if (properties.containsKey("timeout")) {
        this.timeout = Integer.valueOf(properties.getProperty("timeout"));
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

      // Get line schema (i.e., column definitions) for loading jobs.
      if (properties.containsKey("schema")) {
        this.lineSchema = properties.getProperty("schema");
      }

      // Get restpp version, default version is 3.5
      if (properties.containsKey("version")) {
        this.restpp_version = new ComparableVersion(properties.getProperty("version"));
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
      } catch (MalformedURLException e) {
        logger.error("Failed to build SSL context", e);
        throw new SQLException(e);
      } catch (IOException e) {
        logger.error("Failed to build SSL context", e);
        throw new SQLException(e);
      } catch (NoSuchAlgorithmException e) {
        logger.error("Failed to build SSL context", e);
        throw new SQLException(e);
      } catch (KeyStoreException e) {
        logger.error("Failed to build SSL context", e);
        throw new SQLException(e);
      } catch (CertificateException e) {
        logger.error("Failed to build SSL context", e);
        throw new SQLException(e);
      } catch (UnrecoverableKeyException e) {
        logger.error("Failed to build SSL context", e);
        throw new SQLException(e);
      } catch (KeyManagementException e) {
        logger.error("Failed to build SSL context", e);
        throw new SQLException(e);
      }

      // If give url jdbc:tg:https:// but don't provide a certificate, connection will
      // fail.
      if (this.secure && !hasSSLContext) {
        logger.error(
            "Build SSL context failed, please provide a self-signed certificate, or use HTTP instead. https://github.com/tigergraph/ecosys/tree/master/tools/etl/tg-jdbc-driver#support-ssl");
        throw new SQLException(
            "Build SSL context failed, please provide a self-signed certificate, or use HTTP instead. https://github.com/tigergraph/ecosys/tree/master/tools/etl/tg-jdbc-driver#support-ssl");
      }
    }

    /**
     * Need to provide username/password when getting schema,
     * even when authentication is not enabled.
     */
    String userCredentials = "tigergraph:tigergraph";
    if (username != null && password != null) {
      userCredentials = username + ":" + password;
    } else {
      logger.warn(
          "No username/password provided, use tigergraph:tigergraph by default. To get the schema and run the interpreted query, please make sure the password is correct. For security purposes, please change the default password");
    }
    this.basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));

    // Create the http client builder.
    HttpClientBuilder builder = HttpClients.custom();
    if (hasSSLContext) {
      this.secure = Boolean.TRUE;
      HostnameVerifier hostnameVerifier = properties.getProperty("sslHostnameVerification", "true")
          .equalsIgnoreCase("true")
              ? new DefaultHostnameVerifier()
              : NoopHostnameVerifier.INSTANCE;
      SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext,
          new String[] { "TLSv1.2", "TLSv1.1" },
          null,
          hostnameVerifier);
      builder.setSSLSocketFactory(sslConnectionFactory);
      Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
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
    builder.setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(connectTimeout).build());
    this.httpClient = builder.build();

    checkConnectivity();

    if (this.token == null && this.username != null && this.password != null && this.graph != null) {
      getToken();
    } else if (this.token == null && this.graph == null) {
      logger.warn("No graph name provided, unable to get token");
    }
  }

  public String getBasicAuth() {
    return this.basicAuth;
  }

  public String getLineSchema() {
    return this.lineSchema;
  }

  public String getSeparator() {
    return this.sep;
  }

  public String getEol() {
    return this.eol;
  }

  public String getLimit() {
    return this.limit;
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

    /**
     * Execute the queries one by one,
     * and append their results to the firt RestppResponse.
     */
    for (int i = 1; i < queries.size(); i++) {
      RestppResponse newResult = executeQuery(queries.get(i), "");
      result.addResults(newResult.getResults());
    }

    return result;
  }

  /**
   * Get token. e.g.,
   * curl --user tigergraph:tigergraph -X POST \
   * 'http://localhost:14240/restpp/requesttoken' -d '{"graph": "example_graph"}'
   */
  private void getToken() throws SQLException {
    StringBuilder urlSb = new StringBuilder();
    // If restpp version is under 3.5, pass graph name as a parameter
    if (this.restpp_version.compareTo(new ComparableVersion("3.5.0")) < 0) {
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
      if (this.secure)
        url = new URL("https", host, port, urlSb.toString()).toString();
      else
        url = new URL("http", host, port, urlSb.toString()).toString();
    } catch (MalformedURLException e) {
      logger.error("Invalid server URL", e);
      throw new SQLException("Invalid server URL", e);
    }

    HttpPost request = new HttpPost(url);
    request.addHeader("Authorization", basicAuth);
    request.addHeader("Accept", ContentType.APPLICATION_JSON.toString());

    // If restpp version is no less than 3.5, pass graph name as payload
    if (this.restpp_version.compareTo(new ComparableVersion("3.5.0")) >= 0) {
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
       * When authentication is turned off, the token request will fail.
       * In this case, just do not use token and print warning instead of panic.
       */
      RestppResponse result = new RestppResponse(response, Boolean.FALSE);
      if (result.hasError() != null && result.hasError()) {
        if (result.getErrCode() != null && result.getErrCode().equals("REST-1000")) {
          logger.warn(
              "RESTPP authentication is not enabled: https://docs.tigergraph.com/tigergraph-server/current/user-access/enabling-user-authentication#_enable_restpp_authentication");
        } else {
          logger.warn("Failed to get token: {}", result.getErrMsg());
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

  public RestppResponse executeQuery(QueryParser parser,
      String json) throws SQLException {
    RestppResponse result = null;
    Integer retry = 0;
    Integer max_retry = 10;
    for (retry = 0; retry < max_retry; ++retry) {
      String host = this.host;
      // Load balancing
      if (this.ipArray != null && this.ipArray.length > 1) {
        Random rand = new Random();
        int index = rand.nextInt(this.ipArray.length);
        host = this.ipArray[index];
      }
      HttpRequestBase request = parser.buildQuery(host, port, secure,
          graph, token, json, filename, sep, eol);
      try (CloseableHttpResponse response = httpClient.execute(request)) {
        result = new RestppResponse(response, Boolean.TRUE);
        break;
      } catch (Exception e) {
        if (retry >= max_retry - 1) {
          logger.error("Failed to execute query: request: " + request +
              ", payload size: " + json.length(), e);
          throw new SQLException("Failed to execute query: request: " + request +
              ", payload size: " + json.length(), e);
        } else {
          // Exponential Backoff
          logger.info("Retrying in {} seconds......", (long) (Math.pow(2, retry) * 5));
          try {
            Thread.sleep((long) (Math.pow(2, retry) * 5 * SECOND));
          } catch (InterruptedException ex) {
            // Nothing to do
          }
        }
      }
    }
    if (retry > 0) {
      logger.debug("Rest request succeeded after {} times retry.", retry);
    }
    return result;
  }

  /**
   * Check the connectivity to TG server before any requests.
   *
   * @throws SQLException if jks certificate is invalid
   * @throws SQLException if use http while TG server has SSL enabled
   * @throws SQLException if use https while TG server has SSL disabled
   * @throws SQLException if connection fail (timeout, wrong ip address, wrong
   *                      port, firewall, etc.)
   *
   *
   */
  private void checkConnectivity() throws SQLException {
    String healthCheckEndpoint = "/api/ping";
    String url = "";
    try {
      if (this.secure)
        url = new URL("https", host, port, healthCheckEndpoint).toString();
      else
        url = new URL("http", host, port, healthCheckEndpoint).toString();
    } catch (MalformedURLException e) {
      logger.error("Invalid server URL", e);
      throw new SQLException("Invalid server URL", e);
    }
    HttpRequestBase request = new HttpGet(url);
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      RestppResponse result = new RestppResponse(response, Boolean.FALSE);

      if (result.hasError() != null && result.hasError()) {
        throw new SQLException("Error response message: " + result.getErrMsg());
      }

      // No error code or error msgs provided here
      if (result.getContent().contains("The plain HTTP request was sent to HTTPS port")) {
        throw new SQLException(
            "Tigergraph server has SSL enabled, please use HTTPS instead. https://github.com/tigergraph/ecosys/tree/master/tools/etl/tg-jdbc-driver#support-ssl");
      }

    } catch (SSLHandshakeException e) {
      logger.error("Invalid TigerGraph server certificate", e);
      throw new SQLException("Invalid TigerGraph server certificate", e);
    } catch (SSLException e) {
      logger.error("Build SSL connection failed", e);
      throw new SQLException("Build SSL connection failed", e);
    } catch (Exception e) {
      logger.error("Failed to connect to TigerGraph server", e);
      throw new SQLException("Failed to connect to TigerGraph server", e);
    }
  }

  /**
   * Create an Array object which can be used with
   * <code>public void setArray(int parameterIndex, Array val)</code>
   *
   * @param typeName the SQL name of the type the elements of the array
   *                 map to. Should be one of
   *                 SHORT,INTEGER,BYTE,LONG,DOUBLE,FLOAT,BOOLEAN,TIMESTAMP,DATE,DECIMAL,BINARY,ANY.
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
    return new RestppPreparedStatement(this, query, this.timeout, this.atomic);
  }

  @Override
  public PreparedStatement prepareStatement(String query,
      int resultSetType, int resultSetConcurrency) throws SQLException {
    return new RestppPreparedStatement(this, query, this.timeout, this.atomic);
  }

  @Override
  public PreparedStatement prepareStatement(String query,
      int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLException {
    return new RestppPreparedStatement(this, query, this.timeout, this.atomic);
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
    return new RestppStatement(this, this.timeout, this.atomic);
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

  /**
   * Methods not implemented yet.
   */

  @Override
  public void setHoldability(int holdability) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public int getHoldability() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public java.sql.Statement createStatement(int resultSetType,
      int resultSetConcurrency) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public java.sql.Statement createStatement(int resultSetType,
      int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

}
