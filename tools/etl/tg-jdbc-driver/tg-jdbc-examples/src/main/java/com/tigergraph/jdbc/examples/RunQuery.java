package com.tigergraph.jdbc.examples;

import com.tigergraph.jdbc.*;
import com.tigergraph.jdbc.restpp.*;
import java.sql.DriverManager;
import java.util.Properties;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Example code to demonstrate how to invoke TigerGraph pre-installed queries
 * The corresponding TigerGraph demo could be found at:
 * data socailNet: https://docs.tigergraph.com/gsql-ref/3.4/querying/appendix-query/example-graphs
 */
public class RunQuery
{
  public static void main(String[] args) throws SQLException {
    Properties properties = new Properties();
    String ipAddr = "127.0.0.1";
    // port of GraphStudio
    Integer port = 14240;

    /**
     * Need to specify username and password once REST++ authentication is enabled.
     */
    properties.put("username", "tigergraph");
    properties.put("password", "tigergraph");

    /**
     * Only accept 4 parameters: IP address, port, debug and graph name.
     * Enable debug when the third parameter's value is larger than 0.
     */
    if (args.length == 4) {
      ipAddr = args[0];
      port = Integer.valueOf(args[1]);

      properties.put("debug", args[2]);

      // Specify the graph name, especially when multi-graph is enabled.
      properties.put("graph", args[3]);
    }

    /**
     * Specify ip address and port of the TigerGraph server.
     * Please use 'https' instead once ssl is enabled.
     */
    StringBuilder sb = new StringBuilder();
    sb.append("jdbc:tg:http://").append(ipAddr).append(":").append(port);

    try {
      com.tigergraph.jdbc.Driver driver = new Driver();

      try (Connection con = driver.connect(sb.toString(), properties)) {
        /**
         * Run an interpreted query with parameters.
         */
        System.out.println("");
        System.out.println("Running interpreted query \"run interpreted(a=?, b=?)\"...");
        String query = "run interpreted(a=?, b=?)";
        String query_body = "INTERPRET QUERY (int a, int b) FOR GRAPH socialNet {\n"
                     + "PRINT a, b;\n"
                     + "}\n";
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "10");
          pstmt.setString(2, "20");
          pstmt.setString(3, query_body); // The query body is passed as a parameter.
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            do {
              java.sql.ResultSetMetaData metaData = rs.getMetaData();
              System.out.println("Table: " + metaData.getCatalogName(1));
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
        } catch (SQLException e) {
          System.out.println( "Failed to createStatement: " + e);
        }
      } catch (SQLException e) {
          System.out.println( "Failed to getConnection: " + e);
      }
    } catch (SQLException e) {
        System.out.println( "Failed to init Driver: " + e);
    }
  }
}
