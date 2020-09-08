/**
 * **************************************************************************** 
 * Copyright (c) 2020, TigerGraph Inc. 
 * All rights reserved 
 * Unauthorized copying of this file, via any medium is strictly prohibited 
 * Proprietary and confidential 
 * ****************************************************************************
 */

package com.tigergraph.v3_0_5.client.util;

import com.tigergraph.v3_0_5.client.logging.GsqlClientLogger;

import java.nio.file.Paths;

/**
 * Utility methods related to system library such as exit or println
 */
public class SystemUtils {

  /** GSQL Client logger */
  public static GsqlClientLogger logger = GsqlClientLogger.create(SystemUtils.class);

  /** Exit status with pre-defined exit code */
  public enum ExitStatus {
    /** Normal exit */
    OK(0),
    /** Authentication error such as wrong password, user/secret doesn't exist */
    LOGIN_OR_AUTH_ERROR(41),
    /** Error due to wrong arguments given */
    WRONG_ARGUMENT_ERROR(201),
    /** Connection refused */
    CONNECTION_ERROR(202),
    /** Client is not compatible with the server */
    COMPATIBILITY_ERROR(203),
    /** session idle timeout */
    SESSION_TIMEOUT(204),
    /** Systematic runtime error */
    RUNTIME_ERROR(212),
    /** Unknown error */
    UNKNOWN_ERROR(255);

    /** Code to exit the client */
    private int code;

    ExitStatus(int code) {
      this.code = code;
    }

    public int getCode() {
      return this.code;
    }
  }

  /**
   * Exit with {@code ExitStatus.code} after log.
   * 
   * @param es {@code ExitStatus}
   */
  public static void exit(ExitStatus es) {
    System.exit(es.getCode());
  }

  /**
   * Exit with {@code ExitStatus.code} after log and print out a message.<p>
   * Here we assume {@code es} is not {@code ExitStatus.OK} so call {@code logError}.
   * 
   * @param es {@code ExitStatus}
   * @param format A format string
   * @param args Arguments referenced by the format specifiers in the format string
   */
  public static void exit(ExitStatus es, String format, Object... args) {
    println(format, args);
    exit(es);
  }
  
  /**
   * Exit with {@code ExitStatus.code} after log and print out a message based on {@code e}.
   * Here we assume {@code es} is not {@code ExitStatus.OK} so call {@code logError}.
   * 
   * @param es {@code ExitStatus}
   * @param e {@code Exception}
   */
  public static void exit(ExitStatus es, Throwable e) {
    println(e.getMessage());
    // if it's UNKNOWN_ERROR, print additional info to user for them to report
    if (es == ExitStatus.UNKNOWN_ERROR) {
      println("Please contact support@tigergraph.com with the log file: %s",
          Paths.get(System.getProperty("LOG_DIR"), System.getProperty("LOG_FILE_NAME")).toString());
    }
    exit(es);
  }

  /**
   * A wrapper for {@code System.out.println()} with formatted string.
   * 
   * @param format A format string
   * @param args Arguments referenced by the format specifiers in the format string
   */
  public static void println(String format, Object... args) {
    System.out.println(String.format(format, args));
  }

}
