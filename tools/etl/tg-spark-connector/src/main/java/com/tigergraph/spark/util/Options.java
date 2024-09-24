/**
 * Copyright (c) 2023 TigerGraph Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tigergraph.spark.util;

import com.tigergraph.spark.util.OptionDef.OptionKey;
import com.tigergraph.spark.util.OptionDef.Type;
import com.tigergraph.spark.util.OptionDef.ValidVersion;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Validate and transform Spark DataFrame options(configurations) */
public class Options implements Serializable {

  public static enum OptionType {
    WRITE,
    READ
  }

  /** Refer to {@link com.tigergraph.spark.client.Query} */
  public static enum QueryType {
    GET_VERTICES,
    GET_VERTEX,
    GET_EDGES_BY_SRC_VERTEX,
    GET_EDGES_BY_SRC_VERTEX_EDGE_TYPE,
    GET_EDGES_BY_SRC_VERTEX_EDGE_TYPE_TGT_TYPE,
    GET_EDGE_BY_SRC_VERTEX_EDGE_TYPE_TGT_VERTEX,
    INSTALLED,
    INTERPRETED
  }

  private final OptionType optionType;
  private QueryType queryType;

  public static final String GRAPH = "graph";
  public static final String URL = "url";
  public static final String VERSION = "version";
  public static final String USERNAME = "username";
  public static final String PASSWORD = "password";
  public static final String SECRET = "secret";
  public static final String TOKEN = "token";
  // loading
  public static final String LOADING_JOB = "loading.job";
  public static final String LOADING_FILENAME = "loading.filename";
  public static final String LOADING_SEPARATOR = "loading.separator";
  public static final String LOADING_EOL = "loading.eol";
  public static final String LOADING_BATCH_SIZE_BYTES = "loading.batch.size.bytes";
  public static final String LOADING_TIMEOUT_MS = "loading.timeout.ms";
  public static final String LOADING_ACK = "loading.ack";
  public static final String LOADING_MAX_PERCENT_ERROR = "loading.max.percent.error";
  public static final String LOADING_MAX_NUM_ERROR = "loading.max.num.error";
  public static final String LOADING_RETRY_INTERVAL_MS = "loading.retry.interval.ms";
  public static final String LOADING_MAX_RETRY_INTERVAL_MS = "loading.max.retry.interval.ms";
  public static final String LOADING_MAX_RETRY_ATTEMPTS = "loading.max.retry.attempts";
  // loading - default
  public static final String LOADING_SEPARATOR_DEFAULT = ",";
  public static final String LOADING_EOL_DEFAULT = "\n";
  public static final int LOADING_BATCH_SIZE_BYTES_DEFAULT = 2 * 1024 * 1024; // 2mb
  public static final int LOADING_TIMEOUT_MS_DEFAULT = 0; // restpp default
  public static final String LOADING_ACK_ALL = "all";
  public static final String LOADING_ACK_NONE = "none";
  public static final int LOADING_RETRY_INTERVAL_MS_DEFAULT = 5 * 1000; // 5s
  public static final int LOADING_MAX_RETRY_INTERVAL_MS_DEFAULT = 5 * 60 * 1000; // 5min
  public static final int LOADING_MAX_RETRY_ATTEMPTS_DEFAULT = 10;
  // http transport
  public static final String IO_CONNECT_TIMEOUT_MS = "io.connect.timeout.ms";
  public static final String IO_READ_TIMEOUT_MS = "io.read.timeout.ms";
  public static final String IO_RETRY_INTERVAL_MS = "io.retry.interval.ms";
  public static final String IO_MAX_RETRY_INTERVAL_MS = "io.max.retry.interval.ms";
  public static final String IO_MAX_RETRY_ATTEMPTS = "io.max.retry.attempts";
  // http transport - default
  public static final int IO_CONNECT_TIMEOUT_MS_DEFAULT = 30 * 1000; // 30s
  public static final int IO_READ_TIMEOUT_MS_DEFAULT = 60 * 1000; // 1min
  public static final int IO_RETRY_INTERVAL_MS_DEFAULT = 5 * 1000; // 5s
  public static final int IO_MAX_RETRY_INTERVAL_MS_DEFAULT = 10 * 1000; // 10s
  public static final int IO_MAX_RETRY_ATTEMPTS_DEFAULT = 5;
  // SSL
  public static final String SSL_MODE = "ssl.mode";
  public static final String SSL_MODE_BASIC = "basic";
  public static final String SSL_MODE_VERIFY_CA = "verifyCA";
  public static final String SSL_MODE_VERIFY_HOSTNAME = "verifyHostname";
  public static final String SSL_TRUSTSTORE = "ssl.truststore";
  public static final String SSL_TRUSTSTORE_TYPE = "ssl.truststore.type";
  public static final String SSL_TRUSTSTORE_PASSWORD = "ssl.truststore.password";
  public static final String SSL_TRUSTSTORE_TYPE_DEFAULT = "JKS";

