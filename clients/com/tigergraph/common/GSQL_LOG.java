package com.tigergraph.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class GSQL_LOG {
  public enum LEVEL {
    // error log, always print
    ERROR(0, "E"),
    // info log, print by default
    INFO(1, "I"),
    // debug log
    DEBUG(2, "D"),
    // verbose log
    VERBOSE(3, "V");

    private int level;
    private String symbol;
    LEVEL(int level, String symbol) {
      this.level = level;
      this.symbol = symbol;
    }
    public String toString() {
      return this.symbol;
    }
    public boolean printLog(LEVEL l) {
      return l.level <= this.level;
    }
  }

  public static String LOG_FILE = null;
  public static LEVEL LOG_LEVEL = LEVEL.INFO;

  /**
   * Print a string to LOG with a format
   * I@yyyymmdd hh:mm:ss.sss user|ip|session (file.java:line) text
   * @param level the log level
   * @param text the content to be print
   * @param stack the number of file in call stack, default 0
   */
  private static void LogText(LEVEL level, String text, String session, int stack) throws Exception {
    if (LOG_FILE == null || !LOG_LEVEL.printLog(level)) return;
    // try to get the file name and line number of the caller
    String fileInfo = "";
    try {
      int skip = 2 + stack; // skip first two in stack trace
      for (StackTraceElement s : Thread.currentThread().getStackTrace()) {
        if (skip-- > 0) continue;
        fileInfo = "(" + s.getFileName() + ":" + s.getLineNumber() + ")";
        break;
      }
    } catch (Exception e) {
      // ignore if get any exception
    }

    if (text != null && text.startsWith("\n")) {
      text = text.substring(1);
    }

    if (session == null) session = "";

    File f = new File(LOG_FILE);
    String ts = new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS").format(new Date());
    String logContent = level.toString() + "@" + ts + " " + session + " " + fileInfo + " " + text;
    BufferedWriter bw = new BufferedWriter(new FileWriter(f.getPath(), true));
    PrintWriter pw = new PrintWriter(bw, true);
    pw.println(logContent);
    pw.close();
  }

  public static void LogDebug(String text, String session, int stack) throws Exception {
    LogText(LEVEL.DEBUG, text, session, stack + 1);
  }

  public static void LogVerbose(String text, String session, int stack) throws Exception {
    LogText(LEVEL.VERBOSE, text, session, stack + 1);
  }

  public static void LogError(String text, String session, int stack) throws Exception {
    LogText(LEVEL.ERROR, text, session, stack + 1);
  }

  public static void LogInfo(String text, String session, int stack) throws Exception {
    LogText(LEVEL.INFO, text, session, stack + 1);
  }

  public static void LogDebug(String text, String session) throws Exception {
    LogDebug(text, session, 1);
  }

  public static void LogVerbose(String text, String session) throws Exception {
    LogVerbose(text, session, 1);
  }

  public static void LogError(String text, String session) throws Exception {
    LogError(text, session, 1);
  }

  public static void LogInfo(String text, String session) throws Exception {
    LogInfo(text, session, 1);
  }

  public static void LogExceptions(Throwable e, String session, int stack) throws Exception {
    if (LOG_FILE == null) return;
    LogText(LEVEL.ERROR, e.getMessage(), session, stack + 1);

    File f = new File(LOG_FILE);
    BufferedWriter bw = new BufferedWriter(new FileWriter(f.getPath(), true));
    PrintWriter pw = new PrintWriter(bw, true);
    e.printStackTrace(pw);
    pw.close();
  }
}
