package com.tigergraph.jdbc.restpp;

import com.tigergraph.jdbc.common.BaseDriver;
import com.tigergraph.jdbc.log.TGLoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;

/** JDBC Driver class for the Restpp connector. */
public class RestppDriver extends BaseDriver {

  public static final String JDBC_RESTPP_PREFIX = "http(s)?";
  private static final Logger logger = TGLoggerFactory.getLogger(RestppDriver.class);

  /** Default constructor. */
  public RestppDriver() throws SQLException {
    super(JDBC_RESTPP_PREFIX);
  }

  @Override
  public Connection connect(String url, Properties params) throws SQLException {
    Connection connection = null;
    try {
      if (acceptsURL(url)) {
        URL tgUrl =
            new URL(
                url.replace("jdbc:tg:", "")
                    .replaceAll("^(" + JDBC_RESTPP_PREFIX + ":)([^/])", "$1//$2"));
        String host = tgUrl.getHost();
        Boolean secure = tgUrl.getProtocol().equals("https");
        int port = tgUrl.getPort();
        if (port < 0 || port > 65535) {
          port = 14240;
        }
        connection = new RestppConnection(host, port, secure, params, url);
      } else {
        logger.error(
            "The URL is invalid. A valid URL is a string like this:"
                + " 'jdbc:tg:http[s]://<host>:<port>'");
        throw new SQLException(
            "The URL is invalid. A valid URL is a string like this:"
                + " 'jdbc:tg:http[s]://<host>:<port>'");
      }
    } catch (MalformedURLException e) {
      logger.error("The URL is invalid", e);
      throw new SQLException(e);
    }

    return connection;
  }
}