  // Query
  public static final String QUERY_VERTEX = "query.vertex";
  public static final String QUERY_EDGE = "query.edge";
  public static final String QUERY_FIELD_SEPARATOR = "query.field.separator";
  public static final String QUERY_INSTALLED = "query.installed";
  public static final String QUERY_INTERPRETED = "query.interpreted";
  public static final String QUERY_PARAMS = "query.params";
  // row_number:json_key, e.g. 0:vSet which will extract vSet from [{a:1,vSet:[]},{b:2}]
  public static final String QUERY_RESULTS_EXTRACT = "query.results.extract";

  // Query - operator
  public static final String QUERY_OP_SELECT = "query.op.select";
  public static final String QUERY_OP_FILTER = "query.op.filter";
  public static final String QUERY_OP_LIMIT = "query.op.limit";
  public static final String QUERY_OP_SORT = "query.op.sort";
  // Query - request setting
  public static final String QUERY_TIMEOUT_MS = "query.timeout.ms";
  public static final String QUERY_MAX_RESPONSE_BYTES = "query.max.response.bytes";
  // Query - partitioning
  public static final String QUERY_PARTITION_KEY = "query.partition.key";
  public static final String QUERY_PARTITION_NUM = "query.partition.num";
  public static final String QUERY_PARTITION_UPPER_BOUND = "query.partition.upper.bound";
  public static final String QUERY_PARTITION_LOWER_BOUND = "query.partition.lower.bound";

  // Logging
  public static final String LOG_LEVEL = "log.level";
  public static final String LOG_FILE = "log.file";

  // Options' group name
  public static final String GROUP_GENERAL = "general";
  public static final String GROUP_AUTH = "auth";
  public static final String GROUP_LOADING_JOB = "loading.job";
  public static final String GROUP_TRANSPORT_TIMEOUT = "transport.timeout";
  public static final String GROUP_SSL = "ssl";
  public static final String GROUP_QUERY = "query";
  public static final String GROUP_LOG = "log";

  private final HashMap<String, String> originals;
  private final HashMap<String, Serializable> transformed = new HashMap<>();
  private final OptionDef definition;

