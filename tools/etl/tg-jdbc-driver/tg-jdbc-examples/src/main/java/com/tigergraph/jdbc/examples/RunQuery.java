package com.tigergraph.jdbc.examples;

import com.tigergraph.jdbc.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/*
 * Example code to demonstrate how to invoke TigerGraph pre-installed queries and interpreted queries.
 * The graph `Social_Net` and query `page_rank` should be installed first:
 *   https://docs.tigergraph.com/gsql-ref/current/appendix/example-graphs
 *   https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Centrality/pagerank/global/unweighted/tg_pagerank.gsql
 * Ref:
 *   https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_run_an_installed_query_get
 *   https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_run_an_interpreted_query
 */
public class RunQuery {
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
        /*******************************************************************************
         *                         INVOKE PRE-INSTALLED QUERY                          *
         *******************************************************************************/
        System.out.println(
            ">>> Running \"RUN QUERY tg_pageRank(v_type=?, e_type=? max_change=?, "
                + "max_iter=?, damping=?, top_k=?, print_accum=?, "
                + "result_attr=?, file_path=?, display_edges=?)\"...");
        String query =
            "RUN tg_pagerank(v_type=?, e_type=? max_change=?,"
                + "max_iter=?, damping=?, top_k=?, print_accum=?,"
                + "result_attr=?, file_path=?, display_edges=?)";
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          // Set timeout (in seconds).
          pstmt.setQueryTimeout(60);
          pstmt.setString(1, "Person");
          pstmt.setString(2, "Friend");
          pstmt.setString(3, "0.001");
          pstmt.setInt(4, 25);
          pstmt.setString(5, "0.85");
          pstmt.setInt(6, 100);
          pstmt.setBoolean(7, true);
          pstmt.setString(8, "");
          pstmt.setString(9, "");
          pstmt.setBoolean(10, false);
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            printResultSet(rs);
          }
        } catch (SQLException e) {
          System.out.println("Failed to createStatement: " + e);
        }

        /*******************************************************************************
         *                          INVOKE INTERPRETED QUERY                           *
         *******************************************************************************/
        System.out.println("\n>>> Running interpreted query \"run interpreted(a=?, b=?)\"...");
        query = "run interpreted(a=?, b=?)";
        String query_body =
            "INTERPRET QUERY (int a, int b) FOR GRAPH Social_Net {\n" + "PRINT a, b;\n" + "}\n";
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "10");
          pstmt.setString(2, "20");
          pstmt.setString(3, query_body); // The query body is passed as a parameter.
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            printResultSet(rs);
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

  private static void printResultSet(java.sql.ResultSet rs) throws SQLException {
    do {
      System.out.println("");
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
}
