package com.tigergraph.jdbc.examples;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import com.tigergraph.jdbc.*;
import com.tigergraph.jdbc.restpp.*;
import java.sql.DriverManager;
import java.util.Properties;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Example code to demonstrate how to use it with a connection pool
 * https://github.com/brettwooldridge/HikariCP
 */
public class ConnectionPool
{
  public static void main( String[] args ) throws SQLException {
    HikariConfig config = new HikariConfig();

    config.setDriverClassName("com.tigergraph.jdbc.Driver");
    config.setUsername("tigergraph");
    config.setPassword("tigergraph");
    config.addDataSourceProperty("graph", "gsql_demo");
    config.addDataSourceProperty("filename", "f");
    config.addDataSourceProperty("sep", ",");
    config.addDataSourceProperty("eol", ";");

    String ipAddr = "127.0.0.1";
    // port of GraphStudio
    Integer port = 14240;

    /**
     * Only accept 3 parameters: ip address, port and debug.
     * Enable debug when the third parameter's value is larger than 0.
     */
    if (args.length == 3) {
      ipAddr = args[0];
      port = Integer.valueOf(args[1]);
      config.addDataSourceProperty("debug", args[2]);
    }

    /**
     * Specify ip address and port of the TigerGraph server.
     * Please use 'https' instead once ssl is enabled.
     */
    StringBuilder sb = new StringBuilder();
    sb.append("jdbc:tg:http://").append(ipAddr).append(":").append(port);
    config.setJdbcUrl(sb.toString());
    HikariDataSource ds = new HikariDataSource(config);

    try (Connection con = ds.getConnection()) {
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
    ds.close();
  }
}