  public Options(Map<String, String> originals, boolean skipValidate) {
    if (originals == null) throw new IllegalArgumentException("Original option map can't be null.");
    if (originals.containsKey(LOADING_JOB)) {
      this.optionType = OptionType.WRITE;
    } else {
      this.optionType = OptionType.READ;
    }
    this.originals = originals != null ? new HashMap<>(originals) : new HashMap<>();
    this.definition =
        new OptionDef()
            .define(GRAPH, Type.STRING, true, GROUP_GENERAL)
            .define(URL, Type.STRING, true, GROUP_GENERAL)
            .define(
                VERSION,
                Type.STRING,
                OptionDef.DefaultVal.NON_DEFAULT,
                true,
                ValidVersion.INSTANCE,
                GROUP_GENERAL)
            .define(USERNAME, Type.STRING, GROUP_AUTH)
            .define(PASSWORD, Type.STRING, GROUP_AUTH)
            .define(SECRET, Type.STRING, GROUP_AUTH)
            .define(TOKEN, Type.STRING, GROUP_AUTH)
            .define(
                IO_READ_TIMEOUT_MS,
                Type.INT,
                IO_READ_TIMEOUT_MS_DEFAULT,
                true,
                null,
                GROUP_TRANSPORT_TIMEOUT)
            .define(
                IO_CONNECT_TIMEOUT_MS,
                Type.INT,
                IO_CONNECT_TIMEOUT_MS_DEFAULT,
                true,
                null,
                GROUP_TRANSPORT_TIMEOUT)
            .define(
                IO_RETRY_INTERVAL_MS,
                Type.INT,
                IO_RETRY_INTERVAL_MS_DEFAULT,
                true,
                null,
                GROUP_TRANSPORT_TIMEOUT)
            .define(
                IO_MAX_RETRY_INTERVAL_MS,
                Type.INT,
                IO_MAX_RETRY_INTERVAL_MS_DEFAULT,
                true,
                null,
                GROUP_TRANSPORT_TIMEOUT)
            .define(
                IO_MAX_RETRY_ATTEMPTS,
                Type.INT,
                IO_MAX_RETRY_ATTEMPTS_DEFAULT,
                true,
                null,
                GROUP_TRANSPORT_TIMEOUT)
            .define(
                SSL_MODE,
                Type.STRING,
                SSL_MODE_BASIC,
                true,
                OptionDef.ValidString.in(
                    SSL_MODE_BASIC, SSL_MODE_VERIFY_CA, SSL_MODE_VERIFY_HOSTNAME),
                GROUP_SSL)
            .define(SSL_TRUSTSTORE, Type.STRING, null, false, null, GROUP_SSL)
            .define(
                SSL_TRUSTSTORE_TYPE,
                Type.STRING,
                SSL_TRUSTSTORE_TYPE_DEFAULT,
                false,
                null,
                GROUP_SSL)
            .define(SSL_TRUSTSTORE_PASSWORD, Type.STRING, null, false, null, GROUP_SSL)
            .define(LOG_LEVEL, Type.INT, GROUP_LOG)
            .define(LOG_FILE, Type.STRING, "", false, null, GROUP_LOG);

    if (OptionType.WRITE.equals(this.optionType)) {
      this.definition
          .define(LOADING_JOB, Type.STRING, true, GROUP_LOADING_JOB)
          .define(LOADING_FILENAME, Type.STRING, true, GROUP_LOADING_JOB)
          .define(
              LOADING_SEPARATOR,
              Type.STRING,
              LOADING_SEPARATOR_DEFAULT,
              true,
              null,
              GROUP_LOADING_JOB)
          .define(LOADING_EOL, Type.STRING, LOADING_EOL_DEFAULT, true, null, GROUP_LOADING_JOB)
          .define(
              LOADING_BATCH_SIZE_BYTES,
              Type.INT,
              LOADING_BATCH_SIZE_BYTES_DEFAULT,
              true,
              null,
              GROUP_LOADING_JOB)
          .define(
              LOADING_TIMEOUT_MS,
              Type.INT,
              LOADING_TIMEOUT_MS_DEFAULT,
              true,
              null,
              GROUP_LOADING_JOB)
          .define(
              LOADING_ACK,
              Type.STRING,
              LOADING_ACK_ALL,
              true,
              OptionDef.ValidString.in(LOADING_ACK_ALL, LOADING_ACK_NONE),
              GROUP_LOADING_JOB)
          .define(LOADING_MAX_PERCENT_ERROR, Type.DOUBLE, GROUP_LOADING_JOB)
          .define(LOADING_MAX_NUM_ERROR, Type.INT, GROUP_LOADING_JOB)
          .define(
              LOADING_RETRY_INTERVAL_MS,
              Type.INT,
              LOADING_RETRY_INTERVAL_MS_DEFAULT,
              true,
              null,
              GROUP_LOADING_JOB)
          .define(
              LOADING_MAX_RETRY_INTERVAL_MS,
              Type.INT,
              LOADING_MAX_RETRY_INTERVAL_MS_DEFAULT,
              true,
              null,
              GROUP_LOADING_JOB)
          .define(
              LOADING_MAX_RETRY_ATTEMPTS,
              Type.INT,
              LOADING_MAX_RETRY_ATTEMPTS_DEFAULT,
              true,
              null,
              GROUP_LOADING_JOB);
    } else if (OptionType.READ.equals(this.optionType)) {
      this.definition
          .define(QUERY_VERTEX, Type.STRING, false, GROUP_QUERY)
          .define(QUERY_EDGE, Type.STRING, false, GROUP_QUERY)
          .define(QUERY_FIELD_SEPARATOR, Type.STRING, ".", false, null, GROUP_QUERY)
          .define(QUERY_INSTALLED, Type.STRING, false, GROUP_QUERY)
          .define(QUERY_INTERPRETED, Type.STRING, false, GROUP_QUERY)
          .define(QUERY_PARAMS, Type.STRING, "", false, null, GROUP_QUERY)
          .define(QUERY_RESULTS_EXTRACT, Type.STRING, "", false, null, GROUP_QUERY)
          .define(QUERY_OP_SELECT, Type.STRING, false, GROUP_QUERY)
          .define(QUERY_OP_FILTER, Type.STRING, false, GROUP_QUERY)
          .define(QUERY_OP_LIMIT, Type.LONG, false, GROUP_QUERY)
          .define(QUERY_OP_SORT, Type.STRING, false, GROUP_QUERY)
          .define(QUERY_TIMEOUT_MS, Type.INT, 0, true, null, GROUP_QUERY)
          .define(QUERY_MAX_RESPONSE_BYTES, Type.LONG, 0L, true, null, GROUP_QUERY)
          .define(QUERY_PARTITION_KEY, Type.STRING, false, GROUP_QUERY)
          .define(QUERY_PARTITION_NUM, Type.INT, false, GROUP_QUERY)
          .define(QUERY_PARTITION_UPPER_BOUND, Type.STRING, false, GROUP_QUERY)
          .define(QUERY_PARTITION_LOWER_BOUND, Type.STRING, false, GROUP_QUERY);
    }

    if (!skipValidate) {
      this.validate();
    }

    if (OptionType.READ.equals(this.optionType)) {
      this.parseQueryType();
    }
  }

