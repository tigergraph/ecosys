/**
 * ***************************************************************************
 * Copyright (c) 2017, TigerGraph Inc.
 * All rights reserved
 * Unauthorized copying of this file, via any medium is
 * strictly prohibited
 * Proprietary and confidential
 * ****************************************************************************
 */
package com.tigergraph.v3_0_0_beta.client;

import com.tigergraph.v3_0_0_beta.common.GsqlLogger;
import com.tigergraph.v3_0_0_beta.common.util.error.ReturnCode;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

/**
 * Driver for GSQL client.
 * Parse commnd line arguments, validate server IP and port,
 * and then create/start GSQL {@code Client}.
 */
public class Driver {
  private static final String LOCALHOST = "127.0.0.1";
  private static final int DEFAULT_PRIVATE_PORT = 8123;
  private static final int DEFAULT_PUBLIC_PORT = 14240;

  /**
   * Full {@code Path} to configuration file in the same directory for remote client connection.
   */
  private static final Path IP_CONFIG_FILE =
      Paths.get(System.getProperty("user.dir"), "gsql_server_ip_config");

  /**
   * Full {@code Path} to ~/.tg.cfg for connection in localhost.
   */
  private static final Path TG_CONFIG_FILE = Paths.get(System.getProperty("user.home"), ".tg.cfg");

  public static void main(String[] args) {

    // parse command line args
    GsqlCli cli = new GsqlCli();
    if (!cli.parse(args)) {
      System.exit(ReturnCode.CLIENT_ARGUMENT_ERROR);
    }

    // get server IP and port
    String serverIP = LOCALHOST;
    int serverPort = DEFAULT_PRIVATE_PORT;
    String serverConnRaw = null;
    if (cli.hasIp()) {
      serverConnRaw = cli.getIp();
    } else if (IP_CONFIG_FILE.toFile().exists()) {
      try (BufferedReader br = new BufferedReader(new FileReader(IP_CONFIG_FILE.toString()))) {
        serverConnRaw = br.readLine();
      } catch (IOException e) {
        System.out.println(String.format(
            "%s is not found. Falling back to default configuration.",
            IP_CONFIG_FILE.toString()));
      }
    } else if (TG_CONFIG_FILE.toFile().exists()) {
      try {
        JSONObject json = new JSONObject(new String(Files.readAllBytes(TG_CONFIG_FILE)));
        serverPort = json.getJSONObject("GSQL").getInt("Port");
      } catch (Exception e) {
        System.out.println(String.format(
            "Error while reading GSQL Port. Falling back to default port %d.",
            DEFAULT_PUBLIC_PORT));
      }
    }
    
    // parse IP and port from argument/IP_CONFIG_FILE in case of remote connection
    boolean isLocal = true;
    if (serverConnRaw != null) {
      String[] serverConn = serverConnRaw.split(":");
      serverIP = serverConn[0];
      if (serverConn.length > 1) {
        try {
          serverPort = Integer.parseInt(serverConn[1]);
        } catch (NumberFormatException e) {
          System.out.println(String.format(
              "Invalid port %s. Falling back to default port %d.",
              serverConn[1],
              DEFAULT_PUBLIC_PORT));
          serverPort = DEFAULT_PUBLIC_PORT;
        }
        System.out.println(String.format("Connecting to %s:%d", serverIP, serverPort));
      } else {
        serverPort = DEFAULT_PUBLIC_PORT;
        System.out.println("Connecting to " + serverIP);
      }
      // mark this flag as false for remote connection
      isLocal = false;
    }

    // build server endpoint URL
    URI serverEndpoint = null;
    try {
      serverEndpoint = new URIBuilder()
          .setScheme(cli.hasCacert() ? "https" : "http")
          .setHost(serverIP)
          .setPort(serverPort)
          .setPath("/" + (isLocal ? "gsql" : "gsqlserver/gsql"))
          .build();
    } catch (URISyntaxException e) {
      System.out.println("Invalid URL: " + e.getInput());
      System.exit(ReturnCode.CLIENT_ARGUMENT_ERROR);
    }

    // create Client object with optional CA Certificate
    Client client = cli.hasCacert()
        ? new Client(serverEndpoint, isLocal, cli.getCacert())
        : new Client(serverEndpoint, isLocal);

    // start Client
    try {
      client.start(cli);
    } catch (Exception e) {
      Util.LogExceptions(e);
      System.out.println("\nGot error: " + e.getMessage()
          + "\nPlease send the log file '" + GsqlLogger.filePath + "' to TigerGraph.\n");

    }
  }
}
