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
public class UpsertQuery
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
     * For loading job
     */
    properties.put("filename", "f");
    properties.put("sep", ",");
    properties.put("eol", ";");

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
         * Upsert vertices and edges with Statement.
         */
        System.out.println("Running \"INSERT INTO vertex Page(id, page_id) VALUES('1000', '1000')");
        try (java.sql.Statement stmt = con.createStatement()) {
          String query = "INSERT INTO vertex Page(id, page_id) VALUES('1000', '1000')";
          stmt.addBatch(query);
          query = "INSERT INTO vertex Page(id, page_id) VALUES('1001', '1001')";
          stmt.addBatch(query);
          query = "INSERT INTO edge Linkto(Page, Page) VALUES('1000', '1001')";
          stmt.addBatch(query);
          int[] count = stmt.executeBatch();
          System.out.println("Upsert'ed " + count[0] + " vertices, " + count[1] + " edges.");
        } catch (SQLException e) {
          System.out.println( "Failed to createStatement: " + e);
        }

        /**
         * Upsert vertices and edges with PreparedStatement.
         */
        System.out.println("");
        System.out.println("Running \"INSERT INTO vertex Page(id, page_id) VALUES(?, ?)\"...");
        String query = "INSERT INTO vertex Page(id, page_id) VALUES(?, ?)";
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "1002");
          pstmt.setString(2, "1002");
          pstmt.addBatch();
          pstmt.setString(1, "1003");
          pstmt.setString(2, "1003");
          pstmt.addBatch();
          pstmt.setString(1, "1002");
          pstmt.setString(2, "1003");
          query = "INSERT INTO edge Linkto(Page, Page) VALUES(?, ?)";
          pstmt.addBatch(query);
          pstmt.setString(1, "1001");
          pstmt.setString(2, "1002");
          pstmt.addBatch();
          int[] count = pstmt.executeBatch();
          System.out.println("Upsert'ed " + count[0] + " vertices, " + count[1] + " edges.");
        } catch (SQLException e) {
          System.out.println( "Failed to createStatement: " + e);
        }

        /**
         * Invoke a loading job
         */
        System.out.println("");
        System.out.println("Running \"INSERT INTO job load_pagerank(line) VALUES(?)\"...");
        query = "INSERT INTO job load_pagerank(line) VALUES(?)";
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "4,5,1");
          pstmt.addBatch();
          pstmt.setString(1, "5,6,1");
          pstmt.addBatch();
          pstmt.setString(1, "7,10,1");
          pstmt.addBatch();
          int[] count = pstmt.executeBatch();
          System.out.println("Accepted " + count[0] + " lines, rejected " + count[1] + " lines.");
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
