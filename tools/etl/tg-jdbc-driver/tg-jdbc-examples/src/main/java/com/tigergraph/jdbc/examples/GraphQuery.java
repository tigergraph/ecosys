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
 * data socailNet: https://docs.tigergraph.com/gsql-ref/3.4/querying/appendix-query/example-graphs
 * query ref: https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_list_vertices
 */
public class GraphQuery
{
  public static void main( String[] args ) throws SQLException {
    Properties properties = new Properties();
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

      properties.put("debug", args[2]);

      // Specify the graph name, especially when multi-graph is enabled.
      properties.put("graph", args[3]);
    }

    /**
     * Need to specify username and password once REST++ authentication is enabled.
     */
    properties.put("username", "tigergraph");
    properties.put("password", "tigergraph");

    /**
     * Specify SSL certificate
     */
    properties.put("trustStore", "/tmp/trust.jks");
    properties.put("trustStorePassword", "password");
    properties.put("trustStoreType", "JKS");

    properties.put("keyStore", "/tmp/identity.jks");
    properties.put("keyStorePassword", "password");
    properties.put("keyStoreType", "JKS");


    /**
     * Specify ip address and port of the TigerGraph server.
     * Please use 'https' instead once ssl is enabled.
     */
    StringBuilder sb = new StringBuilder();
    sb.append("jdbc:tg:https://").append(ipAddr).append(":").append(port);

    try {
      com.tigergraph.jdbc.Driver driver = new Driver();

      try (Connection con = driver.connect(sb.toString(), properties)) {
        /**
         * To get vertices of person type whose 'gender' is Female.
         * If the value on the right side of a filter is a string literal, it should be enclosed in double-quotes.
         */
        String query = "get person(filter=?)";
        System.out.println("Running \"get person(filter=?)\"...");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "gender=\"Female\"");
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
         * To get a person which has the given id
         */
        System.out.println();
        query = "get person(id=?)";
        System.out.println("Running \"get person(id=?)\"...");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "person2");
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
         * To get all edges from a given vertex
         */
        System.out.println();
        query = "get edges(person, ?)";
        System.out.println("Running \"get edges(person, ?)\"...");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "person2");
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
         * To get all edges from a given vertex and edge type
         */
        System.out.println();
        query = "get edges(person, ?, posted)";
        System.out.println("Running \"get edges(person, ?, posted)\"...");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "person2");
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
         * To get all edges from a given vertex, edge type and target vertex type
         */
        System.out.println();
        query = "get edges(person, ?, posted, post)";
        System.out.println("Running \"get edges(person, ?, posted, post)\"...");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "person2");
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
         * To get a specific edge from a given vertex to another specific vertex
         */
        System.out.println();
        query = "get edge(person, ?, friend, person, ?)";
        System.out.println("Running \"get edge(person, ?, friend, person, ?)\"...");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "person2");
          pstmt.setString(2, "person3");
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
         * Delete edges with PreparedStatement.
         */
        System.out.println("");
        System.out.println("Running \"DELETE edge(src_vertex_type, src_vertex_id, edge_type, tgt_vertex_type, tgt_vertex_id)\"...");
        query = "DELETE edge(person, ?, liked, post, ?)";
        try {
          java.sql.PreparedStatement pstmt = con.prepareStatement(query);
          pstmt.setString(1, "person3");
          pstmt.setString(2, "0");
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
         * Delete vertices with PreparedStatement.
         */
        System.out.println("");
        System.out.println("Running \"DELETE person(id=?)\"...");
        query = "DELETE post(id=?)";
        try {
          java.sql.PreparedStatement pstmt = con.prepareStatement(query);
          pstmt.setString(1, "0");
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

      } catch (SQLException e) {
          System.out.println( "Failed to getConnection: " + e);
      }
    } catch (SQLException e) {
        System.out.println( "Failed to init Driver: " + e);
    }
  }
}
