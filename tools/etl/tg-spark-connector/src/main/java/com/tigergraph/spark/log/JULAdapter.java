package com.tigergraph.spark.log;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.Marker;

/**
 * Wrapper of java.util.logging to log Spark Connector information.
 *
 * <p>Only 4 levels are included: ERROR, WARN, INFO and DEBUG
 *
 * <p>This class implements a part of SLF4J Logger interface LocationAwareLogger, Marker and Lazy
 * Evaluation are not supported
 *
 * <p>mapping from TGLogger to java.util.logging: ERROR -> SEVERE WARN -> WARNING INFO -> INFO DEBUG
 * -> FINE
 */
public class JULAdapter implements org.slf4j.Logger {

  private final Logger logger;
  private final String name;

  public JULAdapter(String name) {
    logger = Logger.getLogger(name);
    this.name = name;
  }

  /**
   * Is the logger instance enabled for the DEBUG level?
   *
   * @return True if this Logger is enabled for the DEBUG level, false otherwise.
   */
  @Override
  public boolean isDebugEnabled() {
    return logger.isLoggable(Level.FINE);
  }

  /**
   * Log a message at the DEBUG level.
   *
   * @param msg the message string to be logged
   */
  @Override
  public void debug(String msg) {
    logger.fine(msg);
  }

  /**
   * Log a message at the DEBUG level according to the specified format and argument.
   *
   * <p>
   *
   * <p>This form avoids superfluous object creation when the logger is disabled for the DEBUG
   * level.
   *
   * @param format the format string
   * @param arg the argument
   */
  @Override
  public void debug(String format, Object arg) {
    logger.log(Level.FINE, formatConverter(format), arg);
  }

  /**
   * Log a message at the DEBUG level according to the specified format and arguments.
   *
   * <p>
   *
   * <p>This form avoids superfluous object creation when the logger is disabled for the DEBUG
   * level.
   *
   * @param format the format string
   * @param arg1 the first argument
   * @param arg2 the second argument
   */
  @Override
  public void debug(String format, Object arg1, Object arg2) {
    logger.log(Level.FINE, formatConverter(format), new Object[] {arg1, arg2});
  }

  /**
   * Log a message at the DEBUG level according to the specified format and arguments.
   *
   * <p>
   *
   * <p>This form avoids superfluous string concatenation when the logger is disabled for the DEBUG
   * level. However, this variant incurs the hidden (and relatively small) cost of creating an
   * <code>Object[]</code> before invoking the method, even if this logger is disabled for DEBUG.
   * The variants taking {@link #debug(String, Object) one} and {@link #debug(String, Object,
   * Object) two} arguments exist solely in order to avoid this hidden cost.
   *
   * @param format the format string
   * @param arguments a list of 3 or more arguments
   */
  @Override
  public void debug(String format, Object... arguments) {
    logger.log(Level.FINE, formatConverter(format), arguments);
  }

  /**
   * Log an exception (throwable) at the DEBUG level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t the exception (throwable) to log
   */
  @Override
  public void debug(String msg, Throwable t) {
    logger.log(Level.FINE, msg, t);
  }

  /**
   * Is the logger instance enabled for the INFO level?
   *
   * @return True if this Logger is enabled for the INFO level, false otherwise.
   */
  @Override
  public boolean isInfoEnabled() {
    return logger.isLoggable(Level.INFO);
  }

  /**
   * Log a message at the INFO level.
   *
   * @param msg the message string to be logged
   */
  @Override
  public void info(String msg) {
    logger.info(msg);
  }

  /**
   * Log a message at the INFO level according to the specified format and argument.
   *
   * <p>
   *
   * <p>This form avoids superfluous object creation when the logger is disabled for the INFO level.
   *
   * @param format the format string
   * @param arg the argument
   */
  @Override
  public void info(String format, Object arg) {
    logger.log(Level.INFO, formatConverter(format), arg);
  }

