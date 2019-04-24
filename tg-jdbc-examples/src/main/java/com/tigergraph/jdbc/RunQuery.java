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
         * Run a pre-installed query with parameters.
         */
        System.out.println("Running \"run pageRank(maxChange=?, maxIteration=?, dampingFactor=?)\"...");
        String query = "run pageRank(maxChange=?, maxIteration=?, dampingFactor=?)";
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(0, "0.001");
          pstmt.setInt(1, 10);
          pstmt.setString(2, "0.15");
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
         * Run a pre-installed query, without specifying all parameters needed.
         */
        System.out.println("");
        System.out.println("Running \"run pageRank(maxChange=?)\"...");
        query = "run pageRank(maxChange=?)";
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(0, "0.001");
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
         * Run a none-existed query.
         */
        System.out.println("");
        System.out.println("Running none-existed query \"run none-existed(param=?)\"...");
        query = "run none-existed(param=?)";
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(0, "0.001");
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

