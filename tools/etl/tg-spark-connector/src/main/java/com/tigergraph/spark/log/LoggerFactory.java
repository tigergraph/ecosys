package com.tigergraph.spark.log;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;

import org.slf4j.Logger;

/** create logger according to specified logger implementation */
public class LoggerFactory {

  private static final Level[] levelMapping =
      new Level[] {Level.SEVERE, Level.WARNING, Level.INFO, Level.FINE};

  private enum LoggerType {
    JUL_DEFAULT,
    SLF4J
  }

  private static final String DEFAULT_ROOT_LOGGER_NAME = "com.tigergraph.spark";

  private static LoggerType loggerType = LoggerType.SLF4J;

  /**
   * @param clazz the returned logger will be named after clazz
   * @return An instance of JavaLogger of SLF4JLogger
   */
  public static Logger getLogger(Class<?> clazz) {
    switch (loggerType) {
      case SLF4J:
        return org.slf4j.LoggerFactory.getLogger(clazz);
      case JUL_DEFAULT:
      default:
        return new JULAdapter(clazz.getName());
    }
  }

  /**
   * Initialize parent logger `com.tigergraph.spark` with default settings if JUL_DEFAULT
   *
   * @param level debug level passed in by properties
   */
  public static synchronized void initJULLogger(Integer level, String pattern) {
    loggerType = LoggerType.JUL_DEFAULT;
    java.util.logging.Logger rootLogger =
        java.util.logging.Logger.getLogger(DEFAULT_ROOT_LOGGER_NAME);
    // Do not forward any log messages to the logger parent handlers.
    if (rootLogger.getHandlers().length == 0) {
      rootLogger.setUseParentHandlers(false);
      if (pattern != null && !pattern.isEmpty()) {
        try {
          rootLogger.addHandler(new FileHandler(pattern, 1024 * 1024 * 100, 100, true));
          rootLogger.getHandlers()[0].setFormatter(new JULFormatter());
        } catch (Exception e) {
          throw new UnsupportedOperationException(
              "Failed to initialize the file log handler for " + pattern, e);
        }
      } else {
        rootLogger.addHandler(new ConsoleHandler());
        rootLogger.getHandlers()[0].setFormatter(new JULFormatter());
      }

      if (level < 0 || level > 3) {
        rootLogger.setLevel(Level.INFO);
        rootLogger.getHandlers()[0].setLevel(Level.INFO);
        rootLogger.log(
            Level.WARNING,
            "Debug Level should between 0 and 3 but got {0}. Use 2(INFO) by default",
            level);
      } else {
        rootLogger.setLevel(levelMapping[level]);
        rootLogger.getHandlers()[0].setLevel(levelMapping[level]);
      }
    }
  }
}
