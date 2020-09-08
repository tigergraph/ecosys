/**
 * **************************************************************************** 
 * Copyright (c) 2020, TigerGraph Inc. 
 * All rights reserved 
 * Unauthorized copying of this file, via any medium is strictly prohibited 
 * Proprietary and confidential 
 * ****************************************************************************
 */

package com.tigergraph.v3_0_5.client.logging;

import java.net.URI;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

/**
 * Extended Logger interface
 * partialy auto-generated using org.apache.logging.log4j.core.tools.Generate$ExtendedLogger
 */
public final class GsqlClientLogger extends ExtendedLoggerWrapper {
  private static final long serialVersionUID = 5676088778950L;
  private final ExtendedLoggerWrapper logger;
  private String session;

  /** Fully Qualified Class Name to be logged */
  private static final String FQCN = GsqlClientLogger.class.getName();

  private GsqlClientLogger(final Logger logger) {
    super((AbstractLogger) logger, logger.getName(), logger.getMessageFactory());
    this.logger = this;
  }

  /**
   * Returns a custom Logger using the fully qualified name of the Class as
   * the Logger name.
   * 
   * @param loggerName The Class whose name should be used as the Logger name.
   *                   If null it will default to the calling class.
   * @return The custom Logger.
   */
  public static GsqlClientLogger create(final Class<?> loggerName) {
    final Logger wrapped = LogManager.getLogger(loggerName, new StringFormatterMessageFactory());
    return new GsqlClientLogger(wrapped);
  }

  /**
   * Set seseion info to log.
   * 
   * @param userName the user name
   * @param baseEndpoint the server endpoint
   */
  public void setSession(String userName, URI baseEndpoint) {
    this.session = String.join("|",
        userName,
        String.format("%s:%d", baseEndpoint.getHost(), baseEndpoint.getPort()));
  }

  /**
   * Get session info to log.
   * 
   * @return Session info
   */
  public String getSession() {
    return this.session;
  }

  /**
   * Logs a message at the {@code INFO} level.
   * 
   * @param format A format string
   * @param args Arguments referenced by the format specifiers in the format string
   */
  @Override
  public void info(String format, Object... args) {
    Message msg = new FormattedMessage(format, args);
    logger.logIfEnabled(FQCN, Level.INFO, null, msg, (Throwable) null);
  }

  /**
   * Logs a message at the {@code ERROR} level.
   * 
   * @param format A format string
   * @param args Arguments referenced by the format specifiers in the format string
   */
  @Override
  public void error(String format, Object... args) {
    Message msg = new FormattedMessage(format, args);
    logger.logIfEnabled(FQCN, Level.INFO, null, msg, (Throwable) null);
  }

  /**
   * Logs a message at the {@code ERROR} level including the stack trace of
   * the {@link Throwable} {@code t} passed as parameter.
   * 
   * @param t Exception to log
   */
  public void error(Throwable t) {
    logger.logIfEnabled(FQCN, Level.ERROR, null, t.getMessage(), t);
  }

}
