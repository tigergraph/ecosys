# TigerGraph JDBC Driver

The TigerGraph JDBC Driver is a Type 4 driver, converting JDBC calls directly into TigerGraph database commands. This driver supports TigerGraph builtin queries, loading jobs, compiled queries (i.e., queries which has been installed to the GSQL server) and interpreted queries (i.e., ad hoc queries, without needing to compile and install the queries beforehand). The driver will then talk to TigerGraph's REST++ server to run queries and get their results.

## Table of Contents
- [Versions](#versions)
- [Download from Maven Central Repository](#download-from-maven-central-repository)
- [Minimum viable snippet](#minimum-viable-snippet)
- [Quick Start](#quick-start)
  * [Prerequisite](#prerequisite)
  * [Examples](#examples)
- [Authentication](#authentication)
  * [Authentication for Tigergraph Cloud](#authentication-for-tigergraph-cloud)
- [Support SSL](#support-ssl)
  * [Certificate Format](#certificate-format)
  * [Server Authentication](#server-authentication)
  * [Client Authentication](#client-authentication)
- [Connection Pool](#connection-pool)
- [Supported Queries and Syntax](#supported-queries-and-syntax)
- [How to use in Apache Spark](#how-to-use-in-apache-spark)
  * [To read from TigerGraph](#to-read-from-tigergraph)
  * [To write to TigerGraph](#to-write-to-tigergraph)
  * [To load data from files](#to-load-data-from-files)
  * [To read vertices with Spark partitioning enabled](#to-read-vertices-with-spark-partitioning-enabled)
  * [To invoke interpreted queries](#to-invoke-interpreted-queries)
  * [To invoke interpreted queries with Spark partitioning enabled](#to-invoke-interpreted-queries-with-spark-partitioning-enabled)
  * [To enable SSL with Spark](#to-enable-ssl-with-spark)
  * [Load balancing](#load-balancing)
  * [Supported dbTable format when used in Spark](#supported-dbtable-format-when-used-in-spark)
  * [Supported SaveMode when used in Spark](#supported-savemode-when-used-in-spark)
- [How to use it in Python](#how-to-use-it-in-python)
- [Optimizations](#optimizations)
- [Limitation of ResultSet](#limitation-of-resultset)
- [Logging Configuration](#logging-configuration)
- [Password Sealing](#password-sealing)
- [Connection Properties](#connection-properties)
- [FAQ](#faq)
## Versions
Please refer to [CHANGELOG](CHANGELOG.md)

## Download from Maven Central Repository

Tigergraph JDBC Driver can be found at [maven.org](https://search.maven.org/artifact/com.tigergraph/tigergraph-jdbc-driver).

Please refer to [Versions](#versions) to find the **suitable version**.

* Use it in your java code:

  * Apache Maven

    ```xml
    <dependency>
      <groupId>com.tigergraph</groupId>
      <artifactId>tigergraph-jdbc-driver</artifactId>
      <version>{SUITABLE_VERSION}</version>
    </dependency>
    ```

  * Gradle Groovy DSL

    ```groovy
    implementation 'com.tigergraph:tigergraph-jdbc-driver:{SUITABLE_VERSION}'
    ```

  Your build automation tool will download it automatically from maven central repository.

* Use the jar file directly (e.g. include the JDBC driver for Spark):

    Go to [maven.org](https://search.maven.org/artifact/com.tigergraph/tigergraph-jdbc-driver) to get the jar file.

## Minimum viable snippet

Parameters are passed as properties when creating a connection, such as username, password and graph name. Once REST++ authentication is enabled, username and password is mandatory. Graph name is required when MultiGraph is enabled.

You may specify IP address and port as needed, and the port is the one used by GraphStudio. For the other connection properties, please refer to [Connection Properties](#connection-properties).

Please make sure the tigergraph version is specified.

For each ResultSet, there might be several tables with different tabular formats. **'isLast()' could be used to switch to the next table.**

```
Properties properties = new Properties();
properties.put("username", "tigergraph");
properties.put("password", "tigergraph");
properties.put("graph", "gsql_demo");
properties.put("version", "3.4");

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

## Quick Start
### Prerequisite
Please [download the example graphs](https://docs.tigergraph.com/gsql-ref/current/appendix/_attachments/example_graphs.tar.gz), go into the folder `social_net` and run the following command: 
```
gsql graph_create.gsql
```
This creates the graph `Social_Net` with vertex `Person`, vertex `Post`, edge `Friend`, edge `Posted` and edge `Liked`, and loads sample data.
### Examples
The [tg-jdbc-examples](tg-jdbc-examples) demonstrate some common uses of tigergraph-jdbc-driver:
* Builtin Fuctions([JAVA](tg-jdbc-examples/src/main/java/com/tigergraph/jdbc/examples/Builtins.java)): TigerGraph builtin queries to get the stats of vertices/edges.
* Retrieve/Upsert/Delete vertices/edges([JAVA](tg-jdbc-examples/src/main/java/com/tigergraph/jdbc/examples/CRUD.java)): basic ways you can operate on the graph data. Currently it's not supported to retrieve data larger than 2GB. **It's recommended to use a loading job instead directly upserting for large amounts of data.**
* Interpreted/Installed GSQL Query([JAVA](tg-jdbc-examples/src/main/java/com/tigergraph/jdbc/examples/RunQuery.java))
* Loading Job([JAVA](tg-jdbc-examples/src/main/java/com/tigergraph/jdbc/examples/LoadingJob.java) and [Spark](tg-jdbc-examples/src/main/java/com/tigergraph/jdbc/examples/SparkLoadingJob.scala)): ingest data using TigerGraph [loading job](https://docs.tigergraph.com/gsql-ref/current/ddl-and-loading/creating-a-loading-job). When loading large amounts of data, it is better to use Spark to utilize its parallel processing capability.

All of the JAVA examples take 4 parameters: IP address, port, debug and graph name. The default IP address is 127.0.0.1, and the default port is 14240. Other values can be specified as needed.

Debug mode:
> 0: print error messages

> 1: print warning messages

> 2: print basic information (e.g., request received, request sent to TigerGraph, response gotten from TigerGraph)

> 3: print detailed debug information

To run the examples, first clone the repository, then compile and run the examples like the following:

```
cd tg-jdbc-driver
mvn clean && mvn install
cd ../tg-jdbc-examples
mvn exec:java -Dexec.mainClass=com.tigergraph.jdbc.examples.Builtins -Dexec.args="127.0.0.1 14240 2 Social_Net"
mvn exec:java -Dexec.mainClass=com.tigergraph.jdbc.examples.CRUD -Dexec.args="127.0.0.1 14240 2 Social_Net"
mvn exec:java -Dexec.mainClass=com.tigergraph.jdbc.examples.RunQuery -Dexec.args="127.0.0.1 14240 2 Social_Net"
mvn exec:java -Dexec.mainClass=com.tigergraph.jdbc.examples.LoadingJob -Dexec.args="127.0.0.1 14240 2 Social_Net"
```

## Authentication
Please specify `username` and `password` in connection properties for [requesting a authentication token](https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_request_a_token), you can directly specify the `token` in connection properties if you already have it.
### Authentication for Tigergraph Cloud
With the release to TigerGraph Cloud (3.6.1), there's a new security standard of identification of secrets as a part of the integrated login between TigerGraph Cloud Console and GraphStudio. With existing TigerGraph Cloud instances users access via REST++ you can continue to access via `username` and `password`. For all **new TigerGraph Cloud instances** you will need to enter username: `__GSQL__secret` and the password will be your generated secret.

To generate a secret you will need to follow these steps.
- Log into TigerGraph Cloud UI at tgcloud.io
- Navigate to solutions
- Click Applications, Graph Studio
- Select a graph
- Click Admin in upper right corner
- From the left side menu click management then users
- Create new Alias
- Click Plus and secret will be generated

Known Limitations:
- Requires UI interface
- Requires Graph being created

## Support SSL
To support SSL, please configure TigerGraph first according to [Encrypting Connections](https://docs.tigergraph.com/admin/admin-guide/data-encryption/encrypting-connections) to apply the server certificate. Then specify your SSL materials in JDBC connection properties as follows:
```
// SSL server certificate
properties.put("trustStore", "/path/to/trust.jks");
properties.put("trustStorePassword", "password");
properties.put("trustStoreType", "JKS");
// SSL client certificate
properties.put("keyStore", "/path/to/identity.jks");
properties.put("keyStorePassword", "password");
properties.put("keyStoreType", "JKS");
// the url prefix should be https instead of http
try {
  com.tigergraph.jdbc.Driver driver = new Driver();
  try (Connection con =
      driver.connect("jdbc:tg:https://127.0.0.1:14240", properties)) {
    ...
  }
}
```
### Certificate Format
The certificate needs to be converted to JKS format by `keytool`, a built-in key and certificate management utility of Java:
```
/path/to/jre/bin/keytool -import -alias alias -file cert_file.crt -keystore keystorefile.jks -storepass password
```
### Server Authentication
For server authentication, the client needs to verify whether the server certificate is issued by the given `trustStore`, the `trustStore` should contain the certificate of CA who issued the server certificate, or the server certificate itself if it's self-signed.
* The hostname verification is enabled by default to prevent man-in-the-middle attack. You can disable it by `properties.put("sslHostnameVerification", "false")`.
* **Note:** if you are connecting to TigerGraph using a proxy server, it's recommanded to configure the server policy to **pass through** the certificate in case of the compromised proxy. Otherwise, please update the `trustStore` to the CA certificate of the proxy accordingly.
### Client Authentication
For Client Authentication, the `keyStore` should contain the certificate issued by the server certificate. By default TigerGraph server doesn't require client authentication.

## Connection Pool
This JDBC driver could be used in together with third party connection pools. For instance, [HikariCP](https://github.com/brettwooldridge/HikariCP).
To do so, first add the following lines to your `pom.xml`:
```
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>3.4.2</version>
    </dependency>
```

And add the following packages to your Java source code:
```
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
```

Then create a HikariDataSource instance like this:
```
config.setJdbcUrl(sb.toString());
HikariDataSource ds = new HikariDataSource(config);
HikariConfig config = new HikariConfig();

config.setDriverClassName("com.tigergraph.jdbc.Driver");
config.setUsername("tigergraph");
config.setPassword("tigergraph");
config.addDataSourceProperty("graph", "gsql_demo");
config.addDataSourceProperty("debug", "1");
config.setJdbcUrl("jdbc:tg:http://127.0.0.1:14240");
HikariDataSource ds = new HikariDataSource(config);
Connection con = ds.getConnection();
```

Don't forget to close the HikariDataSource at the end:
```
ds.close();
```

If SSL is enabled, please provide truststore like this:
```
config.addDataSourceProperty("trustStore", "/path/to/trust.jks");
config.addDataSourceProperty("trustStorePassword", "password");
config.addDataSourceProperty("trustStoreType", "JKS");
```

Detailed example can be found at [ConnectionPool.java](tg-jdbc-examples/src/main/java/com/tigergraph/jdbc/ConnectionPool.java).

## Supported Queries and Syntax
- `builtins`: run a built-in function
  - Syntax: `builtins function_name(type=?)`
  - Description: run a [built-in function](https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_run_built_in_functions_on_graph) and return relevant statistics about a graph.
  - Example:
    ```
    // Get the number of vertices of a specific type
    builtins stat_vertex_number(type=?)

    // Get the number of edges
    builtins stat_edge_number

    // Get the number of edges of a specific type
    builtins stat_edge_number(type=?)
    ```
  - ResultSet schema: `count | v_type/e_type`
- `get vertex`: get a vertex/vertices
  - Syntax:
    - `get vertex(vertex_type) [params(select=?,filter=?,limit=?,sort=?)]`
    - `get vertex(vertex_type, vertex_id) [params(select=?)]`
  - Description: get all vertices having the type `vertex_type` in a graph, or a single vertex by its vertex ID. [Parameters](https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_parameters_17) are optional.
  - Example:
    ```
    // Get any k vertices of a specified type (example: Page type vertex)
    get vertex(Page) params(limit=?)

    // Get a vertex which has the given id (example: Page type vertex)
    get vertex(Page, ?)

    // Get specified attributes of all vertices which satisfy the given filter (example: Page type vertex)
    get vertex(Page) params(filter=?, select=?)
    ```
  - ResultSet schema: `v_id | attr1 | attr2`

- `get edge`: get an edge/edges
  - Syntax:
    - `get edge(src_vertex_type, src_vertex_id) [params(select=?,filter=?,limit=?,sort=?)]`
    - `get edge(src_vertex_type, src_vertex_id, edge_type) [params(select=?,filter=?,limit=?,sort=?)]`
    - `get edge(src_vertex_type, src_vertex_id, edge_type, tgt_vertex_type) [params(select=?,filter=?,limit=?,sort=?)]`
    - `get edge(src_vertex_type, src_vertex_id, edge_type, tgt_vertex_type, tgt_vertex_id) [params(select=?)]`
  - Description: get edges of a vertex or an edge between 2 vertices. [Parameters](https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_parameters_22) are optional.
  - Example:
    ```
    // Get all edges whose source vertex has the specified type and id
    // There might be several tables in ResultSet because there might be more than one type of edge
    // (example: Page vertex with id)
    get edge(Page, id)

    // Get all edges of given type (example: Linkto) whose source vertex
    // has the specified type and id (example: Page vertex with id)
    get edge(Page, id, Linkto) params(select=?,sort=?)

    // Get all edges of given type (example: Linkto) whose source vertex
    // has the specified type and id (example: Page vertex with id),
    // and the target vertex type is also given (example: Page)
    get edge(Page, id, Linkto, Page)

    // Get a specific edge from a given vertex to another specific vertex
    // (example: from a Page vertex, across a Linkto edge, to a Page vertex)
    get edge(Page, id1, Linkto, Page, id2)
    ```
  - ResultSet schema: `src_vertex_type | tgt_vertex_type | attr1 | attr2`
- `delete vertex`: delete a vertex/vertices
  - Syntax:
    - `delete vertex(vertex_type) [params(filter=?,limit=?,sort=?)]`
    - `delete vertex(vertex_type, vertex_id)`
  - Description: delete all vertices having the type `vertex_type` in a graph, or a single vertex by its vertex ID. [Parameters](https://docs.tigergraph.com/tigergraph-server/3.2/api/built-in-endpoints#_parameters_18) are optional, and `sort` should always be used together with `limit`.
  - Example:
    ```
    // Delete k vertices of a specified type sorted by attr1 (example: Page type vertex)
    delete vertex(Page) params(limit=k, sort='attr1')
    ```

- `delete edge`: delete an edge
  - Syntax:
    - `delete edge(src_vertex_type, src_vertex_id, edge_type, tgt_vertex_type, tgt_vertex_id)`
  - Description: delete an edge by its source vertex type and ID, target vertex type and ID, as well as edge type.
  - Example:
    ```
    // Delete a specific edge from a given vertex to another specific vertex
    // (example: from a Page vertex, across a Linkto edge, to a Page vertex)
    delete edge(Page, id1, Linkto, Page, id2)
    ```

- `insert into vertex/edge`: upsert vertex/edge
  - Syntax:
    - `insert into vertex v_type(primary_id, id, attr1, attr2) values(?, ?, ?, ?)`
    - `insert into edge v_type(from, to, attr1, attr2) values(?, ?, ?, ?)`
  - Description: upsert vertices and/or edges into a graph. To upsert means that if a vertex or edge does not exist, it is inserted, and if it does exist, it is updated. `PreparedStatement` and `addBatch` are recommended.
  - Example:
    ```
    // Insert into a vertex type
    INSERT INTO vertex Page(id, page_id) VALUES(?, ?)

    // Insert into edge type
    INSERT INTO edge Linkto(Page, Page) VALUES(?, ?)
    ```
- `insert into job`: run a loading job
  - Syntax: `insert into job job_name(line) values(?)`
  - Description: submit a line/lines to be loaded into the graph by the DDL Loader. If the dataset is too large, it's recommended to [use Spark to write to TigerGraph](#to-write-to-tigergraph).
  - Example:
    ```
    // Run a pre-installed loading job
    INSERT INTO job load_pagerank(line) VALUES(?)
    ```
- `run interpreted`: run a interpreted query
  - Syntax: `run interpreted(arg1=?, arg2=?)`
  - Description: run a GSQL query in Interpreted Mode. Must use `preparedStatement` and set query body as a parameter.
  - Example:
    ```
    // Run an interpreted query
    query = "run interpreted(a=?, b=?)";
    pstmt = con.prepareStatement(query)
    query_body = "INTERPRET QUERY (int a, int b) FOR GRAPH gsql_demo {\n"
      + "PRINT a, b;\n"
      + "}\n";
    pstmt.setString(1, "10");
    pstmt.setString(2, "20");
    pstmt.setString(3, query_body); // The query body is passed as a parameter.
    ```
  - ResultSet: results of `PRINT` statement.

- `run preinstalled`: run a pre-installed query
  - Syntax: `run query_name(arg1=?, arg2=?)`
  - Description: run a GSQL query which is created and installed in advance.
  - Example:
    ```
    // Run a pre-installed query with parameters (example: the pageRank query from the GSQL Demo Examples)
    run pageRank(maxChange=?, maxIteration=?, dampingFactor=?)
    ```
  - ResultSet: results of `PRINT` statement.
- `find shortestpath`: find shortest path between 2 vertices
  - Syntax: `find shortestpath(src_vertex_type, src_vertex_id, tgt_vertex_type, tgt_vertex_id)`
  - Description: find the shortest path between the source and the target. There might be several tables in ResultSet because there might be more than one type of edge and vertex.
  - Example:
    ```
    // Find the shortest path between 2 vertices(example: person)
    find shortestpath(person, Tom, person, Jack)
    ```
  - ResultSet schema: same as `get vertex` and `get edge`.
- `find allpaths`: find all paths between 2 vertices
  - Syntax: `find allpaths(src_vertex_type, src_vertex_id, tgt_vertex_type, tgt_vertex_id, max_length)`
  - Description: find all paths between the source and the target with maximum path length. There might be several tables in ResultSet because there might be more than one type of edge and vertex.
  - Example:
    ```
    // Find all paths between 2 vertices, with limitation of path length(example: person)
    find allpaths(person, Tom, person, Jack, 5)
    ```
  - ResultSet schema: same as `get vertex` and `get edge`.

See [RESTPP API User Guide: Built-in Endpoints](https://docs.tigergraph.com/dev/restpp-api/built-in-endpoints) for more details about the built-in endpoints.

The default timeout for TigerGraph is 16s, you can use **setQueryTimeout(seconds)** of java.sql.Statement to change timeout for any specific query.

**For the query string that contains special charaters: spaces, tabs, \', \", (, ), and ?, please wrap it by two single quotes, or use parameterized query instead. The single quote enclosed by two single quotes should be escaped by two single quotes**
```
// run a pre-installed query giving content = "content has ? ' and \""
// method 1: quote the the content by single quotation, and escape the original single quote charactor
String query = "run content_query(content = 'content has ? '' and \"')";
// method 2: use parameterized query with placeholder ?
String query = "run content_query(content = ?)";
PreparedStatement pstmt = con.prepareStatement(query);
pstmt.setString(1, "content has ? ' and \"");
```

Detailed examples can be found at [tg-jdbc-examples](tg-jdbc-examples).

## How to use in Apache Spark

If your vertex/edge attributes have LIST/SET type, please register TigergraphDialect, which can convert TG LIST/SET to Spark ArrayType when [retrieving vertices/edges](#to-read-from-tigergraph), or convert Spark ArrayType to TG LIST/SET when [inserting vertices/edges](#to-write-to-tigergraph). **This doesn't work for [loading jobs](#to-load-data-from-files)**.
```
import org.apache.spark.sql.jdbc.JdbcDialects
JdbcDialects.registerDialect(new com.tigergraph.jdbc.TigergraphDialect())
```

### To read from TigerGraph
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

### To write to TigerGraph
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
    "timeout" -> "60", // query timeout in seconds
    "atomic" -> "1", // 0 (default): nonatomic, 1: an atomic transaction
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
    "batchsize" -> "100",
    "debug" -> "0")).save()
```
**NOTE:** use loading job instead of writing to vertex/edge directly for large DataFrame for better performance, as the latter will force JDBC to generate JSON payload, which will be much bigger than the raw data.

### To load data from files
**Warning:** TigerGraph JDBC connector is streaming in data via REST endpoints. No data throttle mechanism is in place yet. When the incoming concurrent JDBC connection number exceeds the configured hardware capacity limit, the overload may cause the system to stop responding.
If you use a Spark job to connect TigerGraph via JDBC, we recommend your concurrent Spark loading jobs be capped at 10 with the following per job configuration. This limits the concurrent JDBC connections to 40.
```
/* 2 executors per job and each executor takes 2 cores */
/path/to/spark/bin/spark-shell --jars /path/to/tigergraph-jdbc-driver-${VERSION}.jar -—num-executors 2 —-executor-cores 2 -i test.scala
```
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
    "batchsize" -> "10000",
    "debug" -> "0")).save()

```
**If your TG version is 3.9.0 or higher, please use the following new features:**
* `jobid`: since the Spark loading is sending data in multiple batches, it's hard to collect the loading stats of all the batches. The `jobid` is a new connection property that helps aggregate the stats of each batch loading, so the overall loading stats can be easily acquired.
* `max_num_error`: the threshold of the error objects count within the `jobid`. The loading job will be aborted when reaching the limit. `jobid` is required.
* `max_percent_error`: the threshold of the error objects percentage within the `jobid`. The loading job will be aborted when reaching the limit. `jobid` is required.

Please refer to [SparkLoadingJob.scala](tg-jdbc-examples/src/main/java/com/tigergraph/jdbc/examples/SparkLoadingJob.scala) for detailed usage.

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

### To invoke interpreted queries
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

**"username"** and **"password"** need to be provided if authentication is enabled.

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

Save any piece of the above script in a file (e.g., test.scala), and run it like this:
```
/path/to/spark/bin/spark-shell --jars /path/to/tigergraph-jdbc-driver-${VERSION}.jar -i test.scala
```

**Please do NOT print multiple objects (i.e., variable list, vertex set, edge set, etc.) in your query if it needs to be invoked via Spark. Otherwise, only one object could be printed. The output format of TigerGraph is JSON, which is an unordered collection of key/value pairs. So the order could not be guaranteed.**

### To enable SSL with Spark
Please add the following options to your scala script:
```
    "trustStore" -> "trust.jks",
    "trustStorePassword" -> "password",
    "trustStoreType" -> "JKS",
```

And run it with **"--files"** option like this:
```
/path/to/spark/bin/spark-shell --jars /path/to/tigergraph-jdbc-driver-${VERSION}.jar --files /path/to/trust.jks -i test.scala
```
The `--files` should be provided the JKS file path, while the `"trustStore" -> "trust.jks"` should be the JKS filename only.

### Load balancing
For TigerGraph clusters, all the machines' ip addresses (separated by a comma) could be passed via option **"ip_list"** to the driver, and the driver will pick one ip randomly to issue the query. Please don't enable this feature when m1 has a heavy load.

### Supported dbTable format when used in Spark
| Operator | Parameters |
| --- | --- |
| vertex | vertex_type[(param)] |
| edge | edge_type[(param)] |
| job | loading_jobname |
| query | query_name[(param)] |
| interpreted | [(param)] query_body |

Path-finding query is not supported, because it has multiple tables in ResultSet, which cannot be written to a single dataframe.
### Supported SaveMode when used in Spark
The default behavior of saving a DataFrame to TigerGraph is **upsert**:
> when the vertex/edge exists, it will be updated.

> otherwise, a new vertex/edge will be created.

We do have other modes, like only update graph when the corresponding vertex/edge exists and do not create any new vertex/edge. But sadly it seems this mode cannot be mapped to any Spark SaveMode directly.

## How to use it in Python
The JDBC driver could be used in Python via pyspark. 'pyspark' needs to be installed first:
```
sudo pip install pypandoc pyspark
```

If your vertex/edge attributes have LIST/SET type, please register TigergraphDialect, which can convert TG LIST/SET to Spark ArrayType when retrieving vertices/edges, or convert Spark ArrayType to TG LIST/SET when inserting vertices/edges.
```
from pyspark.sql import SparkSession
from py4j.java_gateway import java_import

spark = SparkSession.builder \
  .appName("TigerGraphAnalysis") \
  .config("spark.driver.extraClassPath", "/path/to/spark-2.x.x-bin-hadoop2.x/jars/*:/path/to/tigergraph-jdbc-driver-${VERSION}.jar") \
  .getOrCreate()

gw = spark.sparkContext._gateway
java_import(gw.jvm, "com.tigergraph.jdbc.TigergraphDialect")
gw.jvm.org.apache.spark.sql.jdbc.JdbcDialects.registerDialect(gw.jvm.com.tigergraph.jdbc.TigergraphDialect())
```

Then you can read from/write to TigerGraph in Python like this:
```
from pyspark.sql import SparkSession
from pyspark.sql.types import StructType, StructField
from pyspark.sql.types import StringType, IntegerType

spark = SparkSession.builder \
  .appName("TigerGraphAnalysis") \
  .config("spark.driver.extraClassPath", "/path/to/spark-2.x.x-bin-hadoop2.x/jars/*:/path/to/tigergraph-jdbc-driver-${VERSION}.jar") \
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

## Optimizations
* use loading job instead of inserting vertex/edge directly, as the latter will force JDBC to generate JSON payload, which will be much bigger than the raw data
* don’t split the raw data into different columns, i.e., each row should only have one column, so that JDBC can simply pass along the raw data and won’t need to re-org the data. If a data frame is being used, you can merge different columns into one column
* choose `batchsize` carefully according to your average data size of each line/row, the idea payload size is 2-6MB.
* when running compiled or interpreted query via Spark, you can have a parameter named "topK" to limit the number of results returned, and improve the speed of the schema query which appends a query paramter "topK=0" automatically:
    ```
    val dbtable2 = """interpreted INTERPRET QUERY (int lowerBound = 0, int upperBound = 100, int topK = 9999999999) FOR GRAPH gsql_demo {
      V0 = {Person.*};
      V1 = SELECT s FROM V0:s
          WHERE s.account >= lowerBound and s.account < upperBound
          LIMIT topK;
      PRINT V1;
    }"""
    ```
* when running loading job via Spark, better to use `token` instead of `username/password` to avoid repeatedly generating too many tokens.


Here's a demo:
https://tigergraph-misc.s3.amazonaws.com/jdbc-demo.tar.gz

To run it:
```
tar zxf jdbc-demo.tar.gz
cd demo
bash -x run.sh
```
It’ll load 543MB data via JDBC and show how long it takes.

## Limitation of ResultSet
The response packet size from the TigerGraph server should be less than 2GB, which is the largest response size supported by the TigerGraph Restful API.

## Logging Configuration
Tigergraph JDBC Driver supports 4 logging levels: 0 -> ERROR, 1 -> WARN, 2 -> INFO(Default) and 3 -> DEBUG.
It supports two logging frameworks:
- java.util.logging (JUL)
  - To use logger, only need to pass in logging level by `properties.put("debug", "0|1|2|3");`, it will initialize with default logging handler and formatter, which only print logs to console.
  - To customize the JUL configuration, please provide your logging configuration file `logging.properties` and specify the JVM system property **explicitly**: `-Djava.util.logging.config.file=path_to_logging.properties`. Reference: [JUL Documentation](https://docs.oracle.com/javase/7/docs/api/java/util/logging/package-summary.html).

    For example, create a logging configuration file `logging.properties` with following contents:
    ```
    ###########################################################
    #   Default Logging Configuration File
    #
    # You can use a different file by specifying a filename
    # with the java.util.logging.config.file system property.
    # For example java -Djava.util.logging.config.file=myfile
    ############################################################

    ############################################################
    #   Global properties
    ############################################################

    # "handlers" specifies a comma-separated list of log Handler
    # classes.  These handlers will be installed during VM startup.
    # Note that these classes must be on the system classpath.
    # ConsoleHandler and FileHandler are configured here such that
    # the logs are dumped into both a standard error and a file.
    handlers = java.util.logging.ConsoleHandler, java.util.logging.FileHandler

    # Default global logging level.
    # This specifies which kinds of events are logged across
    # all loggers.  For any given facility this global level
    # can be overriden by a facility specific level.
    # Note that the ConsoleHandler also has a separate level
    # setting to limit messages printed to the console.
    .level = INFO

    ############################################################
    # Handler specific properties.
    # Describes specific configuration information for Handlers.
    ############################################################

    # default file output is in the tmp dir
    java.util.logging.FileHandler.pattern = /tmp/TG_JDBC_%u.log
    java.util.logging.FileHandler.limit = 5000000000000000
    java.util.logging.FileHandler.count = 10
    java.util.logging.FileHandler.level = INFO
    java.util.logging.FileHandler.formatter = java.util.logging.SimpleFormatter

    # Limit the messages that are printed on the console to INFO and above.
    java.util.logging.ConsoleHandler.level = INFO
    java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter

    # Example to customize the SimpleFormatter output format
    # to print one-line log message like this:
    #     <level>: <log message> [<date/time>]
    #
    # java.util.logging.SimpleFormatter.format=%4$s: %5$s [%1$tc]%n

    ############################################################
    # Facility specific properties.
    # Provides extra control for each logger.
    ############################################################

    # Tigergraph JDBC logging level.
    com.tigergraph.jdbc.level = INFO
    com.tigergraph.jdbc.handler = java.util.logging.FileHandler
    ```
- Simple Logging Facade for Java (SLF4J)
  - To use SLF4J, specify the JVM system property: `-Dcom.tigergraph.jdbc.loggerImpl=slf4j` and put SLF4J binding in your classpath. Reference: [SLF4J Documentation](https://www.slf4j.org/docs.html).

## Password Sealing
It could be insecure to type in some sensitive properties like `password`, `token` in the code directly, here we recommend storing the properties in a `.properties` file.
- Load properties from file:
  ```
  Properties properties = new Properties();
  InputStream properties_file = new FileInputStream("path/to/properties/file");
  properties.load(properties_file);
  ```
- For higher security requirement, you can encrypt properties file by some third party crypto libraries like [`jasypt`](http://www.jasypt.org):
  - Use [Jasypt CLI Tools](http://www.jasypt.org/cli.html) to encrypt each entry of the properties file, the encrypted value should be like `password=ENC(!"DGAS24FaIO$)`
  - Load the encrypted file and decrypt it:
    ```
    BasicTextEncryptor encryptor = new BasicTextEncryptor();
    encryptor.setPassword(ENCRYPTOR_STRING);
    Properties properties = new EncryptableProperties(encryptor);
    InputStream encrypted_properties_file = new FileInputStream("path/to/encrypted/properties/file");
    properties.load(encrypted_properties_file);
    ```

## Connection Properties
| **Property Name**         | **Default** | **Meaning**                                                  | **Required**                                                 | **Scope**         |
| :------------------------ | :---------- | :----------------------------------------------------------- | :----------------------------------------------------------- | :---------------- |
| `driver`                  | (none)      | Fully qualified domain name(FQCN) of the JDBC driver: `com.tigergraph.jdbc.Driver`. | Yes                                                          | all               |
| `url`                     | (none)      | The JDBC URL to connect to: `jdbc:tg:http(s)://ip:port`, the port is the one used by GraphStudio. | Yes                                                          | all               |
| `graph`                   | (none)      | The graph name.                                              | Yes                  | all      |
| `version`                 | 3.9.0       | The TigerGraph version.                                      | Yes                  | all      |
| `username`                | tigergraph  | TigerGraph username.                                         | If [RESTPP authentication](https://docs.tigergraph.com/tigergraph-server/current/user-access/enabling-user-authentication#_enable_restpp_authentication) is enabled, one of `username/password` and `token` is required. | all               |
| `password`                | tigergraph  | TigerGraph password.                                         | If [RESTPP authentication](https://docs.tigergraph.com/tigergraph-server/current/user-access/enabling-user-authentication#_enable_restpp_authentication) is enabled, one of `username/password` and `token` is required. | all               |
| `token`                   | (none)      | A token used to authenticate RESTPP requests. [Request a token](https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_request_a_token) | If [RESTPP authentication](https://docs.tigergraph.com/tigergraph-server/current/user-access/enabling-user-authentication#_enable_restpp_authentication) is enabled, one of `username/password` and `token` is required. | all               |
| `jobid` (TG version >= 3.9.0)                 | (none)      | A unique ID for tracing aggregated loading statistics.       | No                                                           | loading job       |
| `max_num_error` (TG version >= 3.9.0)         | (none)      | The threshold of the error objects count within the `jobid`. The loading job will be aborted when reaching the limit. `jobid` is required. | No | loading job |
| `max_percent_error` (TG version >= 3.9.0)     | (none)      | The threshold of the error objects percentage within the `jobid`. The loading job will be aborted when reaching the limit. `jobid` is required. | No | loading job |
| `filename`                | (none)      | The filename defined in the loading job.                     | Yes                                                          | loading job       |
| `sep`                     | (none)      | Column separator. E.g., `,`.                                 | Yes                                                          | loading job       |
| `eol`                     | (none)      | Line separator. E.g., `\n`.                                  | Yes                                                          | loading job       |
| `dbtable`                 | (none)      | The specification of the operation of the form: `operation_type operation_object`.  For loading job: `job JOB_NAME`; E.g. for querying loading statistics: `jobid JOB_ID`. | Yes                                                          | Spark             |
| `batchsize`               | 1000        | Maximum number of lines per POST request.                    | Yes                                                          | Spark loading job |
| `partitionColumn`         | (none)      | The column used for partitioning, it has to be numeric or date or timestamp column. | No | Spark partitioning query |
| `lowerBound`              | (none)      | The minimum value in the partition column. | No | Spark partitioning query |
| `upperBound`              | (none)      | The maximum value in the partition column. | No | Spark partitioning query |
| `numPartitions`           | (none)      | The maximum number of partitions that can be used for parallel processing in table reading and writing. | No | Spark partitioning query |
| `debug`                   | 2           | Log level:0 → ERROR, 1 → WARN, 2 → INFO, 3 → DEBUG                 | Yes                                                          | all               |
| `logFilePattern`          | (none)      | The log file name pattern, e.g., "/tmp/tigergraph-jdbc-driver.log", the log will be printed to stderr when it is not given        | all               |
| `ip_list`                 | (none)      | A string that contains IP addresses of TigerGraph nodes separated by a comma, which can be used for load balancing. E.g., `192.168.0.50,192.168.0.51,192.168.0.52` | No                                                           | all               |
| `trustStore`              | (none)      | Filename of the truststore which stores the SSL certificate. Please add `--files /path/to/trust.jks` when submitting the Spark job. | No                                                           | all               |
| `trustStorePassword`      | (none)      | Password of the truststore.                                  | No                                                           | all               |
| `trustStoreType`          | (none)      | Truststore type, e.g., `jks`.                                | No                                                           | all               |
| `sslHostnameVerification` | true        | Whether to verify the host name in the `url` matches the host name in the certificate. | No                                                           | all               |
| `queryTimeout`   | RESTPP.Factory.DefaultQueryTimeoutSec        | The timeout (s) for RESTPP request.      | No                                                           | all               |
| `connectTimeout` | 30                                           | The connect timeout (s) for HTTP client. | No                                                           | all               |

## FAQ
- Q: Is the JDBC driver able to load a `LIST` or `SET` attribute?

  A: Yes, this can be done with a loading job, but you should ensure the [loading job and data format](https://docs.tigergraph.com/gsql-ref/current/ddl-and-loading/creating-a-loading-job#_loading_a_list_or_set_attribute) are correct.
  
  When the vertex or edge already exists, loading the `LIST` attribute will be in appending manner. To overwrite it, please refer to the reducer function [overwrite(arg)](https://docs.tigergraph.com/gsql-ref/current/ddl-and-loading/functions/reducer/#_table_of_reducer_functions).

- Q: How can I run my own queries?

  A: If your query is simple, we recommend using interpreted query for its convenience. However, due to [interpreted GSQL limitations](https://docs.tigergraph.com/gsql-ref/current/appendix/interpreted-gsql-limitations), you have to use pre-installed query for some features like `Accumulator`.
