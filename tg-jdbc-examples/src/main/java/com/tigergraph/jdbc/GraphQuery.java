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
    properties.put("graph", "BitCoinGraph");

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
         * To get any 3 vertices of Block type
         */
        String query = "get Block(limit=?)";
        System.out.println("Running \"get Block(limit=?)\"...");
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
         * To get a Block which has the given id
         */
        System.out.println();
        query = "get Block(id=?)";
        System.out.println("Running \"get Block(id=?)\"...");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(0, "000000005d1ee9f633d16642d1bd8af362c3e49e7ce819937ebe6873aaa8c7b7");
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
         * To get a Block whose height is 1
         */
        System.out.println();
        query = "get Block(filter=?)";
        System.out.println("Running \"get Block(filter=?)\"...");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(0, "height=1");
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
        query = "get edges(Block, ?)";
        System.out.println("Running \"get edges(Block, ?)\"...");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(0, "000000005d1ee9f633d16642d1bd8af362c3e49e7ce819937ebe6873aaa8c7b7");
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
        query = "get edge(Block, ?, chain, Block, ?)";
        System.out.println("Running \"get edge(Block, ?, Chain, Block, ?)\"...");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(0, "000000005d1ee9f633d16642d1bd8af362c3e49e7ce819937ebe6873aaa8c7b7");
          pstmt.setString(1, "00000000184d61d43e667b4aebe6224e0a3265a2be87048b7924d7339de6095d");
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

