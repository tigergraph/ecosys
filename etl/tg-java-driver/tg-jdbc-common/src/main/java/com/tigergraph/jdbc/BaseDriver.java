package com.tigergraph.jdbc;

import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public abstract class BaseDriver implements java.sql.Driver {

  protected static final String JDBC_PREFIX = "jdbc:tg:";
  private String DRIVER_PREFIX;

  protected BaseDriver(String prefix) throws SQLException {
    this.DRIVER_PREFIX = prefix;
    DriverManager.registerDriver(this);
  }

  @Override public abstract Connection connect(String url, Properties info) throws SQLException;

  public abstract Connection connect(String url, Properties info, Boolean debug) throws SQLException;

	@Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return new DriverPropertyInfo[0];
	}

	@Override public int getMajorVersion() {
		return 1;
	}

	@Override public int getMinorVersion() {
		return 0;
	}

	@Override public boolean jdbcCompliant() {
		return false;
	}

	@Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new UnsupportedOperationException("Not implemented yet.");
	}

  @Override public boolean acceptsURL(String url) throws SQLException {
    if (null == url) {
      throw new SQLException("url is invalid.");
    }

    String[] parts = url.split(":");
    if ((parts.length > 3) && (url.startsWith(JDBC_PREFIX))) {
      if (null != DRIVER_PREFIX) {
        return parts[2].matches(DRIVER_PREFIX);
      } else {
        return true;
      }
    }
    return false;
  }

}
