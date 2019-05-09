package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.restpp.driver.QueryParser;
import com.tigergraph.jdbc.restpp.driver.RestppResponse;
import com.tigergraph.jdbc.Connection;
import com.tigergraph.jdbc.ResultSet;
import com.tigergraph.jdbc.DatabaseMetaData;
import com.tigergraph.jdbc.PreparedStatement;
import com.tigergraph.jdbc.*;

import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import org.apache.http.util.EntityUtils;
import org.apache.http.HttpEntity;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class RestppConnection extends Connection {

  private Boolean closed = Boolean.FALSE;
  private String host;
  private Integer port;
  private Boolean secure;
  private String token;
  private String graph;
  private Boolean debug = Boolean.FALSE;

  private CloseableHttpClient httpClient;

  /**
   * Default constructor.
   */
  public RestppConnection(String host, Integer port, Boolean secure,
      Properties properties, String url, Boolean debug) throws SQLException {
    super(properties, url);
    this.secure = secure;
    this.host = host;
    this.port = port;
    this.debug = debug;

    // Get token for authentication.
    if (null != properties && properties.containsKey("token")) {
      this.token = properties.getProperty("token");
    } else {
      this.token = null;
    }

    // Get graph name.
    if (null != properties && properties.containsKey("graph")) {
      this.graph = properties.getProperty("graph");
    } else {
      this.graph = null;
    }

    // Create the http client builder.
    HttpClientBuilder builder = HttpClients.custom();
    if (null != properties && properties.containsKey("useragent")) {
      String userAgent = properties.getProperty("useragent");
      builder.setUserAgent(userAgent);
    }
    this.httpClient = builder.build();
  }

  public RestppResponse executeQueries(final List<QueryParser> queries) throws SQLException {
    RestppResponse result = null;

    if (queries.size() < 1) {
      throw new SQLException("No query specified.");
    }

    result = executeQuery(queries.get(0));

    /**
     * Execute the queries one by one,
     * and append their results to the firt RestppResponse.
     */
    for(int i = 1; i < queries.size(); i++) {
      RestppResponse newResult = executeQuery(queries.get(i));
      result.addResults(newResult.getResults());
    }

    return result;
  }

  public RestppResponse executeQuery(QueryParser parser) throws SQLException {
    RestppResponse result = null;
    HttpRequestBase request = parser.buildQuery(host, port, secure, graph, token);
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      result = new RestppResponse(response, this.debug);
    } catch (Exception e) {
      throw new SQLException(e);
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

  /**
   * Methods not implemented yet.
   */

  @Override public DatabaseMetaData getMetaData() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setAutoCommit(boolean autoCommit) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean getAutoCommit() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void commit() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void rollback() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public void setHoldability(int holdability) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public int getHoldability() throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public java.sql.Statement createStatement() throws SQLException {
    return new RestppStatement(this, this.debug);
  }

  @Override public java.sql.Statement createStatement(int resultSetType,
    int resultSetConcurrency) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public java.sql.Statement createStatement(int resultSetType,
    int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public PreparedStatement prepareStatement(String query) throws SQLException {
    return new RestppPreparedStatement(this, query, this.debug);
  }

  @Override public PreparedStatement prepareStatement(String query,
    int resultSetType, int resultSetConcurrency) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public PreparedStatement prepareStatement(String query,
    int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override public boolean isValid(int timeout) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

}

