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
 * https://docs.tigergraph.com/dev/gsql-examples/common-applications#example-2-page-rank
 */
public class RunQuery
{
  public static void main( String[] args ) throws SQLException {
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
     * Specify the graph name, especially when multi-graph is enabled.
     */
    properties.put("graph", "gsql_demo");

    /**
     * Only accept 3 parameters: ip address, port and debug.
     * Enable debug when the third parameter's value is larger than 0.
     */
    if (args.length == 3) {
      ipAddr = args[0];
      port = Integer.valueOf(args[1]);
      properties.put("debug", args[2]);
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
         * Run a pre-installed query with parameters.
         */
        System.out.println("Running \"run pageRank(maxChange=?, maxIteration=?, dampingFactor=?)\"...");
        String query = "run pageRank(maxChange=?, maxIteration=?, dampingFactor=?)";
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          // Set timeout (in seconds).
          pstmt.setQueryTimeout(60);
          pstmt.setString(1, "0.001");
          pstmt.setInt(2, 10);
          pstmt.setString(3, "0.15");
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            do {
              java.sql.ResultSetMetaData metaData = rs.getMetaData();
              System.out.println("\n>>>Table: " + metaData.getCatalogName(1));
              for (int i = 1; i <= metaData.getColumnCount(); ++i) {
                if (i > 1) {
                  System.out.print("\t");
                }
                System.out.print(metaData.getColumnName(i));
                System.out.print("," + String.valueOf(metaData.getScale(i)));
                System.out.print("," + String.valueOf(metaData.getPrecision(i)));
              }
              System.out.println("");
              while (rs.next()) {
                System.out.print(rs.getObject(1));
                for (int i = 2; i <= metaData.getColumnCount(); ++i) {
                  Object obj = rs.getObject(i);
                  System.out.print("\t" + String.valueOf(obj));
                }
                System.out.println("");
              }
            } while (!rs.isLast());
          }
        } catch (SQLException e) {
          System.out.println( "Failed to createStatement: " + e);
        }

        /**
         * Run a pre-installed query, without specifying all parameters needed.
         */
        System.out.println("");
        System.out.println("Running \"run pageRank(maxChange=?)\"...");
        query = "run pageRank(maxChange=?)";
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "0.001");
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
                  System.out.print("\t" + String.valueOf(obj));
                }
                System.out.println("");
              }
            } while (!rs.isLast());
          }
        } catch (SQLException e) {
          System.out.println( "Failed to createStatement: " + e);
        }

        /**
         * Run an interpreted query with parameters.
         */
        System.out.println("");
        System.out.println("Running interpreted query \"run interpreted(a=?, b=?)\"...");
        query = "run interpreted(a=?, b=?)";
        String query_body = "INTERPRET QUERY (int a, int b) FOR GRAPH gsql_demo {\n"
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
