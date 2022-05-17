package com.tigergraph.jdbc.log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Default formatter for JUL, e.g. 05-09 02:37:53 [SEVERE] Default JUL Format
 */

public class JULFormatter extends Formatter{
    private static final DateFormat df = new SimpleDateFormat("MM-dd HH:mm:ss");

    static {
      df.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public String format(LogRecord record) {
      StringBuilder builder = new StringBuilder(256);
      builder.append(df.format(new Date(record.getMillis()))).append(" [");
      builder.append(record.getLevel()).append("] ");
      builder.append(formatMessage(record));
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
}
