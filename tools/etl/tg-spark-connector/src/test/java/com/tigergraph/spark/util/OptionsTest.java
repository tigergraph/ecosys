package com.tigergraph.spark.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OptionsTest {
  static Map<String, String> defaultOptions;
  private static final String TEST_VERSION = "99.99.99";

  @BeforeAll
  static void prepare() {
    defaultOptions = new HashMap<>();
    defaultOptions.put(Options.GRAPH, "graph_test");
    defaultOptions.put(Options.URL, "url_test");
    defaultOptions.put(Options.VERSION, TEST_VERSION);
    defaultOptions.put(Options.LOADING_JOB, "job_test");
    defaultOptions.put(Options.LOADING_FILENAME, "file_test");
  }

  @Test
  public void shouldSslModeError() {
    Map<String, String> originals = new HashMap<>(defaultOptions);
    originals.put(Options.SSL_MODE, "ssl_mode_test");
    Options options = new Options(originals, true);
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
    Options options = new Options(originals, true);
    List<OptionError> errors = options.validateOpts();
    assertEquals(1, errors.size());
    assertEquals(
        "Option(io.connect.timeout.ms) failed to convert the value to type INT, error is"
            + " java.lang.NumberFormatException: For input string: \"not_integer\"",
        errors.get(0).getErrorMsgs().get(0));
  }

  @Test
  public void shouldPassValidation() {
    Options options = new Options(defaultOptions, true);
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
            put(Options.VERSION, TEST_VERSION);
            put(Options.QUERY_VERTEX, "Person");
            put(Options.QUERY_PARTITION_KEY, "length");
            put(Options.QUERY_PARTITION_NUM, "5");
          }
        };

    assertThrowsExactly(IllegalArgumentException.class, () -> new Options(originals_1, false));

    // 2. non-numeric range value
    Map<String, String> originals_2 =
        new HashMap<String, String>() {
          {
            put(Options.GRAPH, "graph_test");
            put(Options.URL, "url_test");
            put(Options.VERSION, TEST_VERSION);
            put(Options.QUERY_VERTEX, "Person");
            put(Options.QUERY_PARTITION_KEY, "length");
            put(Options.QUERY_PARTITION_NUM, "5");
            put(Options.QUERY_PARTITION_LOWER_BOUND, "not a number");
            put(Options.QUERY_PARTITION_UPPER_BOUND, "123");
          }
        };
    assertThrowsExactly(IllegalArgumentException.class, () -> new Options(originals_2, false));

    // 3. lower >= upper
    Map<String, String> originals_3 =
        new HashMap<String, String>() {
          {
            put(Options.GRAPH, "graph_test");
            put(Options.URL, "url_test");
            put(Options.VERSION, TEST_VERSION);
            put(Options.QUERY_VERTEX, "Person");
            put(Options.QUERY_PARTITION_KEY, "length");
            put(Options.QUERY_PARTITION_NUM, "5");
            put(Options.QUERY_PARTITION_LOWER_BOUND, "12345");
            put(Options.QUERY_PARTITION_UPPER_BOUND, "12345");
          }
        };
    assertThrowsExactly(IllegalArgumentException.class, () -> new Options(originals_3, false));

    // 4. correct options
    Map<String, String> originals_4 =
        new HashMap<String, String>() {
          {
            put(Options.URL, "url_test");
            put(Options.GRAPH, "graph_test");
            put(Options.VERSION, TEST_VERSION);
            put(Options.QUERY_VERTEX, "Person");
            put(Options.QUERY_PARTITION_KEY, "length");
            put(Options.QUERY_PARTITION_NUM, "5");
            put(Options.QUERY_PARTITION_LOWER_BOUND, "100");
            put(Options.QUERY_PARTITION_UPPER_BOUND, "1000");
          }
        };
    assertDoesNotThrow(() -> new Options(originals_4, false));
  }

  @Test
  public void testMissingVertexIdFieldThrowsException() {
    Map<String, String> opts = new HashMap<>();
    opts.put(Options.GRAPH, "test_graph");
    opts.put(Options.URL, "http://localhost:8080");
    opts.put(Options.VERSION, "4.1.0");
    opts.put(Options.UPSERT_VERTEX_TYPE, "Person");
    // Missing UPSERT_VERTEX_ID_FIELD

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new Options(opts, false);
            });
    assertTrue(exception.getMessage().contains("upsert.vertex.id.field is required"));
  }

  @Test
  public void testMissingEdgeSourceTypeThrowsException() {
    Map<String, String> opts = new HashMap<>();
    opts.put(Options.GRAPH, "test_graph");
    opts.put(Options.URL, "http://localhost:8080");
    opts.put(Options.VERSION, "4.1.0");
    opts.put(Options.UPSERT_EDGE_TYPE, "KNOWS");
    // Missing required edge fields

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new Options(opts, false);
            });
    assertTrue(exception.getMessage().contains("upsert.edge.source.type is required"));
  }

  @Test
  public void testMissingEdgeTargetIdFieldThrowsException() {
    Map<String, String> opts = new HashMap<>();
    opts.put(Options.GRAPH, "test_graph");
    opts.put(Options.URL, "http://localhost:8080");
    opts.put(Options.VERSION, "4.1.0");
    opts.put(Options.UPSERT_EDGE_TYPE, "KNOWS");
    opts.put(Options.UPSERT_EDGE_SOURCE_TYPE, "Person");
    opts.put(Options.UPSERT_EDGE_SOURCE_ID_FIELD, "source_id");
    opts.put(Options.UPSERT_EDGE_TARGET_TYPE, "Person");
    // Missing UPSERT_EDGE_TARGET_ID_FIELD

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new Options(opts, false);
            });
    assertTrue(exception.getMessage().contains("upsert.edge.target.id.field is required"));
  }

  @Test
  public void testBothVertexAndEdgeUpsertThrowsException() {
    Map<String, String> opts = new HashMap<>();
    opts.put(Options.GRAPH, "test_graph");
    opts.put(Options.URL, "http://localhost:8080");
    opts.put(Options.VERSION, "4.1.0");
    opts.put(Options.UPSERT_VERTEX_TYPE, "Person");
    opts.put(Options.UPSERT_VERTEX_ID_FIELD, "id");
    opts.put(Options.UPSERT_EDGE_TYPE, "KNOWS");

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new Options(opts, false);
            });
    assertTrue(
        exception.getMessage().contains("Cannot specify both vertex and edge upsert options"));
  }

  @Test
  public void testSslKeystoreTypeRequiresKeystorePath() {
    Map<String, String> originals = new HashMap<>(defaultOptions);
    originals.put(Options.SSL_KEYSTORE_TYPE, "PKCS12");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new Options(originals, false));
    assertTrue(exception.getMessage().contains("ssl.keystore is required"));
  }
}