  /**
   * validate all the Options on their type and validator, then put the parsed value into map
   * 'transformed'.
   *
   * <p>Visible for testing
   *
   * @return the errors of validation, If the returned List's size is 0, there is no validation
   *     error.
   */
  protected List<OptionError> validateOpts() {
    List<OptionError> errors = new ArrayList<>();
    this.definition
        .optionKeys()
        .forEach(
            (k, v) -> {
              OptionError err = null;
              String key = v.name;
              Serializable value = null;
              try {
                value = this.parse(v.name);
                transformed.put(key, value);
              } catch (Exception e) {
                err = new OptionError(key, value, e.getMessage());
              }
              try {
                if (v.validator != null && this.containsOption(key)) {
                  v.validator.ensureValid(key, value);
                }
              } catch (Exception e) {
                if (err == null) {
                  err = new OptionError(key, value, e.getMessage());
                } else {
                  err.getErrorMsgs().add(e.getMessage());
                }
              }
              if (err != null) {
                errors.add(err);
              }
            });
    return errors;
  }

  public void validate() {
    List<OptionError> errors = validateOpts();
    if (errors != null && errors.size() > 0) {
      throw new IllegalArgumentException(
          "Invalid input options: "
              + errors.stream().map(e -> e.toString()).reduce(". ", String::concat));
    }

    sanityCheck();
  }

