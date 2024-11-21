package com.tigergraph.jdbc.log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.io.*;

/** Default formatter for JUL, e.g. 05-09 02:37:53 [SEVERE] Default JUL Format */
public class JULFormatter extends Formatter {
  private static final DateFormat df = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");

  static {
    df.setTimeZone(TimeZone.getDefault());
  }

  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String ANSI_BLUE = "\u001B[34m";
  public static final String ANSI_WHITE = "\u001B[37m";

  private final boolean isConsole;

  JULFormatter(boolean isConsole) {
    super();
    this.isConsole = isConsole;
  }

  @Override
  public String format(LogRecord record) {
    String throwable = "";
    int level = record.getLevel().intValue();
    if (record.getThrown() != null) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println();
      record.getThrown().printStackTrace(pw);
      pw.close();
      throwable = sw.toString();
    }

    StringBuilder builder = new StringBuilder(256);
    // Set color for console output
    if (isConsole) {
      builder.append(getColor(level));
    }
    builder.append(df.format(new Date(record.getMillis()))).append(" [");
    builder.append(getLocalizedLevel(level)).append("] ");
    builder.append(formatMessage(record));
    builder.append(throwable);
    if (isConsole) {
      builder.append(ANSI_RESET);
    }
    builder.append("\n");
    return builder.toString();
  }

  @Override
  public String getHead(Handler h) {
    return super.getHead(h);
  }

  @Override
  public String getTail(Handler h) {
    return super.getTail(h);
  }

  private String getLocalizedLevel(int level) {
    switch (level) {
      // SEVERE
      case 1000:
        return "ERROR";
      // WARNING
      case 900:
        return "WARN";
      // INFO
      case 800:
        return "INFO";
      // FINE
      case 500:
      default:
        return "DEBUG";
    }
  }

  private String getColor(int level) {
    switch (level) {
      // ERROR
      case 1000:
        return ANSI_RED;
      // WARN
      case 900:
        return ANSI_YELLOW;
      // INFO
      case 800:
        return ANSI_WHITE;
      // DEBUG
      case 500:
      default:
        return ANSI_BLUE;
    }
  }
}
