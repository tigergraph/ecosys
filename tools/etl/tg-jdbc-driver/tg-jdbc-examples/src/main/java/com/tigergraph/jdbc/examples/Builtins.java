package com.tigergraph.jdbc.examples;

import com.tigergraph.jdbc.Driver;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/*
 * Example code to demonstrate how to invoke TigerGraph builtin queries.
 * The corresponding TigerGraph demo could be found at:
 * data Social_Net:
 *   https://docs.tigergraph.com/gsql-ref/current/appendix/example-graphs
 * query ref:
 *   https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_run_built_in_functions_on_graph
 */
public class Builtins {
  public static void main(String[] args) throws SQLException {
    Properties properties = new Properties();
    String ipAddr = "127.0.0.1";
    // port of GraphStudio
    Integer port = 14240;

    /** Need to specify username and password once REST++ authentication is enabled. */
    properties.put("username", "tigergraph");
    properties.put("password", "tigergraph");

    /**
     * Only accept 4 parameters: IP address, port, debug and graph name. Enable debug when the third
     * parameter's value is larger than 0.
     */
    if (args.length == 4) {
      ipAddr = args[0];
      port = Integer.valueOf(args[1]);

      properties.put("debug", args[2]);

      // Specify the graph name, especially when multi-graph is enabled.
      properties.put("graph", args[3]);
    }

    /**
     * Specify ip address and port of the TigerGraph server. Please use 'https' instead once ssl is
     * enabled.
     */
    StringBuilder sb = new StringBuilder();
    sb.append("jdbc:tg:http://").append(ipAddr).append(":").append(port);

    try {
      com.tigergraph.jdbc.Driver driver = new Driver();
      try (Connection con = driver.connect(sb.toString(), properties)) {
        try (Statement stmt = con.createStatement()) {
          /** Run a builtin query without any parameter. */
          String query = "builtins stat_vertex_number";
          System.out.println("Getting stats of all vertices");
          try (java.sql.ResultSet rs = stmt.executeQuery(query)) {
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

          /** Run a builtin query with a parameter. */
          System.out.println();
          System.out.println("Getting stats of vertex Person");
          query = "builtins stat_vertex_number(type=?)";
          try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, "Person");
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
          }

          /** Get edge number statistics. */
          System.out.println();
          System.out.println("Getting stats of all edges");
          query = "builtins stat_edge_number";
          try (java.sql.ResultSet rs = stmt.executeQuery(query)) {
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

          /** Get total number of a specific edge type. */
          System.out.println();
          System.out.println("Getting stats of edge Liked");
          query = "builtins stat_edge_number(type=?)";
          try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, "Liked");
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
          }
        } catch (SQLException e) {
          System.out.println("Failed to createStatement: " + e);
        }
      } catch (SQLException e) {
        System.out.println("Failed to getConnection: " + e);
      }
    } catch (SQLException e) {
      System.out.println("Failed to init Driver: " + e);
    }
  }
}
