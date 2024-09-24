# v0.2.1
* Fix the NPE issue when `log.level` is unset. The `log.level` option is optional. If not provided, the connector should initialize the logger using the Spark Log4j configurations
* New writer option `loading.ack` with default value "all": 
  * "all": loading requests will return after all GPE instances have acknowledged the requests 
  * "none": loading requests will return immediately after RESTPP processed the requests
* Change the naming convention of release packages:
  * `tigergraph-spark-connector-<version>.jar` is renamed to `original-tigergraph-spark-connector-<version>.jar` and moved into `tigergraph-spark-connector-<version>.tar.gz`: the JAR file containing only the compiled classes of the connector, which does not include any dependencies.
  * `tigergraph-spark-connector-<version>-jar-with-dependencies.jar` is renamed to `tigergraph-spark-connector-<version>.jar`: the JAR file that includes compiled classes, as well as all the dependencies.

# v0.2.0
* Support reading from TigerGraph to Spark DataFrame: [documentation](https://docs.tigergraph.com/tigergraph-server/current/data-loading/read-to-spark-dataframe)
  * Built-in vertex query
  * Built-in edge query
  * Pre-installed query
  * Interpreted query
* Support built-in logger by setting option `log.level` to 0,1,2 or 3
* Support TigerGraph 4.1+ (Still backward compatible with previous versions)
  * Support requesting and refreshing JWT
  * **NOTE**: it's mandatory to provide TigerGraph version by option `version`, e.g. `.option("version", "4.1.0")`

# v0.1.1
* Fix the issue that failed to load decimal data type

# v0.1.0
* Support writing from Spark DataFrame to TigerGraph: [documentation](https://docs.tigergraph.com/tigergraph-server/current/data-loading/load-from-spark-dataframe)
  * Batch write
  * Write with Spark Structured Streaming API