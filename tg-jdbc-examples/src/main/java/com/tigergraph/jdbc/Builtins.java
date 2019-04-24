package com.tigergraph.jdbc.examples;

import com.tigergraph.jdbc.*;
import com.tigergraph.jdbc.restpp.*;
import java.sql.DriverManager;
import java.util.Properties;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Example code to demonstrate how to invoke TigerGraph builtin queries
 * The corresponding TigerGraph demo could be found at:
 * https://docs.tigergraph.com/dev/gsql-examples/common-applications#example-2-page-rank
 */
public class Builtins {
  public static void main( String[] args ) throws SQLException {
    Boolean debug = Boolean.FALSE;
    Properties properties = new Properties();
    String ipAddr = "127.0.0.1";
    Integer port = 9000;

    /**
     * Need to specify token once REST++ authentication is enabled.
     * Token could be got from gsql shell or administrator.
     */
    properties.put("token", "36oaj9gck3rrqc9jo2ve6fh6796i629c");

    /**
     * Specify the graph name, especially when multi-graph is enabled.
     */
    properties.put("graph", "gsql_demo");

    /**
     * Only accept 3 parameters: IP address, port and debug.
     * Enable debug when the third parameter's value is larger than 0.
     */
    if (args.length == 3) {
      ipAddr = args[0];
      port = Integer.valueOf(args[1]);
      debug = (Integer.valueOf(args[2]) > 0);
    }

    /**
     * Specify ip address and port of the TigerGraph server.
     * Please use 'https' instead once ssl is enabled.
     */
    StringBuilder sb = new StringBuilder();
    sb.append("jdbc:tg:http://").append(ipAddr).append(":").append(port);

    try {
      com.tigergraph.jdbc.Driver driver = new Driver();
      try (Connection con = driver.connect(sb.toString(), properties, debug)) {
        try (Statement stmt = con.createStatement()) {
          /**
           * Run a builtin query without any parameter.
           */
          String query = "builtins stat_vertex_number";
          System.out.println("Running \"builtins stat_vertex_number\"...");
          try (java.sql.ResultSet rs = stmt.executeQuery(query)) {
              while (rs.next()) {
                Object obj = rs.getObject(0);
                System.out.println(String.valueOf(obj));
              }
          }

          /**
           * Run a builtin query with a parameter.
           */
          System.out.println();
          System.out.println("Running \"builtins stat_vertex_number(type=?)\"...");
          query = "builtins stat_vertex_number(type=?)";
          try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(0, "*");
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
              while (rs.next()) {
                Object obj = rs.getObject(0);
                System.out.println(String.valueOf(obj));
              }
            }
          }

          /**
           * Get edge number statistics.
           */
          System.out.println();
          System.out.println("Running \"builtins stat_edge_number\"...");
          query = "builtins stat_edge_number";
          try (java.sql.ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
              Object obj = rs.getObject(0);
              System.out.println(String.valueOf(obj));
            }
          }

          /**
           * Get total number of a specific edge type.
           */
          System.out.println();
          System.out.println("Running \"builtins stat_edge_number(type=?)\"...");
          query = "builtins stat_edge_number(type=?)";
          try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(0, "Linkto");
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
              while (rs.next()) {
                Object obj = rs.getObject(0);
                System.out.println(String.valueOf(obj));
              }
            }
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

