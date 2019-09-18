# TigerGraph JDBC Driver

The TigerGraph JDBC Driver is a [Type 4 Driver](https://en.wikipedia.org/wiki/JDBC_driver), converting JDBC calls directly into TigerGraph HTTP REST calls. This JDBC driver supports to ingest data into TigerGraph and read query result from TigerGraph. The JDBC driver supports the access to TigerGraph's builtin queries, loading jobs, compiled queries (i.e., queries must be compiled and installed before being invoked via the JDBC driver) and interpreted queries (i.e., ad hoc queries, without needing to compile and install the queries beforehand). Via TigerGraph JDBC Driver, an instantiation of Spark Connector is also provided, allowing spark programs to write data into TigerGraph, and read query results from TigerGraph.

## Versions compatibility

| JDBC Version | TigerGraph Version | Java | Protocol | Query Result Format | New Features |
| --- | --- | --- | --- | --- | --- |
| 1.0 | 2.2.4+ | 1.8 | Rest++ | JSON | Support builtin, compiled queries and loading jobs |
| 1.1 | 2.4.1+ | 1.8 | Rest++ | ResultSet | Support tabular format and Spark |
| 1.2 | 2.4.1+ | 1.8 | Rest++ | ResultSet | Support interpreted queries and Spark partitioning |

## Minimum viable snippet
Parameters are passed as properties when creating a connection, such as username, password and graph name. Once REST++ authentication is enabled, username and password is mandatory. Graph name is required when MultiGraph is enabled.

You may specify IP address and port as needed, and the port is the one used by GraphStudio. Please change 'http' to 'https' when SSL is enabled.

For each ResultSet, there might be several tables with different tabular formats. **'isLast()' was overridden to switch to the next table. Otherwise, only the first table could be retrieved.**

```
Properties properties = new Properties();
properties.put("username", "tigergraph");
properties.put("password", "tigergraph");
properties.put("graph", "gsql_demo");

try {
  com.tigergraph.jdbc.Driver driver = new Driver();
  try (Connection con =
      driver.connect("jdbc:tg:http://127.0.0.1:14240", properties)) {
    try (Statement stmt = con.createStatement()) {
      String query = "builtins stat_vertex_number";
      try (java.sql.ResultSet rs = stmt.executeQuery(query)) {
        do {
          java.sql.ResultSetMetaData metaData = rs.getMetaData();
          // Gets the name of the designated column (1-based indexing)
          System.out.print(metaData.getColumnName(1));
          for (int i = 2; i <= metaData.getColumnCount(); ++i) {
            System.out.print("\t" + metaData.getColumnName(i));
          }
          System.out.println("");
          while (rs.next()) {
            System.out.print(rs.getObject(1));
            for (int i = 2; i <= metaData.getColumnCount(); ++i) {
              Object obj = rs.getObject(i);
              System.out.println("\t" + String.valueOf(obj));
            }
          }
        } while (!rs.isLast());
      }
    }
  }
}
```

## Supported Queries
```
// Run a pre-installed query with parameters (example: the pageRank query from the GSQL Demo Examples)
run pageRank(maxChange=?, maxIteration=?, dampingFactor=?)

// Get the number of vertices of a specific type
builtins stat_vertex_number(type=?)

// Get the number of edges
builtins stat_edge_number

// Get the number of edges of a specific type
builtins stat_edge_number(type=?)

// Get any k vertices of a specified type (example: Page type vertex)
get Page(limit=?)

// Get a vertex which has the given id (example: Page type vertex)
get Page(id=?)

// Get all vertices which satisfy the given filter (example: Page type vertex)
get Page(filter=?)

// Get all edges whose source vertex has the specified type and id (example: Page vertex with id)
get edges(Page, id)

// Get a specific edge from a given vertex to another specific vertex
// (example: from a Page vertex, across a Linkto edge, to a Page vertex)
get edge(Page, id1, Linkto, Page, id2)

// Run a pre-installed query with parameters
run pageRank(maxChange=?, maxIteration=?, dampingFactor=?)

// Run an interpreted query
run interpreted(a=?)

// Run a pre-installed loading job
INSERT INTO job load_pagerank(line) VALUES(?)

// Insert into a vertex type
INSERT INTO vertex Page(id, page_id) VALUES(?, ?)

// Insert into edge type
INSERT INTO edge Linkto(Page, Page) VALUES(?, ?)
```
See [RESTPP API User Guide: Built-in Endpoints](https://docs.tigergraph.com/dev/restpp-api/built-in-endpoints) for more details about the built-in endpoints.

The default timeout for TigerGraph is 16s, you can use **setQueryTimeout(seconds)** to change timeout for any specific query.

Detailed examples can be found at [tg-jdbc-examples](tg-jdbc-examples).

## Run examples
There are 4 demo applications. All of them take 3 parameters: IP address, port, debug. The default IP address is 127.0.0.1, and the default port is 14240. Other values can be specified as needed.

Debug mode:
> 0: do not print any debug information

> 1: print basic debug information (e.g., request received, request sent to TigerGraph, response gotten from TigerGraph)

> 2: print detailed debug information

To run the examples, first clone the repository, then compile and run the examples like the following:

```
mvn clean && mvn install
cd tg-jdbc-examples
mvn exec:java -Dexec.mainClass=com.tigergraph.jdbc.examples.Builtins -Dexec.args="127.0.0.1 14240 1"
mvn exec:java -Dexec.mainClass=com.tigergraph.jdbc.examples.GraphQuery -Dexec.args="127.0.0.1 14240 1"
mvn exec:java -Dexec.mainClass=com.tigergraph.jdbc.examples.RunQuery -Dexec.args="127.0.0.1 14240 1"
mvn exec:java -Dexec.mainClass=com.tigergraph.jdbc.examples.UpsertQuery -Dexec.args="127.0.0.1 14240 1"
```

## How to use in Apache Spark
### To read from TigerGraph:
```
// read vertex
val jdbcDF1 = spark.read.format("jdbc").options(
  Map(
    "driver" -> "com.tigergraph.jdbc.Driver",
    "url" -> "jdbc:tg:http://127.0.0.1:14240",
    "username" -> "tigergraph",
    "password" -> "tigergraph",
    "graph" -> "gsql_demo", // graph name
    "dbtable" -> "vertex Page", // vertex type
    "limit" -> "10", // number of vertices to retrieve
    "debug" -> "0")).load()
jdbcDF1.show

// read edge
val jdbcDF2 = spark.read.format("jdbc").options(
  Map(
    "driver" -> "com.tigergraph.jdbc.Driver",
    "url" -> "jdbc:tg:http://127.0.0.1:14240",
    "username" -> "tigergraph",
    "password" -> "tigergraph",
    "graph" -> "gsql_demo", // graph name
    "dbtable" -> "edge Linkto", // edge type
    "limit" -> "10", // number of edges to retrieve
    "source" -> "3", // source vertex id
    "debug" -> "0")).load()
jdbcDF2.show

// invoke pre-intalled query
val jdbcDF3 = spark.read.format("jdbc").options(
  Map(
    "driver" -> "com.tigergraph.jdbc.Driver",
    "url" -> "jdbc:tg:http://127.0.0.1:14240",
    "username" -> "tigergraph",
    "password" -> "tigergraph",
    "graph" -> "gsql_demo", // graph name
    "dbtable" -> "query pageRank(maxChange=0.001, maxIteration=10, dampingFactor=0.15)", // query name & parameters
    "debug" -> "0")).load()
jdbcDF3.show
```
**When retrieving wildcard edges, option "src_vertex_type" must be specified.**

### To write to TigerGraph:
```
val dataList: List[(Integer, Integer)] = List(
  (4,4),
  (5,5),
  (6,6),
  (7,7))

val colArray: Array[String] = Array("id", "account")

val df = dataList.toDF(colArray: _*)

// write vertices
df.write.mode("overwrite").format("jdbc").options(
  Map(
    "driver" -> "com.tigergraph.jdbc.Driver",
    "url" -> "jdbc:tg:http://127.0.0.1:14240",
    "username" -> "tigergraph",
    "password" -> "tigergraph",
    "graph" -> "gsql_demo", // graph name
    "dbtable" -> "vertex Person", // vertex type
    "batchsize" -> "100",
    "debug" -> "0")).save()

val dataList2: List[(Integer, Integer, Integer)] = List(
  (4,5,1),
  (5,6,1),
  (7,10,1))

val colArray2: Array[String] = Array("Person", "Person", "weight")

val df2 = dataList2.toDF(colArray2: _*)

// write edges
df2.write.mode("overwrite").format("jdbc").options(
  Map(
    "driver" -> "com.tigergraph.jdbc.Driver",
    "url" -> "jdbc:tg:http://127.0.0.1:14240",
    "username" -> "tigergraph",
    "password" -> "tigergraph",
    "graph" -> "gsql_demo", // graph name
    "dbtable" -> "edge Follow", // edge type
    "batchsize" -> "100",
    "debug" -> "0")).save()

// invoke loading job
df2.write.mode("overwrite").format("jdbc").options(
  Map(
    "driver" -> "com.tigergraph.jdbc.Driver",
    "url" -> "jdbc:tg:http://127.0.0.1:14240",
    "username" -> "tigergraph",
    "password" -> "tigergraph",
    "graph" -> "gsql_demo", // graph name
    "dbtable" -> "job load_pagerank", // loading job name
    "filename" -> "f", // filename defined in the loading job
    "sep" -> ",", // separator between columns
    "eol" -> ";", // End Of Line
    "schema" -> colArray2.mkString(","), // column definitions
    "batchsize" -> "100",
    "debug" -> "0")).save()
```

### To load data from files
```
val df = sc.textFile("/path/to/your_file", 100).toDF()

// invoke loading job
df.write.mode("append").format("jdbc").options(
  Map(
    "driver" -> "com.tigergraph.jdbc.Driver",
    "url" -> "jdbc:tg:http://127.0.0.1:14240",
    "username" -> "tigergraph",
    "password" -> "tigergraph",
    "graph" -> "ldbc_snb",
    "dbtable" -> "job load_ldbc_snb", // loading job name
    "filename" -> "v_comment_file", // filename defined in the loading job
    "sep" -> "|", // separator between columns
    "eol" -> "\n", // End Of Line
    "schema" -> "value", // column definition, each line only has one column
    "batchsize" -> "10000",
    "debug" -> "0")).save()

```
**For the sake of performance, please do NOT split columns when loading from files.**
For the **"batchsize"** option, if it is set too small, lots of time will be spent on setting up connections; if it is too large, the http payload may exceed limit (the default TigerGraph restpp maximum payload size is 128MB). Furthermore, large "batchsize" may result in high jitter performance.

To bypass the disk IO limitation, it is better to put the raw data file on a different disk other than the one used by TigerGraph.

### To read vertices with Spark partitioning enabled
**"account"** is a numeric attribute of vertex type **"Person"**.
```
val jdbcDF1 = spark.read.format("jdbc").options(
  Map(
    "driver" -> "com.tigergraph.jdbc.Driver",
    "url" -> "jdbc:tg:http://127.0.0.1:14240",
    "username" -> "tigergraph",
    "password" -> "tigergraph",
    "graph" -> "gsql_demo", // graph name
    "dbtable" -> "vertex Person", // vertex type
    "partitionColumn" -> "account", // a numeric vertex attribute
    "lowerBound" -> "0",
    "upperBound" -> "100",
    "numPartitions" -> "10",
    "debug" -> "0")).load()
jdbcDF1.show
```

### To invoke interpreted queries:
```
val dbtable1 = """interpreted(a=10, b=20) INTERPRET QUERY (int a, int b) FOR GRAPH gsql_demo {
  PRINT a, b;
}"""

val jdbcDF2 = spark.read.format("jdbc").options(
  Map(
    "driver" -> "com.tigergraph.jdbc.Driver",
    "url" -> "jdbc:tg:http://127.0.0.1:14240",
    "username" -> "tigergraph",
    "password" -> "tigergraph",
    "dbtable" -> dbtable1,
    "debug" -> "0")).load()
jdbcDF2.show
```

### To invoke interpreted queries with Spark partitioning enabled
**"account"** is a numeric attribute of vertex type **"Person"**, and **the queries' output must contain this attribute**, otherwise Spark will panic.
```
val dbtable2 = """interpreted(partitionColumn=account) INTERPRET QUERY (string partitionColumn, int lowerBound = 0, int upperBound = 100, int topK = 9999999999) FOR GRAPH gsql_demo {
  V0 = {Person.*};

  V1 = SELECT s FROM V0:s
       WHERE s.getAttr(partitionColumn) >= lowerBound and s.getAttr(partitionColumn) < upperBound
       limit topK;

  PRINT V1;
}"""

val jdbcDF3 = spark.read.format("jdbc").options(
  Map(
    "driver" -> "com.tigergraph.jdbc.Driver",
    "url" -> "jdbc:tg:http://127.0.0.1:14240",
    "username" -> "tigergraph",
    "password" -> "tigergraph",
    "dbtable" -> dbtable2,
    "partitionColumn" -> "account", // a numeric vertex attribute
    "lowerBound" -> "0",
    "upperBound" -> "100",
    "numPartitions" -> "10",
    "debug" -> "0")).load()
jdbcDF3.show
```

**"username"** and **"password"** need to be provided if authentication is enabled. **"schema"** (i.e., column definitions) needs to be specified when invoking loading jobs.

For compiled and interpreted queries that need to be invoked by Spark, it is better to have a parameter named **"topK"** to limit the number of results returned, as the example shown above. As Spark will call the queries twice, firstly it will invoke the queries to get the results' schema, then it will call the queries again to retrieve data. We are working on a feature to retrieve queries' output schema without running them, which is more efficient and is supposed to be available on TigerGraph v3.0. We will update this driver accordingly in the near future.

To support partitioning, the queries must have parameters **"lowerBound"** and **"upperBound"**, and their default values should be set to minimum and maximum values of the corresponding attribute respectively. **getAttr()** will be supported on TigerGraph v3.0, before that you can use hard code attributes instead of passing as a parameter, like this:
```
val dbtable2 = """interpreted INTERPRET QUERY (int lowerBound = 0, int upperBound = 100, int topK = 9999999999) FOR GRAPH gsql_demo {
  V0 = {Person.*};

  V1 = SELECT s FROM V0:s
       WHERE s.account >= lowerBound and s.account < upperBound
       limit topK;

  PRINT V1;
}"""
```

Save any piece of the above script in a file, and run it like this:
```
/path/to/spark/bin/spark-shell --jars /path/to/tg-jdbc-driver-1.2.jar -i read.scala
```

**Please do NOT print multiple objects (i.e., variable list, vertex set, edge set, etc.) in your gquery if it needs to be invoked via Spark. Otherwise, only one object could be printed. The output format of TigerGraph is JSON, which is an unordered collection of key/value pairs. So the order could not be guaranteed.**

### Load balancing
For TigerGraph clusters, all the machines' ip addresses (separated by a comma) could be passed via option **"ip_list"** to the driver, and the driver will pick one ip randomly to issue the query.

### Supported dbTable format when used in Spark
| Operator | Parameters |
| --- | --- |
| vertex | vertex_type[(param)] |
| edge | edge_type[(param)] |
| job | loading_jobname |
| query | query_name[(param)] |
| interpreted | [(param)] query_body |

### Supported SaveMode when used in Spark
The default behavior of saving a DataFrame to TigerGraph is **upsert**:
> when the vertex/edge exists, it will be updated.

> otherwise, a new vertex/edge will be created.

We do have other modes, like only update graph when the corresponding vertex/edge exists and do not create any new vertex/edge. But sadly it seems this mode cannot be mapped to any Spark SaveMode directly.

## How to use in Python
The JDBC driver could be used in Python via pyspark. 'pyspark' needs to be installed first:
```
sudo pip install pypandoc pyspark
```

Then you can read from/write to TigerGraph in Python like this:
```
from pyspark.sql import SparkSession
from pyspark.sql.types import StructType, StructField
from pyspark.sql.types import StringType, IntegerType

spark = SparkSession.builder \
  .appName("TigerGraphAnalysis") \
  .config("spark.driver.extraClassPath", "/path/to/spark-2.x.x-bin-hadoop2.x/jars/*:/path/to/tg-jdbc-driver-1.2.jar") \
  .getOrCreate()

# read vertex
jdbcDF = spark.read \
  .format("jdbc") \
  .option("driver", "com.tigergraph.jdbc.Driver") \
  .option("url", "jdbc:tg:http://127.0.0.1:14240") \
  .option("user", "tigergraph") \
  .option("password", "tigergraph") \
  .option("graph", "gsql_demo") \
  .option("dbtable", "vertex Page") \
  .option("limit", "10") \
  .option("debug", "0") \
  .load()

jdbcDF.show()

# read edge
jdbcDF = spark.read \
  .format("jdbc") \
  .option("driver", "com.tigergraph.jdbc.Driver") \
  .option("url", "jdbc:tg:http://127.0.0.1:14240") \
  .option("user", "tigergraph") \
  .option("password", "tigergraph") \
  .option("graph", "gsql_demo") \
  .option("dbtable", "edge Linkto") \
  .option("limit", "10") \
  .option("source", "3") \
  .option("debug", "0") \
  .load()

jdbcDF.show()

# invoke pre-intalled query
jdbcDF = spark.read \
  .format("jdbc") \
  .option("driver", "com.tigergraph.jdbc.Driver") \
  .option("url", "jdbc:tg:http://127.0.0.1:14240") \
  .option("user", "tigergraph") \
  .option("password", "tigergraph") \
  .option("graph", "gsql_demo") \
  .option("dbtable", "query pageRank(maxChange=0.001, maxIteration=10, dampingFactor=0.15)") \
  .option("debug", "0") \
  .load()

jdbcDF.show()

# write vertices
schema = StructType([
  StructField("id", IntegerType(), True),
  StructField("account", IntegerType(), True)])
data = [(8, 8), (9, 9)]
jdbcDF = spark.createDataFrame(data, schema)
print(jdbcDF)
jdbcDF.show()
jdbcDF.write \
  .mode("overwrite") \
  .format("jdbc") \
  .option("driver", "com.tigergraph.jdbc.Driver") \
  .option("url", "jdbc:tg:http://127.0.0.1:14240") \
  .option("user", "tigergraph") \
  .option("password", "tigergraph") \
  .option("graph", "gsql_demo") \
  .option("dbtable", "vertex Person") \
  .option("debug", "1") \
  .save()

# write edges
schema = StructType([
  StructField("Person", IntegerType(), True),
  StructField("Person", IntegerType(), True),
  StructField("weight", IntegerType(), True)])
data = [(4,5,1), (5,6,1)]
jdbcDF = spark.createDataFrame(data, schema)
print(jdbcDF)
jdbcDF.show()
jdbcDF.write \
  .mode("overwrite") \
  .format("jdbc") \
  .option("driver", "com.tigergraph.jdbc.Driver") \
  .option("url", "jdbc:tg:http://127.0.0.1:14240") \
  .option("user", "tigergraph") \
  .option("password", "tigergraph") \
  .option("graph", "gsql_demo") \
  .option("dbtable", "edge Follow") \
  .option("debug", "1") \
  .save()

# invoke loading job
jdbcDF.write \
  .mode("overwrite") \
  .format("jdbc") \
  .option("driver", "com.tigergraph.jdbc.Driver") \
  .option("url", "jdbc:tg:http://127.0.0.1:14240") \
  .option("user", "tigergraph") \
  .option("password", "tigergraph") \
  .option("graph", "gsql_demo") \
  .option("dbtable", "job load_pagerank") \
  .option("filename", "f") \
  .option("sep", ",") \
  .option("eol", ";") \
  .option("schema", "Person,Person,weight") \
  .option("batchsize", "100") \
  .option("debug", "1") \
  .save()
```

Sometimes it may complain that "Incompatible Jackson version: 2.x.x". You may add the following code to [tg-jdbc-driver/pom.xml](tg-jdbc-driver/pom.xml) and recompile the jar package. (It will make the jar package much bigger, so we don't add this by default)
```
  <dependency>
    <groupId>com.fasterxml.jackson.module</groupId>
    <artifactId>jackson-module-scala_2.11</artifactId>
    <version>2.6.5</version>
  </dependency>
```

## Limitation of ResultSet
The response packet size from the TigerGraph server should be less than 2GB, which is the largest response size supported by the TigerGraph Restful API.