  /**
   * Log a message at the INFO level according to the specified format and arguments.
   *
   * <p>
   *
   * <p>This form avoids superfluous object creation when the logger is disabled for the INFO level.
   *
   * @param format the format string
   * @param arg1 the first argument
   * @param arg2 the second argument
   */
  @Override
  public void info(String format, Object arg1, Object arg2) {
    logger.log(Level.INFO, formatConverter(format), new Object[] {arg1, arg2});
  }

  /**
   * Log a message at the INFO level according to the specified format and arguments.
   *
   * <p>
   *
   * <p>This form avoids superfluous string concatenation when the logger is disabled for the INFO
   * level. However, this variant incurs the hidden (and relatively small) cost of creating an
   * <code>Object[]</code> before invoking the method, even if this logger is disabled for INFO. The
   * variants taking {@link #info(String, Object) one} and {@link #info(String, Object, Object) two}
   * arguments exist solely in order to avoid this hidden cost.
   *
   * @param format the format string
   * @param arguments a list of 3 or more arguments
   */
  @Override
  public void info(String format, Object... arguments) {
    logger.log(Level.INFO, formatConverter(format), arguments);
  }

  /**
   * Log an exception (throwable) at the INFO level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t the exception (throwable) to log
   */
  @Override
  public void info(String msg, Throwable t) {
    logger.log(Level.INFO, msg, t);
  }

  /**
   * Is the logger instance enabled for the WARN level?
   *
   * @return True if this Logger is enabled for the WARN level, false otherwise.
   */
  @Override
  public boolean isWarnEnabled() {
    return logger.isLoggable(Level.WARNING);
  }

  /**
   * Log a message at the WARN level.
   *
   * @param msg the message string to be logged
   */
  @Override
  public void warn(String msg) {
    logger.warning(msg);
  }

  /**
   * Log a message at the WARN level according to the specified format and argument.
   *
   * <p>
   *
   * <p>This form avoids superfluous object creation when the logger is disabled for the WARN level.
   *
   * @param format the format string
   * @param arg the argument
   */
  @Override
  public void warn(String format, Object arg) {
    logger.log(Level.WARNING, formatConverter(format), arg);
  }

  /**
   * Log a message at the WARN level according to the specified format and arguments.
   *
   * <p>
   *
   * <p>This form avoids superfluous string concatenation when the logger is disabled for the WARN
   * level. However, this variant incurs the hidden (and relatively small) cost of creating an
   * <code>Object[]</code> before invoking the method, even if this logger is disabled for WARN. The
   * variants taking {@link #warn(String, Object) one} and {@link #warn(String, Object, Object) two}
   * arguments exist solely in order to avoid this hidden cost.
   *
   * @param format the format string
   * @param arguments a list of 3 or more arguments
   */
  @Override
  public void warn(String format, Object... arguments) {
    logger.log(Level.WARNING, formatConverter(format), arguments);
  }

  /**
   * Log a message at the WARN level according to the specified format and arguments.
   *
   * <p>
   *
   * <p>This form avoids superfluous object creation when the logger is disabled for the WARN level.
   *
   * @param format the format string
   * @param arg1 the first argument
   * @param arg2 the second argument
   */
  @Override
  public void warn(String format, Object arg1, Object arg2) {
    logger.log(Level.WARNING, formatConverter(format), new Object[] {arg1, arg2});
  }

  /**
   * Log an exception (throwable) at the WARN level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t the exception (throwable) to log
   */
  @Override
  public void warn(String msg, Throwable t) {
    logger.log(Level.WARNING, msg, t);
  }

  /**
   * Is the logger instance enabled for the ERROR level?
   *
   * @return True if this Logger is enabled for the ERROR level, false otherwise.
   */
  @Override
  public boolean isErrorEnabled() {
    return logger.isLoggable(Level.SEVERE);
  }

  /**
   * Log a message at the ERROR level.
   *
   * @param msg the message string to be logged
   */
  @Override
  public void error(String msg) {
    logger.severe(msg);
  }

