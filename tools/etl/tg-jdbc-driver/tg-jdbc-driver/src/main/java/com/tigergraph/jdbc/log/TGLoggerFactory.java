package com.tigergraph.jdbc.log;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.logging.Level;
import java.sql.SQLException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;

/** create logger according to specified logger implementation */
public class TGLoggerFactory {

  private static final Level[] levelMapping =
      new Level[] {Level.SEVERE, Level.WARNING, Level.INFO, Level.FINE};

  private static enum LoggerType {
    JUL_DEFAULT,
    JUL_WITH_CONFIG,
    SLF4J
  }

  private static final String DEFAULT_ROOT_LOGGER_NAME = "com.tigergraph.jdbc";

  private static LoggerType loggerType;

  /**
   * @param clazz the returned logger will be named after clazz
   * @return An instance of JavaLogger of SLF4JLogger
   */
  public static Logger getLogger(Class<?> clazz) {
    switch (loggerType) {
      case SLF4J:
        return LoggerFactory.getLogger(clazz);
      case JUL_DEFAULT:
      case JUL_WITH_CONFIG:
      default:
        return new JULAdapter(clazz.getName());
    }
  }

  /**
   * Initialize parent logger `com.tigergraph.jdbc` with default settings if JUL_DEFAULT
   *
   * <p>NOP if JUL_WITH_CONFIG or SLF4J
   *
   * @param level debug level passed in by properties
   */
  public static synchronized void initializeLogger(Integer level, String pattern)
      throws SQLException {
    loggerType = setLoggerType();
    switch (loggerType) {
      case JUL_WITH_CONFIG:
      case SLF4J:
        return;
      case JUL_DEFAULT:
      default:
        java.util.logging.Logger jdbcRootLogger =
            java.util.logging.Logger.getLogger(DEFAULT_ROOT_LOGGER_NAME);
        // Do not forward any log messages to the logger parent handlers.
        if (jdbcRootLogger.getHandlers().length == 0) {
          jdbcRootLogger.setUseParentHandlers(false);
          try {
            if (pattern != null && !pattern.isEmpty()) {
              jdbcRootLogger.addHandler(new FileHandler(pattern, 1024 * 1024 * 100, 100, true));
              jdbcRootLogger.getHandlers()[0].setFormatter(new JULFormatter(false));
            } else {
              jdbcRootLogger.addHandler(new ConsoleHandler());
              jdbcRootLogger.getHandlers()[0].setFormatter(new JULFormatter(true));
            }
          } catch (Exception e) {
            throw new SQLException("Failed to initialize JDBC log handler", e);
          }

          if (level < 0 || level > 3) {
            jdbcRootLogger.setLevel(Level.INFO);
            jdbcRootLogger.getHandlers()[0].setLevel(Level.INFO);
            jdbcRootLogger.log(
                Level.WARNING,
                "Debug Level should between 0 and 3 but got {0}. Use 2(INFO) by default",
                level);
          } else {
            jdbcRootLogger.setLevel(levelMapping[level]);
            jdbcRootLogger.getHandlers()[0].setLevel(levelMapping[level]);
          }
        }
    }
  }

  /**
   * Read from system properties to determine the logger type
   *
   * @return one of JUL_DEFAULT, JUL_WITH_CONFIG, SLF4J
   */
  private static LoggerType setLoggerType() {
    if (Util.getSysProperty("com.tigergraph.jdbc.loggerImpl") != null
        && Util.getSysProperty("com.tigergraph.jdbc.loggerImpl").equalsIgnoreCase("slf4j")) {
      return LoggerType.SLF4J;
    } else if (Util.getSysProperty("java.util.logging.config.file") != null) {
      return LoggerType.JUL_WITH_CONFIG;
    } else {
      return LoggerType.JUL_DEFAULT;
    }
  }

  /**
   * For testing
   *
   * @return
   */
  public static String getLoggerType() {
    return loggerType.name();
  }
}
