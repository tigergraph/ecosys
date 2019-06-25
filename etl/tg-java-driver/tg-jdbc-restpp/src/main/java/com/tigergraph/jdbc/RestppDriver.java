package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.BaseDriver;
import com.tigergraph.jdbc.restpp.RestppConnection;

import java.net.URL;
import java.sql.Connection;
import java.util.Properties;
import java.sql.SQLException;
import java.net.MalformedURLException;

/**
 * JDBC Driver class for the Restpp connector.
 */
public class RestppDriver extends BaseDriver {

  private Boolean debug = Boolean.FALSE;

  public final static String JDBC_RESTPP_PREFIX = "http(s)?";

  /**
   * Default constructor.
   */
  public RestppDriver() throws SQLException {
    super(JDBC_RESTPP_PREFIX);
  }

  @Override public Connection connect(String url, Properties params, Boolean debug) throws SQLException {
    this.debug = debug;
    return connect(url, params);
  }

  @Override public Connection connect(String url, Properties params) throws SQLException {
    Connection connection = null;
    try {
      if (acceptsURL(url)) {
        URL tgUrl = new URL(url.replace("jdbc:tg:", "").replaceAll("^(" + JDBC_RESTPP_PREFIX + ":)([^/])", "$1//$2"));
        String host = tgUrl.getHost();
        Boolean secure = tgUrl.getProtocol().equals("https");
        int port = tgUrl.getPort();
        if (port < 0 || port > 65535) {
          port = 9000;
        }
        connection = new RestppConnection(host, port, secure, params, url, this.debug);
      } else {
        throw new SQLException("The URL is invalid.\nA valid URL is a string like this: 'jdbc:tg:http[s]://<host>:<port>'");
      }
    } catch (MalformedURLException e) {
      throw new SQLException(e);
    }

    return connection;
  }

}

