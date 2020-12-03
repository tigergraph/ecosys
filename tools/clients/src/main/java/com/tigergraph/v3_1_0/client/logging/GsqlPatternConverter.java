/**
 * ****************************************************************************
 * Copyright (c) 2020, TigerGraph Inc.
 * All rights reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * ****************************************************************************
 */

package com.tigergraph.v3_1_0.client.logging;

import com.tigergraph.v3_1_0.client.util.SystemUtils;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

/**
 * Custom {@code PatternLayout} to add user session as %us
 */
@Plugin(name = "GsqlPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"us"})
public final class GsqlPatternConverter extends LogEventPatternConverter {

  /**
   * Private constructor.
   *
   * @param options options, may be null.
   */
  private GsqlPatternConverter(final String[] options) {
    super("User Session", "userSession");
  }

  /**
   * Obtains an instance of {@code PatternConverter}.
   *
   * @param options options, may be null
   * @return instance of {@code PatternConverter}
   */
  public static GsqlPatternConverter newInstance(final String[] options) {
    return new GsqlPatternConverter(options);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void format(final LogEvent event, final StringBuilder toAppendTo) {
    String userSession = SystemUtils.logger.getSession();
    // since userSession can be null,
    // we put the space separator only when userSession is not null
    toAppendTo.append(userSession == null ? "" : userSession + " ");
  }
}
