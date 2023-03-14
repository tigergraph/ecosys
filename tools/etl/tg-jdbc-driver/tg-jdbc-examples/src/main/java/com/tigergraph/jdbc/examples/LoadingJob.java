package com.tigergraph.jdbc.examples;

import com.tigergraph.jdbc.Driver;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/*
 * Example code to demonstrate how to ingest data via TigerGraph Loading Job.
 * Demo graph Social_Net, run `gsql -g Social_Net ls` to check the loading job definition:
 *   https://docs.tigergraph.com/gsql-ref/current/appendix/example-graphs
 * Ref:
 *   https://docs.tigergraph.com/gsql-ref/current/ddl-and-loading/creating-a-loading-job
 *   https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_run_a_loading_job
 * !!! It's recommanded to use run loading job in Spark for large amouts of data ultilizing its
 * parallelism capability. See `SparkLoadingJob.scala`
 */
public class LoadingJob {
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

    // Specify the query timeout (in seconds)
    properties.put("timeout", "60");

    // This request is an atomic transaction
    properties.put("atomic", "1");

    // For load balancing. Please don't enable this when m1's load is heavy.
    // properties.put("ip_list", "172.30.2.20,172.30.2.21");

    /** For loading job */
    // The filename defined in loading job
    properties.put("filename", "f");
    // The character that separates columns in the data line
    properties.put("sep", ",");
    // The end-of-line character used to concat the lines added by `addBatch()`
    properties.put("eol", ";");

    String[] rows = {
      "10,Graphs,2011-02-04 17:03:41",
      "11,cats,2011-02-04 17:03:42",
      "12,coffee,2011-02-04 17:03:44",
      "13,apple,2011-02-04 17:03:45",
      "14,car,2011-02-04 17:03:46",
    };

    // How many rows will be carried in a single post request.
    // A good practice is to let BATCH_SIZE * SIZE_PER_ROW ~= 1~10MB
    final int BATCH_SIZE = 1000;

    /**
     * Specify ip address and port of the TigerGraph server. Please use 'https' instead once ssl is
     * enabled.
     */
    StringBuilder sb = new StringBuilder();
    sb.append("jdbc:tg:http://").append(ipAddr).append(":").append(port);

    try {
      com.tigergraph.jdbc.Driver driver = new Driver();

      try (Connection con = driver.connect(sb.toString(), properties)) {
        /** Invoke a loading job: `insert into job JOB_NAME(line) values(?)` */
        // - CREATE LOADING JOB load_post FOR GRAPH Social_Net {
        //   DEFINE FILENAME f;
        //   LOAD f TO VERTEX Post VALUES($0, $1, $2) USING SEPARATOR=",", HEADER="false", EOL="\n";
        // }
        System.out.println("");
        System.out.println("Running \"INSERT INTO JOB load_post(line) VALUES(?)\"...");
        String query = "INSERT INTO JOB load_post(line) VALUES(?)";
        try (java.sql.PreparedStatement pstmt = con.prepareStatement(query)) {
          for (int i = 0; i < rows.length; i++) {
            pstmt.setString(1, rows[i]);
            pstmt.addBatch();
            // everytime reaching the batch size, send the batch to TG
            if ((i + 1) % BATCH_SIZE == 0 || i + 1 == rows.length) {
              int[] count = pstmt.executeBatch();
              System.out.println("Accepted " + count[0] + " lines");
              pstmt.clearBatch();
            }
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
}
