package com.tigergraph.jdbc.examples;

import com.tigergraph.jdbc.*;
import com.tigergraph.jdbc.restpp.*;
import java.sql.DriverManager;
import java.util.Properties;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Example code to demonstrate how to query certain vertices & edges.
 * The corresponding TigerGraph demo could be found at:
 * https://docs.tigergraph.com/dev/gsql-examples/common-applications#example-2-page-rank
 */
public class GraphQuery
{
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
     * Only accept 3 parameters: ip address, port and debug.
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
        /**
         * To get any 3 vertices of Page type
         */
        String query = "get Page(limit=?)";
        System.out.println("Running \"get Page(limit=?)\"...");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setInt(0, 3);
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
              Object obj = rs.getObject(0);
              System.out.println(String.valueOf(obj));
            }
          }
        } catch (SQLException e) {
          System.out.println( "Failed to createStatement: " + e);
        }

        /**
         * To get a Page which has the given id
         */
        System.out.println();
        query = "get Page(id=?)";
        System.out.println("Running \"get Page(id=?)\"...");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(0, "2");
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
              Object obj = rs.getObject(0);
              System.out.println(String.valueOf(obj));
            }
          }
        } catch (SQLException e) {
          System.out.println( "Failed to createStatement: " + e);
        }

        /**
         * To get a Page whose page_id is 2
         */
        System.out.println();
        query = "get Page(filter=?)";
        System.out.println("Running \"get Page(filter=?)\"...");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(0, "page_id=2");
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
              Object obj = rs.getObject(0);
              System.out.println(String.valueOf(obj));
            }
          }
        } catch (SQLException e) {
          System.out.println( "Failed to createStatement: " + e);
        }

        /**
         * To get all edges from a given vertex
         */
        System.out.println();
        query = "get edges(Page, ?)";
        System.out.println("Running \"get edges(Page, ?)\"...");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(0, "2");
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
              Object obj = rs.getObject(0);
              System.out.println(String.valueOf(obj));
            }
          }
        } catch (SQLException e) {
          System.out.println( "Failed to createStatement: " + e);
        }

        /**
         * To get a specific edge from a given vertex to another specific vertex
         */
        System.out.println();
        query = "get edge(Page, ?, Linkto, Page, ?)";
        System.out.println("Running \"get edge(Page, ?, Linkto, Page, ?)\"...");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(0, "2");
          pstmt.setString(1, "3");
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
              Object obj = rs.getObject(0);
              System.out.println(String.valueOf(obj));
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

