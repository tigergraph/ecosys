package com.tigergraph.jdbc.examples;

import com.tigergraph.jdbc.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/*
 * Example code to demonstrate how to query/upsert/delete certain vertices & edges.
 * The corresponding TigerGraph demo graph Social_Net:
 *    https://docs.tigergraph.com/gsql-ref/current/appendix/example-graphs
 * query ref:
 *    https://docs.tigergraph.com/tigergraph-server/current/api/upsert-rest
 *    https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_vertices
 *    https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_edges
 */
public class CRUD {
  public static void main(String[] args) throws SQLException {
    Properties properties = new Properties();
    String ipAddr = "127.0.0.1";
    // port of GraphStudio
    Integer port = 14240;

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
         *                   UPSERT VERTICES OR EDGES INTO THE GRAPH                   *
         *******************************************************************************/
        // !!! It's not recommanded to use it to load large amounts of data, please use loading job
        // instead. See `LoadingJobSpark.scala`.

        /*
         * Upsert vertices:
         *    insert into vertex VERTEX_TYPE(PRIMARY_ID, ATTR1, ATTR2, ATTR3) values(?, ?, ?, ?)
         * The primary_id should be placed first.
         */
        System.out.println("");
        System.out.println(">>> Upsert into vertex Person");
        String query = "insert into vertex Person(person_id, id, gender) values(?, ?, ?)";
        try {
          java.sql.PreparedStatement pstmt = con.prepareStatement(query);
          pstmt.setString(1, "person_0");
          pstmt.setString(2, "id_0");
          pstmt.setString(3, "Male");
          pstmt.addBatch();
          pstmt.setString(1, "person_1");
          pstmt.setString(2, "id_1");
          pstmt.setString(3, "Female");
          pstmt.addBatch();
          int[] count = pstmt.executeBatch();
          System.out.println("Upsert'ed " + count[0] + " vertices.");
        } catch (SQLException e) {
          System.out.println("Failed to createStatement: " + e);
        }

        /*
         * Upsert edges:
         * insert into edge EDGE_TYPE(FROM_TYPE, TO_TYPE, ATTRIBUTES...) values (FROM_ID, TO_ID, ATTRIBUTE...)
         */
        System.out.println("");
        System.out.println(">>> Upsert into edge Liked");
        query = "insert into edge Liked(Person, Post, action_time) values(?, ?, ?)";
        try {
          java.sql.PreparedStatement pstmt = con.prepareStatement(query);
          pstmt.setString(1, "person1");
          pstmt.setInt(2, 10);
          pstmt.setString(3, "2020-01-01 10:00:00");
          pstmt.addBatch();
          int[] count = pstmt.executeBatch();
          System.out.println("Upsert'ed " + count[1] + " edges.");
        } catch (SQLException e) {
          System.out.println("Failed to createStatement: " + e);
        }

        /*******************************************************************************
         *                      RETRIEVE VERTICES FROM THE GRAPH                       *
         *******************************************************************************/

        /*
         * List all the vertices of the given type:
         *    get vertex(VERTEX_TYPE) [params(select=?,filter=?,limit=?,sort=?)]
         * The params are optional:
         *    https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_parameters_18
         */
        System.out.println("");
        query = "get vertex(Person) params(filter=?,limit=?)";
        System.out.println(">>> List 5 vertices from Person whose gender is Female");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "gender=\"Female\"");
          pstmt.setInt(2, 5);
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            printResultSet(rs);
          }
        } catch (SQLException e) {
          System.out.println("Failed to createStatement: " + e);
        }

        /*
         * To get a vertex which has the given type and primary id:
         *    get vertex(VERTEX_TYPE,VERTEX_ID) [params(select=?)]
         */
        System.out.println();
        query = "get vertex(Person,?)";
        System.out.println(">>> Get the vertex from Person whose vertex id is person2");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "person2");
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            printResultSet(rs);
          }
        } catch (SQLException e) {
          System.out.println("Failed to createStatement: " + e);
        }

        /*******************************************************************************
         *                        RETRIEVE EDGES FROM THE GRAPH                        *
         *******************************************************************************/

        /*
         * To get all edges which are connected to a given vertex in the graph:
         *    get edge(SRC_VERTEX_TYPE, SRC_VERTEX_ID) [params(select=?,filter=?,limit=?,sort=?)]
         * The params are optional:
         *    https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_parameters_23
         */
        System.out.println();
        query = "get edge(Person, ?) ";
        System.out.println(">>> Get the edges which are from the Person 'person2'");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "person2");
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            printResultSet(rs);
          }
        } catch (SQLException e) {
          System.out.println("Failed to createStatement: " + e);
        }

        /*
         * To get all edges of a specified type which are connected to a given vertex in the graph:
         *    get edge(SRC_VERTEX_TYPE, SRC_VERTEX_ID, EDGE_TYPE) [params(select=?,filter=?,limit=?,sort=?)]
         * The params are optional:
         *    https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_parameters_24
         */
        System.out.println();
        query = "get edge(Person, ?, Posted)";
        System.out.println(">>> Get the edges of type Posted from the Person 'person2'");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "person2");
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            printResultSet(rs);
          }
        } catch (SQLException e) {
          System.out.println("Failed to createStatement: " + e);
        }

        /*
         * To get edges of a vertex by edge type and target vertex type:
         *    get edge(SRC_VERTEX_TYPE,SRC_VERTEX_ID, EDGE_TYPE, TGT_VERTEX_TYPE) [params(select=?,filter=?,limit=?,sort=?)]
         * The params are optional:
         *    https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_parameters_25
         */
        System.out.println();
        query = "get edge(Person, ?, Posted, Post)";
        System.out.println(
            ">>> Get the edges of type Posted from the Person 'person2' to type Post");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "person2");
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            printResultSet(rs);
          }
        } catch (SQLException e) {
          System.out.println("Failed to createStatement: " + e);
        }

        /*
         * To get a specific edge from a given vertex to another specific vertex:
         *    get edge(SRC_VERTEX_TYPE, SRC_VERTEX_ID, EDGE_TYPE, TGT_VERTEX_TYPE, TGT_VERTEX_ID) [params(select=?)]
         * The param 'select' is optional:
         *    https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_parameters_26
         */
        System.out.println();
        query = "get edge(Person, ?, Posted, Post, ?)";
        System.out.println(">>> Get the edge of type Posted from the Person 'person2' to Post '1'");
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          pstmt.setString(1, "person2");
          pstmt.setInt(2, 1);
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            printResultSet(rs);
          }
        } catch (SQLException e) {
          System.out.println("Failed to createStatement: " + e);
        }

        /*******************************************************************************
         *                       DELETE VERTICES FROM THE GRAPH                        *
         *******************************************************************************/

        /*
         * delete vertex(vertex_type) [params(filter=?,limit=?,sort=?)]
         * delete vertex(vertex_type, vertex_id)
         * params:
         *    https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_parameters_20
         */
        System.out.println("");
        System.out.println(">>> Delete the top 5 vertices of Post ordered by 'post_time'");
        query = "delete vertex(Post) params(limit=?, sort=?)";
        try {
          java.sql.PreparedStatement pstmt = con.prepareStatement(query);
          pstmt.setInt(1, 5);
          pstmt.setString(2, "post_time");
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
            printResultSet(rs);
          }
        } catch (SQLException e) {
          System.out.println("Failed to createStatement: " + e);
        }

        /*******************************************************************************
         *                        DELETE AN EDGE FROM THE GRAPH                        *
         *******************************************************************************/

        /*
         * delete edge(src_vertex_type, src_vertex_id, edge_type, tgt_vertex_type, tgt_vertex_id)
         */
        System.out.println("");
        System.out.println(
            ">>> Delete the edge of Friend from Person 'person1' to Person 'person2'");
        query = "delete edge(Person, ?, Friend, Person, ?)";
        try {
          java.sql.PreparedStatement pstmt = con.prepareStatement(query);
          pstmt.setString(1, "person1");
          pstmt.setString(2, "person2");
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
