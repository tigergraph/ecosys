package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.common.Connection;
import com.tigergraph.jdbc.common.DatabaseMetaData;
import com.tigergraph.jdbc.common.PreparedStatement;
import com.tigergraph.jdbc.restpp.driver.QueryParser;
import com.tigergraph.jdbc.restpp.driver.RestppResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.spark.SparkFiles;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.*;

public class RestppConnection extends Connection {

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
  private Integer debug = 0;
  private Integer atomic = 0;
  private Integer timeout = -1;
  private Integer level = 1;
  private String[] ipArray = null;

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

      // Get debugging mode.
      if (properties.containsKey("debug")) {
        this.debug = Integer.valueOf(properties.getProperty("debug"));
      }

      if (this.debug > 1) {
        System.out.println(">>> properties: " + properties);
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
            trustFilename = SparkFiles.get(trustFilename);
            if (this.debug > 0) {
              System.out.println(">>> SparkFiles: " + trustFilename);
            }
          }
          final KeyStore truststore = KeyStore.getInstance(trustStoreType);
          try (final InputStream in = new FileInputStream(new File(trustFilename))) {
            truststore.load(in, trustStorePassword.toCharArray());
          }
          sslBuilder = sslBuilder.loadTrustMaterial(truststore, new TrustSelfSignedStrategy());
        }

        if (properties.containsKey("keyStore")) {
          hasSSLContext = Boolean.TRUE;
          String keyFilename = properties.getProperty("keyStore");
          File tempFile = new File(keyFilename);
          if (!tempFile.exists()) {
            keyFilename = SparkFiles.get(keyFilename);
            if (this.debug > 0) {
              System.out.println(">>> SparkFiles: " + keyFilename);
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
        throw new SQLException(e);
      } catch (IOException e) {
        throw new SQLException(e);
      } catch (NoSuchAlgorithmException e) {
        throw new SQLException(e);
      } catch (KeyStoreException e) {
        throw new SQLException(e);
      } catch (CertificateException e) {
        throw new SQLException(e);
      } catch (UnrecoverableKeyException e) {
        throw new SQLException(e);
      } catch (KeyManagementException e) {
        throw new SQLException(e);
      }

    }

    /**
     * Need to provide username/password when getting schema,
     * even when authentication is not enabled.
     */
    String userCredentials = "tigergraph:tigergraph";
    if (username != null && password != null) {
      userCredentials = username + ":" + password;
    }
    this.basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));

    // Create the http client builder.
    HttpClientBuilder builder = HttpClients.custom();
    if (hasSSLContext) {
      this.secure = Boolean.TRUE;
      SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext,
        new String[]{"TLSv1.2", "TLSv1.1"},
        null,
        SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
      builder.setSSLSocketFactory(sslConnectionFactory)
        .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
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
    this.httpClient = builder.build();

    if (this.token == null && this.username != null && this.password != null) {
      getToken();
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
      throw new SQLException("No query specified.");
    }

    result = executeQuery(queries.get(0), "");

    /**
     * Execute the queries one by one,
     * and append their results to the firt RestppResponse.
     */
    for(int i = 1; i < queries.size(); i++) {
      RestppResponse newResult = executeQuery(queries.get(i), "");
      result.addResults(newResult.getResults());
    }

    return result;
  }

  /**
   * Get token. e.g.,
   * curl --user tigergraph:tigergraph -X GET \
   * 'http://localhost:14240/gsqlserver/gsql/authtoken?graph=gsql_demo'
   */
  private void getToken() throws SQLException {
    StringBuilder sb = new StringBuilder();
    sb.append("/gsqlserver/gsql/authtoken");
    if (this.graph != null) {
      sb.append("?graph=");
      sb.append(this.graph);
    }
    String url = "";
    try {
      if (this.secure)
        url = new URL("https", host, port, sb.toString()).toString();
      else
        url = new URL("http", host, port, sb.toString()).toString();
    } catch (MalformedURLException e) {
      throw new SQLException("Invalid server URL", e);
    }
    HttpRequestBase request = new HttpGet(url);
    request.addHeader("Authorization", basicAuth);
    request.addHeader("Accept", ContentType.APPLICATION_JSON.toString());
    /**
     * Response example:
     * {"error":false,"message":"","results":{"token":"5r6scnj83963gnfjqtvico1hf2hn394o"}}
     */
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      /**
       * When authentication is turned off, the token request will fail.
       * In this case, just do not use token instead of panic.
       */
      RestppResponse result = new RestppResponse(response, Boolean.FALSE, this.debug);
      List<JSONObject> jsonList = result.getResults();
      for(int i = 0; i < jsonList.size(); i++){
        JSONObject obj = jsonList.get(i);
        if (obj.has("token")) {
          this.token = obj.getString("token");
          if (this.debug > 0) {
            System.out.println(">>> Got token: " + token);
          }
          return;
        }
      }
    } catch (Exception e) {
      throw new SQLException("Failed to get token: " + e);
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
        result = new RestppResponse(response, Boolean.TRUE, this.debug);
        break;
      } catch (Exception e) {
        if (retry >= max_retry - 1) {
          System.out.println(">>> Request: " + request +
             ", payload: " + json + ", error: " + e);
          throw new SQLException("Request: " + request +
             ", payload size: " + json.length() + ", error: " + e);
        }
      }
    }
    if (this.debug > 0 && retry > 0) {
      System.out.println(">>> Rest request succeeded after " + retry + " times retry.");
    }
    return result;
  }

  @Override public boolean isClosed() throws SQLException {
    return closed;
  }

  @Override public void close() throws SQLException {
    closed = Boolean.TRUE;
    try {
      httpClient.close();
    } catch (IOException e) {
      throw new SQLException("Failed to close the http client: " + e);
    }
  }

  @Override public PreparedStatement prepareStatement(String query) throws SQLException {
    return new RestppPreparedStatement(this, query, this.debug, this.timeout, this.atomic);
  }

  @Override public PreparedStatement prepareStatement(String query,
    int resultSetType, int resultSetConcurrency) throws SQLException {
    return new RestppPreparedStatement(this, query, this.debug, this.timeout, this.atomic);
  }

  @Override public PreparedStatement prepareStatement(String query,
    int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLException {
    return new RestppPreparedStatement(this, query, this.debug, this.timeout, this.atomic);
  }

  @Override public DatabaseMetaData getMetaData() throws SQLException {
    return new DatabaseMetaData(this, this.debug);
  }

  @Override public void setAutoCommit(boolean autoCommit) throws SQLException {
    return;
  }

  @Override public boolean getAutoCommit() throws SQLException {
    return Boolean.TRUE;
  }

  @Override public java.sql.Statement createStatement() throws SQLException {
    return new RestppStatement(this, this.debug, this.timeout, this.atomic);
  }

  @Override public void commit() throws SQLException {
    // Update on TigerGraph is autoCommit, nothing to do.
    return;
  }

  @Override public void rollback() throws SQLException {
    return;
  }

  @Override public void setTransactionIsolation(int level) throws SQLException {
    this.level = level;
    return;
  }

  @Override public int getTransactionIsolation() throws SQLException {
    return this.level;
  }

  @Override public boolean isValid(int timeout) throws SQLException {
    return Boolean.TRUE;
  }

  /**
   * Methods not implemented yet.
   */

  @Override public void setHoldability(int holdability) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getHoldability() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public java.sql.Statement createStatement(int resultSetType,
    int resultSetConcurrency) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public java.sql.Statement createStatement(int resultSetType,
    int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

}
