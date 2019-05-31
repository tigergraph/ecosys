# TigerGraph JDBC Driver
version 1.0
The TigerGraph JDBC Driver is a Type 4 JDBC, converting JDBC calls directly into TigerGraph database commands. Currently this driver only supports TigerGraph builtin queries and compiled queries (i.e., queries must be compiled and installed before being invoked via the JDBC driver). The driver will then talk to TigerGraph Rest++ server to run queries and get their results.

Support for GSQL interpreted mode is on the roadmap, meaning it will be able to run ad hoc queries, without needing to compile and install the quries beforehand. 

## Versions compatibility

| JDBC Version | TigerGraph Version | Java | Protocols |
| --- | --- | --- | --- |
| 1.0.0 | 2.2.4+ | 1.8 | Rest++ |

## Minimum viable snippet
Parameters are passed as properties when creating a connection, such as token and graph name. Once REST++ authentication is enabled, a token must be specified. Graph name is required when multi-graph is enabled.

You may specify IP address and port as needed. Please change 'http' to 'https' when SSL is enabled.

For each ResultSet, there is only one column, which is a JSON object, representing each PRINT statement in the GSQL query. You can use any JSON library to parse the returned JSON object.
```
Properties properties = new Properties();
properties.put("token", "84d37c434950e7e54339057e93af72de79728ba7");
properties.put("graph", "gsql_demo");

try {
  com.tigergraph.jdbc.Driver driver = new Driver();
  try (Connection con =
      driver.connect("jdbc:tg:http://127.0.0.1:9000",
        properties, debug)) {
    try (Statement stmt = con.createStatement()) {
      String query = "builtins stat_vertex_number";
      try (java.sql.ResultSet rs = stmt.executeQuery(query)) {
          while (rs.next()) {
            Object obj = rs.getObject(0);
            System.out.println(String.valueOf(obj));
          }
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

// Get any k vertices of a specified type (example: Page type)
get Page(limit=?)

// Get a vertex which has the given id (example: Page type vertex)
get Page(limit=?)

// Get a vertex which is satisfied with certain condition (example: Page t ype vertex)
get Page(filter=?)

// Get all edges whose source vertex has the specified type and if (example: Page type)
get edges(Page, ?)

// Get a specific edge from a given vertex to another specific vertex
// (example: from Page type vertex, across Linkto type edge, to Page type vertex)
get edge(Page, ?, Linkto, Page, ?)
```

Detailed examples can be found at [tg-jdbc-examples](https://github.com/tigergraph/tg-java-driver/tg-jdbc-examples).

## Run examples
There are 3 demo applications. All of them take 3 parameters: IP address, port, debug. The default IP address is 127.0.0.1, and the default port is 9000. Other values can be specified as needed. Debug mode is turned on when the third parameter is larger than 0.

To run the examples, first clone [the repository](https://github.com/tigergraph/tg-java-driver), then compile and run the examples like the following:

```
mvn compile
cd tg-jdbc-examples
mvn exec:java -Dexec.mainClass=com.tigergraph.jdbc.examples.Builtins -Dexec.args="127.0.0.1 9000 1"
mvn exec:java -Dexec.mainClass=com.tigergraph.jdbc.examples.GraphQuery -Dexec.args="127.0.0.1 9000 1"
mvn exec:java -Dexec.mainClass=com.tigergraph.jdbc.examples.Builtins -Dexec.args="127.0.0.1 9000 1"
```

## Limitation of ResultSet
The response packet size from the TigerGraph server should be less than 2GB, which is the largest response size supported by the TigerGraph Restful API.

