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
 * Running on TigerGraph V3.4
 * Example code to demonstrate how to use it with a connection pool
 * https://github.com/brettwooldridge/HikariCP
 * data socailNet: https://docs.tigergraph.com/gsql-ref/3.4/querying/appendix-query/example-graphs
 * Pagerank need installed first. https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Centrality/pagerank/global/unweighted/tg_pagerank.gsql
 */
public class ConnectionPool
{
  public static void main( String[] args ) throws SQLException {
    HikariConfig config = new HikariConfig();

    String ipAddr = "127.0.0.1";
    // port of GraphStudio
    Integer port = 14240;

    /**
     * Only accept 4 parameters: IP address, port, debug and graph name.
     * Enable debug when the third parameter's value is larger than 0.
     */
    if (args.length == 4) {
      ipAddr = args[0];
      port = Integer.valueOf(args[1]);

      config.addDataSourceProperty("debug", args[2]);

      // Specify the graph name, especially when multi-graph is enabled.
      config.addDataSourceProperty("graph", args[3]);
    }

    config.setDriverClassName("com.tigergraph.jdbc.Driver");
    config.setUsername("tigergraph");
    config.setPassword("tigergraph");

    if (! config.getDataSourceProperties().containsKey("graph")) {
      config.addDataSourceProperty("graph", "socail");
    }
    config.addDataSourceProperty("filename", "f");
    config.addDataSourceProperty("sep", ",");
    config.addDataSourceProperty("eol", ";");

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
      System.out.println("Running \"RUN QUERY tg_pageRank(v_type=?, e_type=? max_change=?, " +
              "max_iter=?, damping=?, top_k=?, print_accum=?, " +
              "result_attr=?, file_path=?, display_edges=?)\"...");
      String query = "RUN tg_pagerank(v_type=?, e_type=? max_change=?," +
              "max_iter=?, damping=?, top_k=?, print_accum=?," +
              "result_attr=?, file_path=?, display_edges=?)";
      try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
        // Set timeout (in seconds).
        pstmt.setQueryTimeout(60);
        pstmt.setString(1, "person");
        pstmt.setString(2, "liked");
        pstmt.setString(3, "0.001");
        pstmt.setInt(4, 25);
        pstmt.setString(5, "0.85");
        pstmt.setInt(6, 100);
        pstmt.setBoolean(7, true);
        pstmt.setString(8, "");
        pstmt.setString(9, "");
        pstmt.setBoolean(10, false);
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
    } catch (SQLException e) {
        System.out.println( "Failed to getConnection: " + e);
    }
    ds.close();
  }
}
