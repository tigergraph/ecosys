package com.tigergraph.v3_0_0_beta.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class GsqlLogger {
  /**
   * Log levels.
   * The order is important because the logger will skip logging with the lower rank.
   * For example, if {@code GsqlLogger.logLevel} is {@code Level.ERROR}
   * and the given message has {@code Level.INFO}, it will be discarded.
   */
  public enum Level {
    OFF, // turn off logging
    ERROR, // error log
    INFO, // info log (default)
    DEBUG; // debug log

    /**
     * Get the symbol of this {@code Level} that is the first character.
     */
    public char getSymbol() {
      return this.name().charAt(0);
    }
  }

  // File name is in the following format: GSQL#1.out
  // where 1 is the replica number passed from Executor.
  private static final String FILE_PREFIX = "GSQL";
  private static final String FILE_REPLICA_SEPARATOR = "#";
  private static final String FILE_EXT = ".out";

  /**
   * Full path to the log file.
   * e.g. /home/tigergraph/tigergraph/logs/gsql/GSQL#1.out
   */
  public static String filePath = null;

  /**
   * Log level for this Logger.
   * This will be dynamically changed by setting the value of
   * {@code GSQL.BasicConfig.LogConfig.LogLevel} in tg.cfg.
   * By Default it is {@code Level.INFO}
   */
  public static Level logLevel = Level.INFO;

  /**
   * Log {@code text} with the following format:
   * <LOG_LEVEL_SYMBOL>@<TIMESTAMP> <USER>|<IP>|<SESSION> (<FILE_NAME>:<LINE_NUM>) <TEXT>
   * <TIMESTAMP> is in yyyymmdd hh:mm:ss.sss.
   * 
   * @param level {@code Level} of this log
   * @param text Message to log
   * @param session Info of <USER>|<IP>|<SESSION>
   * @param stack The number of file in call stack (0 by default)
   * @throws IOException if there is an issue with {@code filePath}
   */
  private static void log(Level level, String text, String session, int stack) throws IOException {
    if (filePath == null || !isLoggable(level)) return;
    // try to get the file name and line number of the caller
    String fileInfo = "";
    try {
      int skip = 2 + stack; // skip first two in stack trace
      for (StackTraceElement s : Thread.currentThread().getStackTrace()) {
        if (skip-- > 0) continue;
        fileInfo = "(" + s.getFileName() + ":" + s.getLineNumber() + ")";
        break;
      }
    } catch (SecurityException e) {
      // ignore SecurityException while logging
    }

    File f = new File(filePath);
    PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(f.getPath(), true)), true);
    // log with the specific format
    pw.println(String.format("%s@%s %s %s %s",
        level.getSymbol(),
        new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS").format(new Date()),
        session == null ? "" : session,
        fileInfo,
        text == null ? "" : text.trim()));
    pw.close();
  }

  /**
   * Check if the given {@code Level} is loggable based on the {@code logLevel}.
   * If the {@code logLevel} is {@code Level.OFF}, it is not loggable in any case.
   * Else, {@code other} must be higher or equal rank of the {@code logLevel} to be logged.
   * 
   * @param other {@code Level} of a message to log
   * @return {@code true} if the message of {@code other} is loggable;
   *         {@code false} otherwise
   */
  private static boolean isLoggable(Level other) {
    return logLevel != Level.OFF && other.ordinal() <= logLevel.ordinal();
  }

  /**
   * Log stack trace when there is an exception.
   * 
   * @param e {@code Throwable}
   * @param session Info of <USER>|<IP>|<SESSION>
   * @param stack The number of file in call stack (0 by default)
   * @throws IOException if there is an issue with {@code filePath}
   */
  public static void exception(Throwable e, String session, int stack) throws IOException {
    if (filePath == null) return;
    log(Level.ERROR, e.getMessage(), session, stack + 1);

    File f = new File(filePath);
    PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(f.getPath(), true)), true);
    e.printStackTrace(pw);
    pw.close();
  }

  public static void error(String text, String session, int stack) throws IOException {
    log(Level.ERROR, text, session, stack + 1);
  }

  public static void error(String text, String session) throws IOException {
    error(text, session, 1);
  }

  public static void info(String text, String session, int stack) throws IOException {
    log(Level.INFO, text, session, stack + 1);
  }

  public static void info(String text, String session) throws IOException {
    info(text, session, 1);
  }

  public static void debug(String text, String session, int stack) throws IOException {
    log(Level.DEBUG, text, session, stack + 1);
  }

  public static void debug(String text, String session) throws IOException {
    debug(text, session, 1);
  }

  /**
   * Get name of the log file based on replica number for this specific GSQL server.
   * 
   * @param replicaNum Replica number for this GSQL server
   * @return Name of the log file, e.g. GSQL#1.out
   */
  public static String getFileName(int replicaNum) {
    return FILE_PREFIX + FILE_REPLICA_SEPARATOR + replicaNum + FILE_EXT;
  }

}
