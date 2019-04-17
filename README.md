# TigerGraph JDBC Driver
Currently this driver only supports builtin queries and compiled queries (i.e., queries must be compiled and installed before being invoked via the JDBC driver), and the driver will talk to TigerGraph Rest++ to run queries and get their results.

Going forward there will also be support for interpreted mode, which means it could install queries as well as run queries.

## Versions compatibility

| JDBC Version | TigerGraph Version | Java | Protocols |
| --- | --- | --- | --- |
| 1.0.0 | 2.2.4+ | 1.8 | Rest++ |

## Minimum viable snippet
Parameters could be passed as properties when creating a connection, like token and graph name. Once REST++ authentication is enabled, token is needed to be specified. Graph name is needed especially when multi-graph is enabled.

You may specify IP address and port as needed. Please change 'http' to 'https' when SSL is enabled.

For each ResultSet, there is only one column, which is a JSON object, representing each print statement in GQuery. You may use any JSON library to parse it.
```
Properties properties = new Properties();
properties.put("token", "84d37c434950e7e54339057e93af72de79728ba7");
properties.put("graph", "BitCoinGraph");

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
// Get vertex number of a specific type
builtins stat_vertex_number(type=?)

// Get edge number statistics.
builtins stat_edge_number

// Get edge number of a specific type
builtins stat_edge_number(type=?)

// Get any k vertices of Block type
get Block(limit=?)

// Get a Block which has the given id
get Block(limit=?)

// Get a Block which is satisfied with certain condition
get Block(filter=?)

// Get all edges from a given vertex
get edges(Block, ?)

// Get a specific edge from a given vertex to another specific vertex
get edge(Block, ?, chain, Block, ?)

// Run a pre-installed query with parameters
run pageRank(maxChange=?, maxIteration=?, dumpingFactor=?)
```

Detailed examples could be found at [tg-jdbc-examples](https://github.com/tigergraph/tg-java-driver/tg-jdbc-examples).

## Run examples
There are 3 demo applications, all ot them take 3 parameters: IP address, port, debug. The default IP address is 127.0.0.1, and the default port is 9000. Other values may be specified as needed. Debug mode could be turned on when the third parameter is larger than 0.

To run the examples, first clone [the repository](https://github.com/tigergraph/tg-java-driver), then compile it and run it like this:

```
mvn compile
cd tg-jdbc-examples
mvn exec:java -Dexec.mainClass=com.tigergraph.jdbc.examples.Builtins -Dexec.args="127.0.0.1 9000 1"
mvn exec:java -Dexec.mainClass=com.tigergraph.jdbc.examples.GraphQuery -Dexec.args="127.0.0.1 9000 1"
mvn exec:java -Dexec.mainClass=com.tigergraph.jdbc.examples.Builtins -Dexec.args="127.0.0.1 9000 1"
```

## Limitation of ResultSet
The response package size from TigerGraph server should be less than 2GB, otherwise it may lead to OOM.