  /**
   * Log a message at the ERROR level according to the specified format and argument.
   *
   * <p>
   *
   * <p>This form avoids superfluous object creation when the logger is disabled for the ERROR
   * level.
   *
   * @param format the format string
   * @param arg the argument
   */
  @Override
  public void error(String format, Object arg) {
    logger.log(Level.SEVERE, formatConverter(format), arg);
  }

  /**
   * Log a message at the ERROR level according to the specified format and arguments.
   *
   * <p>
   *
   * <p>This form avoids superfluous object creation when the logger is disabled for the ERROR
   * level.
   *
   * @param format the format string
   * @param arg1 the first argument
   * @param arg2 the second argument
   */
  @Override
  public void error(String format, Object arg1, Object arg2) {
    logger.log(Level.SEVERE, formatConverter(format), new Object[] {arg1, arg2});
  }

  /**
   * Log a message at the ERROR level according to the specified format and arguments.
   *
   * <p>
   *
   * <p>This form avoids superfluous string concatenation when the logger is disabled for the ERROR
   * level. However, this variant incurs the hidden (and relatively small) cost of creating an
   * <code>Object[]</code> before invoking the method, even if this logger is disabled for ERROR.
   * The variants taking {@link #error(String, Object) one} and {@link #error(String, Object,
   * Object) two} arguments exist solely in order to avoid this hidden cost.
   *
   * @param format the format string
   * @param arguments a list of 3 or more arguments
   */
  @Override
  public void error(String format, Object... arguments) {
    logger.log(Level.SEVERE, formatConverter(format), arguments);
  }

  /**
   * Log an exception (throwable) at the ERROR level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t the exception (throwable) to log
   */
  @Override
  public void error(String msg, Throwable t) {
    logger.log(Level.SEVERE, msg, t);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isTraceEnabled() {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void trace(String msg) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void trace(String format, Object arg) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void trace(String format, Object arg1, Object arg2) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void trace(String format, Object... arguments) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void trace(String msg, Throwable t) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void trace(Marker marker, String msg) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void trace(Marker marker, String format, Object arg) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void trace(Marker marker, String format, Object arg1, Object arg2) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void trace(Marker marker, String format, Object... argArray) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void trace(Marker marker, String msg, Throwable t) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void debug(Marker marker, String msg) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void debug(Marker marker, String format, Object arg) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void debug(Marker marker, String format, Object arg1, Object arg2) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void debug(Marker marker, String format, Object... arguments) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void debug(Marker marker, String msg, Throwable t) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void info(Marker marker, String msg) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void info(Marker marker, String format, Object arg) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void info(Marker marker, String format, Object arg1, Object arg2) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void info(Marker marker, String format, Object... arguments) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void info(Marker marker, String msg, Throwable t) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void warn(Marker marker, String msg) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void warn(Marker marker, String format, Object arg) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void warn(Marker marker, String format, Object arg1, Object arg2) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void warn(Marker marker, String format, Object... arguments) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void warn(Marker marker, String msg, Throwable t) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void error(Marker marker, String msg) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void error(Marker marker, String format, Object arg) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void error(Marker marker, String format, Object arg1, Object arg2) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void error(Marker marker, String format, Object... arguments) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  @Override
  public void error(Marker marker, String msg, Throwable t) {
    throw new UnsupportedOperationException("Unimplemented mothod.");
  }

  /**
   * Convert slf4j message format to java.util.logging style "hello {}, this is {}" -> "hello {0},
   * this is {1}"
   *
   * @param format
   * @return
   */
  private static String formatConverter(String format) {
    StringBuilder sb = new StringBuilder();
    int argCount = 0;
    for (int i = 0; i < format.length(); i++) {
      if (i < format.length() - 1 && format.charAt(i) == '{' && format.charAt(i + 1) == '}') {
        sb.append(String.format("{%d}", argCount));
        argCount++;
        i++;
      } else {
        sb.append(format.charAt(i));
      }
    }
    return sb.toString();
  }
}
