package com.tigergraph.spark.client.common;

public class RestppErrorException extends RuntimeException {
  public RestppErrorException(String code, String message) {
    super(String.format("RESTPP error response, code: %s, message: %s", code, message));
  }

  public RestppErrorException(String message, Throwable cause) {
    super(message, cause);
  }

  public RestppErrorException(String message) {
    super(message);
  }
}
