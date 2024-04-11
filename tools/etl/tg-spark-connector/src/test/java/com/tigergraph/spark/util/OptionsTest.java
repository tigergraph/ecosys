package com.tigergraph.spark.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
    Options options = new Options(null, Options.OptionType.WRITE, true);
    List<OptionError> errors = options.validateOpts();
    assertEquals(4, errors.size());
  }

  @Test
  public void shouldSslModeError() {
    Map<String, String> originals = new HashMap<>(defaultOptions);
    originals.put(Options.SSL_MODE, "ssl_mode_test");
    Options options = new Options(originals, Options.OptionType.WRITE, true);
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
    Options options = new Options(originals, Options.OptionType.WRITE, true);
    List<OptionError> errors = options.validateOpts();
    assertEquals(1, errors.size());
    assertEquals(
        "Option(io.connect.timeout.ms) failed to convert the value to type INT, error is"
            + " java.lang.NumberFormatException: For input string: \"not_integer\"",
        errors.get(0).getErrorMsgs().get(0));
  }

  @Test
  public void shouldPassValidation() {
    Options options = new Options(defaultOptions, Options.OptionType.WRITE, true);
    List<OptionError> errors = options.validateOpts();
    assertEquals(0, errors.size());
  }

  @Test
  public void testPartitionedQueryOpts() {
    // 1. lack of some options
    Map<String, String> originals_1 =
        new HashMap<String, String>() {
          {
            put(Options.GRAPH, "graph_test");
            put(Options.URL, "url_test");
            put(Options.QUERY_PARTITION_KEY, "length");
            put(Options.QUERY_PARTITION_NUM, "5");
          }
        };

    assertThrowsExactly(
        IllegalArgumentException.class,
        () -> new Options(originals_1, Options.OptionType.READ, false));

    // 2. non-numeric range value
    Map<String, String> originals_2 =
        new HashMap<String, String>() {
          {
            put(Options.GRAPH, "graph_test");
            put(Options.URL, "url_test");
            put(Options.QUERY_PARTITION_KEY, "length");
            put(Options.QUERY_PARTITION_NUM, "5");
            put(Options.QUERY_PARTITION_LOWER_BOUND, "not a number");
            put(Options.QUERY_PARTITION_UPPER_BOUND, "123");
          }
        };
    assertThrowsExactly(
        IllegalArgumentException.class,
        () -> new Options(originals_2, Options.OptionType.READ, false));

    // 3. lower >= upper
    Map<String, String> originals_3 =
        new HashMap<String, String>() {
          {
            put(Options.GRAPH, "graph_test");
            put(Options.URL, "url_test");
            put(Options.QUERY_PARTITION_KEY, "length");
            put(Options.QUERY_PARTITION_NUM, "5");
            put(Options.QUERY_PARTITION_LOWER_BOUND, "12345");
            put(Options.QUERY_PARTITION_UPPER_BOUND, "12345");
          }
        };
    assertThrowsExactly(
        IllegalArgumentException.class,
        () -> new Options(originals_3, Options.OptionType.READ, false));

    // 4. correct options
    Map<String, String> originals_4 =
        new HashMap<String, String>() {
          {
            put(Options.URL, "url_test");
            put(Options.GRAPH, "graph_test");
            put(Options.QUERY_PARTITION_KEY, "length");
            put(Options.QUERY_PARTITION_NUM, "5");
            put(Options.QUERY_PARTITION_LOWER_BOUND, "100");
            put(Options.QUERY_PARTITION_UPPER_BOUND, "1000");
          }
        };
    assertDoesNotThrow(() -> new Options(originals_4, Options.OptionType.READ, false));
  }
}
