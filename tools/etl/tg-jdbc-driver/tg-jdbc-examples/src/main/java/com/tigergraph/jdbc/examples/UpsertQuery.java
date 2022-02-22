package com.tigergraph.jdbc.examples;

import com.tigergraph.jdbc.Driver;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Example code to demonstrate how to invoke TigerGraph pre-installed queries
 * The corresponding TigerGraph demo could be found at:
 * data socailNet and loading job : https://docs.tigergraph.com/gsql-ref/3.4/querying/appendix-query/example-graphs
 */
public class UpsertQuery
{
  public static void main( String[] args) throws SQLException {
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
     * Only accept 4 parameters: IP address, port, debug and graph name.
     * Enable debug when the third parameter's value is larger than 0.
     */
    if (args.length == 4) {
      ipAddr = args[0];
      port = Integer.valueOf(args[1]);

      properties.put("debug", args[2]);

      // Specify the graph name, especially when multi-graph is enabled.
      properties.put("graph", args[3]);
    }

    // Specify the query timeout (in seconds)
    properties.put("timeout", "60");

    // This request is an atomic transaction
    properties.put("atomic", "1");

    // For load balancing
    // properties.put("ip_list", "172.30.2.20,172.30.2.21");

    /**
     * For loading job
     */
    properties.put("filename", "f");
    properties.put("sep", ",");
    properties.put("eol", ";");

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
        System.out.println("Running Statement to insert person and edge.");
        try (java.sql.Statement stmt = con.createStatement()) {
          String query = "INSERT INTO vertex person(id, gender, id) VALUES('person9', 'Male', 'person0')";
          stmt.addBatch(query);
          query = "INSERT INTO edge friend(person, person) VALUES('person9', 'person4')";
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
        System.out.println("Running \"INSERT INTO vertex person(id, gender, id) VALUES(?, ?, ?)\"...");
        String query = "INSERT INTO vertex person(id, gender, id) VALUES(?, ?, ?)";
        try {
          java.sql.PreparedStatement pstmt = con.prepareStatement(query);
          pstmt.setString(1, "person10");
          pstmt.setString(2, "Male");
          pstmt.setString(3, "person10");
          pstmt.addBatch();
          pstmt.setString(1, "person11");
          pstmt.setString(2, "Male");
          pstmt.setString(3, "person11");
          pstmt.addBatch();
          pstmt.setString(1, "person12");
          pstmt.setString(2, "Male");
          pstmt.setString(3, "person12");
          pstmt.addBatch();
          int[] count = pstmt.executeBatch();
          System.out.println("Upsert'ed " + count[0] + " vertices.");
        } catch (SQLException e) {
          System.out.println( "Failed to createStatement: " + e);
        }

        System.out.println("");
        System.out.println("Running \"INSERT INTO edge friend(person, person) VALUES(?, ?)\"...");
        query = "INSERT INTO edge friend(person, person) VALUES(?, ?)";
        try {
          java.sql.PreparedStatement pstmt = con.prepareStatement(query);
          pstmt.setString(1, "person11");
          pstmt.setString(2, "person10");
          pstmt.addBatch();
          int[] count = pstmt.executeBatch();
          System.out.println("Upsert'ed " + count[1] + " edges.");
        } catch (SQLException e) {
          System.out.println( "Failed to createStatement: " + e);
        }

        /**
         * Invoke a loading job
         */
        System.out.println("");
        System.out.println("Running \"INSERT INTO job loadPost(line) VALUES(?)\"...");
        query = "INSERT INTO job loadPost(line) VALUES(?)";
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "10,Graphs,2011-02-04 17:03:41\n");
          pstmt.addBatch();
          pstmt.setString(1, "11,cats,2011-02-04 17:03:42\n");
          pstmt.addBatch();
          pstmt.setString(1, "12,coffee,2011-02-04 17:03:44\n");
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
