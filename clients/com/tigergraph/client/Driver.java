package com.tigergraph.client;

import com.tigergraph.common.GSQL_LOG;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.tigergraph.common.util.error.ReturnCode;

/**
 * Created by tigergraph on 7/25/17.
 */
public class Driver {
  public static void main(String[] args) {

    GsqlCli cli = new GsqlCli();
    if (!cli.parse(args)) {
      System.exit(ReturnCode.CLIENT_ARGUMENT_ERROR);
    }

    // If there is a config file named gsql_server_ip_config, connect to the ip in it.
    // Else, connect to localhost.
    Client client;
    String relativePathInfo = "If there is any relative path,"
        + " it is relative to tigergraph/dev/gdk/gsql";
    if (cli.hasIp()) {
      client = new Client(cli.getIp(), cli);
      System.out.println("Connecting to " + cli.getIp());
      System.out.println(relativePathInfo);
    } else {
      try (BufferedReader br = new BufferedReader(new FileReader("gsql_server_ip_config"))) {
        String serverIP = br.readLine();
        client = new Client(serverIP, cli);
        System.out.println("Connecting to " + serverIP);
        System.out.println(relativePathInfo);
      } catch (IOException e) {
        // connect to localhost
        client = new Client();
      }
    }

    try {
      client.start(cli);
    } catch (Exception e) {
      Util.LogExceptions(e);
      System.out.println("\nGot error: " + e.getMessage()
          + "\nPlease send the log file '" + GSQL_LOG.LOG_FILE + "' to TigerGraph.\n");
      System.exit(ReturnCode.UNKNOWN_ERROR);
    }

  } // end main
}
