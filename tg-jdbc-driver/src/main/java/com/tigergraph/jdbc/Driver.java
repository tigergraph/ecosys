package com.tigergraph.jdbc;

import com.tigergraph.jdbc.restpp.RestppDriver;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Driver extends BaseDriver {

  /**
   * <Prefix, class> hash map of all available drivers.
   */
  @SuppressWarnings({"rawtypes", "serial"})
  private final Map<String, Class> DRIVERS = new HashMap<String, Class>() {{
    put(RestppDriver.JDBC_RESTPP_PREFIX, RestppDriver.class);
  }};

  public Driver() throws SQLException {
    super(null);
  }

  @Override public Connection connect(String url, Properties info) throws SQLException {
    return connect(url, info, Boolean.FALSE);
  }

  @Override public Connection connect(String url, Properties info, Boolean debug) throws SQLException {
    return getDriver(url).connect(url, info, debug);
  }

  /**
   * Retrieve the corresponding driver according to url.
   * @param url: The JDBC url
   * @return The driver
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private BaseDriver getDriver(String url) throws SQLException {
    BaseDriver driver = null;

    if (url == null) {
      throw new SQLException("null is not a valid url");
    }

    try {
      // Check the driver prefix from the url
      if (url.startsWith(JDBC_PREFIX)) {
        String[] pieces = url.split(":");
        if (pieces.length > 3) {
          String prefix = pieces[2];

          // Search the driver hash map.
          for(String key: DRIVERS.keySet()) {
            if (prefix.matches(key)) {
              Constructor constructor = DRIVERS.get(key).getDeclaredConstructor();
              driver = (BaseDriver) constructor.newInstance();
            }
          }
        }
      }
    } catch (Exception e) {
      throw new SQLException(e);
    }

    if (driver == null) {
      throw new SQLException("Cannot find a suitable driver from the url " + url);
    }

    return driver;
  }
}