  /**
   * Determine if the Option is contained. The required option will be considered to always exist.
   */
  public boolean containsOption(String key) {
    if (!this.originals.containsKey(key)) {
      if (this.definition.optionKeys().containsKey(key)) {
        OptionKey optionKey = this.definition.optionKeys().get(key);
        if (optionKey.required || optionKey.hasDefault()) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  private void sanityCheck() {
    if (OptionType.READ.equals(optionType)) {
      sanityCheckPartitionQueryOpts();
    }
  }

  private void sanityCheckPartitionQueryOpts() {
    // all those options are required for partitioned query
    if ((containsOption(QUERY_PARTITION_KEY)
            || containsOption(QUERY_PARTITION_NUM)
            || containsOption(QUERY_PARTITION_LOWER_BOUND)
            || containsOption(QUERY_PARTITION_UPPER_BOUND))
        && !(containsOption(QUERY_PARTITION_KEY)
            && containsOption(QUERY_PARTITION_NUM)
            && containsOption(QUERY_PARTITION_LOWER_BOUND)
            && containsOption(QUERY_PARTITION_UPPER_BOUND))) {
      throw new IllegalArgumentException(
          "To run partitioned queries, option "
              + QUERY_PARTITION_KEY
              + ", "
              + QUERY_PARTITION_NUM
              + ", "
              + QUERY_PARTITION_LOWER_BOUND
              + "and "
              + QUERY_PARTITION_UPPER_BOUND
              + " are all required");
    }

    if (containsOption(QUERY_PARTITION_KEY)) {
      BigInteger lower;
      try {
        lower = new BigInteger(getString(QUERY_PARTITION_LOWER_BOUND));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Please provide an integer for " + QUERY_PARTITION_LOWER_BOUND);
      }
      BigInteger upper;
      try {
        upper = new BigInteger(getString(QUERY_PARTITION_UPPER_BOUND));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Please provide an integer value for " + QUERY_PARTITION_UPPER_BOUND);
      }

      if (lower.compareTo(upper) >= 0) {
        throw new IllegalArgumentException(
            QUERY_PARTITION_LOWER_BOUND + " should be less than " + QUERY_PARTITION_UPPER_BOUND);
      }
    }
  }

  /**
   * Gets the value of the specified Option, converts the original value to the corresponding type,
   * but does not attempt to convert the default value.
   *
   * @param key the name of Option
   * @return the value of Option
   */
  private Serializable parse(String key) {
    if (this.definition.optionKeys().containsKey(key)) {
      OptionKey optionKey = this.definition.optionKeys().get(key);
      if (this.originals.containsKey(key)) {
        String value = this.originals.get(key);
        String trimmed = null;
        if (value != null) {
          trimmed = value.trim();
        }
        Type type = optionKey.type;
        try {
          switch (type) {
            case BOOLEAN:
              if (trimmed != null && trimmed.equalsIgnoreCase("true")) return Boolean.TRUE;
              else if (trimmed != null && trimmed.equalsIgnoreCase("false")) return Boolean.FALSE;
              else throw new IllegalArgumentException("Expected value to be either true or false");
            case STRING:
              return value;
            case INT:
              return Integer.parseInt(trimmed);
            case LONG:
              return Long.parseLong(trimmed);
            case DOUBLE:
              return Double.parseDouble(trimmed);
            default:
              throw new IllegalStateException("Unknown type.");
          }
        } catch (Exception e) {
          throw new IllegalArgumentException(
              "Option("
                  + key
                  + ") failed to convert the value to type "
                  + type
                  + ", error is "
                  + e.toString());
        }
      } else {
        if (optionKey.hasDefault()) {
          return optionKey.defaultValue;
        } else if (optionKey.required) {
          throw new IllegalArgumentException(
              "Option(" + key + ") has no default value and has not been set to a value");
        } else {
          return null;
        }
      }
    } else {
      throw new IllegalArgumentException("Option(" + key + ") is not defined");
    }
  }

  private void parseQueryType() {
    if (containsOption(QUERY_VERTEX)) {
      switch (Utils.countQueryFields(
          getString(QUERY_VERTEX), getString(Options.QUERY_FIELD_SEPARATOR))) {
        case 1:
          queryType = QueryType.GET_VERTICES;
          break;
        case 2:
          queryType = QueryType.GET_VERTEX;
          break;
        default:
          throw new IllegalArgumentException(
              "Invalid read option: " + QUERY_VERTEX + " -> " + getString(QUERY_VERTEX));
      }
    } else if (containsOption(QUERY_EDGE)) {
      switch (Utils.countQueryFields(
          getString(QUERY_EDGE), getString(Options.QUERY_FIELD_SEPARATOR))) {
        case 2:
          queryType = QueryType.GET_EDGES_BY_SRC_VERTEX;
          break;
        case 3:
          queryType = QueryType.GET_EDGES_BY_SRC_VERTEX_EDGE_TYPE;
          break;
        case 4:
          queryType = QueryType.GET_EDGES_BY_SRC_VERTEX_EDGE_TYPE_TGT_TYPE;
          break;
        case 5:
          queryType = QueryType.GET_EDGE_BY_SRC_VERTEX_EDGE_TYPE_TGT_VERTEX;
          break;
        default:
          throw new IllegalArgumentException(
              "Invalid read option: " + QUERY_EDGE + " -> " + getString(QUERY_EDGE));
      }
    } else if (containsOption(QUERY_INSTALLED)) {
      queryType = QueryType.INSTALLED;
    } else if (containsOption(QUERY_INTERPRETED)) {
      queryType = QueryType.INTERPRETED;
    } else {
      throw new IllegalArgumentException(
          String.format(
              "Unknown query type, please provide a valid value from %s, %s, %s or %s.",
              QUERY_VERTEX, QUERY_EDGE, QUERY_INSTALLED, QUERY_INTERPRETED));
    }
  }

  /**
   * Retrieve the value from transformed option map. Retrieve it from the original options if not in
   * transformed map.
   *
   * @param key
   */
  public Object get(String key) {
    if (transformed.containsKey(key)) {
      return transformed.get(key);
    } else if (originals.containsKey(key)) {
      return originals.get(key);
    } else {
      return null;
    }
  }

  public String getString(String key) {
    return (String) get(key);
  }

  public Integer getInt(String key) {
    return (Integer) get(key);
  }

  public Long getLong(String key) {
    return (Long) get(key);
  }

  public Double getDouble(String key) {
    return (Double) get(key);
  }

  public Boolean getBoolean(String key) {
    return (Boolean) get(key);
  }

  public Map<String, String> getOriginals() {
    return originals;
  }

  public OptionType getOptionType() {
    return this.optionType;
  }

  public QueryType getQueryType() {
    return this.queryType;
  }
}
