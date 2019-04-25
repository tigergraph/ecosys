package com.tigergraph.jdbc.utils;

/**
 * An exception builder.
 */
public class ExceptionBuilder {

  /**
   * Build a not implemented exception with method and class name.
   *
   * @return an UnsupportedOperationException
   */
  public static UnsupportedOperationException buildUnsupportedOperationException() {
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    if (stackTraceElements.length > 2) {
      StackTraceElement caller = stackTraceElements[2];

      StringBuilder sb = new StringBuilder().append("Method ").append(caller.getMethodName())
        .append(" in class ").append(caller.getClassName()).append(" is not implemented yet.");

      return new UnsupportedOperationException(sb.toString());
    }

    return new UnsupportedOperationException("Not implemented yet.");
  }
}

