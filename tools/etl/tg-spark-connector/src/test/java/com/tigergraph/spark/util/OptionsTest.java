package com.tigergraph.spark.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptionsTest {
  static Map<String, String> defaultOptions;

  @BeforeAll
  static void prepare() {
    defaultOptions = new HashMap<>();
    defaultOptions.put(Options.GRAPH, "graph_test");
    defaultOptions.put(Options.URL, "url_test");
    defaultOptions.put(Options.LOADING_JOB, "job_test");
    defaultOptions.put(Options.LOADING_FILENAME, "file_test");
  }

  @Test
  public void shouldHasMultipleErrorsCauseByNoSettings() {
    Options options = new Options(null, Options.OptionType.WRITE);
    List<OptionError> errors = options.validateOpts();
    assertEquals(4, errors.size());
  }

  @Test
  public void shouldSslModeError() {
    Map<String, String> originals = new HashMap<>(defaultOptions);
    originals.put(Options.SSL_MODE, "ssl_mode_test");
    Options options = new Options(originals, Options.OptionType.WRITE);
    List<OptionError> errors = options.validateOpts();
    assertEquals(1, errors.size());
    assertEquals(
        "Option(ssl.mode) must be one of: basic, verifyCA, verifyHostname",
        errors.get(0).getErrorMsgs().get(0));
  }

  @Test
  public void shouldTypeConversionError() {
    Map<String, String> originals = new HashMap<>(defaultOptions);
    originals.put(Options.IO_CONNECT_TIMEOUT_MS, "not_integer");
    Options options = new Options(originals, Options.OptionType.WRITE);
    List<OptionError> errors = options.validateOpts();
    assertEquals(1, errors.size());
    assertEquals(
        "Option(io.connect.timeout.ms) failed to convert the value to type INT, error is"
            + " java.lang.NumberFormatException: For input string: \"not_integer\"",
        errors.get(0).getErrorMsgs().get(0));
  }

  @Test
  public void shouldPassValidation() {
    Options options = new Options(defaultOptions, Options.OptionType.WRITE);
    List<OptionError> errors = options.validateOpts();
    assertEquals(0, errors.size());
  }
}
