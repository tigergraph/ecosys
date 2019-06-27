package com.tigergraph.common.util.error;

/**
 * The ReturnCode constants between client and server.
 */
public class ReturnCode {

  // wrong password, user/secret doesn't exist, etc.
  public static final int LOGIN_OR_AUTH_ERROR = 41;

  // normal exit
  public static final int NO_ERROR = 0;

  public static final int NUM_FORMAT_ERROR = 255;

  // server error
  public static final int SYNTAX_ERROR = 211;
  public static final int RUNTIME_ERROR = 212;
  public static final int NO_GRAPH_ERROR = 213;

  // client error
  public static final int CLIENT_ARGUMENT_ERROR = 201;
  public static final int CONNECTION_ERROR = 202;
  public static final int CLIENT_COMPATIBILITY_ERROR = 203;

  // unknown
  public static final int UNKNOWN_ERROR = 255;
}
