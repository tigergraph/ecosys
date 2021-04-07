/**
 * ***************************************************************************
 * Copyright (c) 2017, TigerGraph Inc.
 * All rights reserved
 * Unauthorized copying of this file, via any medium is
 * strictly prohibited
 * Proprietary and confidential
 * ****************************************************************************
 */
package com.tigergraph.v3_0_5.client;

import static com.tigergraph.v3_0_5.client.util.SystemUtils.ExitStatus;

import com.tigergraph.v3_0_5.client.util.RetryableHttpConnection;
import com.tigergraph.v3_0_5.client.util.SystemUtils;

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
  private static final Path DEFAULT_LOG_DIR =
      Paths.get(System.getProperty("user.home"), ".gsql_client_log");
  private static final String DEFAULT_LOG_FILE_NAME = "log.out";

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
      SystemUtils.exit(ExitStatus.WRONG_ARGUMENT_ERROR);
    }

    // set log/history file path
    if (cli.hasLogdir()) {
      Path logDir = Paths.get(cli.getLogdir());
      // check --logdir if given
      if (!Files.exists(logDir)) {
        // check whether --logdir exists and create if it doesn't
        if (logDir.toFile().mkdirs()) {
          SystemUtils.println("--logdir %s does not exist, so we created it for you.", logDir);
        } else {
          // use DEFAULT_LOG_DIR when --logdir cannot be created
          SystemUtils.println("Cannot create --logdir %s. Log will be in %s",
              logDir, DEFAULT_LOG_DIR);
          logDir = DEFAULT_LOG_DIR;
        }
      }
      System.setProperty("LOG_DIR", logDir.toAbsolutePath().toString());
    } else {
      // use DEFAULT_LOG_DIR if --logdir is not specified
      System.setProperty("LOG_DIR", DEFAULT_LOG_DIR.toString());
    }
    // set log file name as DEFAULT_LOG_FILE_NAME to log activities prior to login.
    // will use user-specific log file name once login
    System.setProperty("LOG_FILE_NAME", DEFAULT_LOG_FILE_NAME);

    // get server IP and port
    int serverPort = DEFAULT_PRIVATE_PORT;
    String serverConnRaw = null;
    if (cli.hasIp()) {
      serverConnRaw = cli.getIp();
    } else if (Files.exists(IP_CONFIG_FILE)) {
      try (BufferedReader br = new BufferedReader(new FileReader(IP_CONFIG_FILE.toString()))) {
        serverConnRaw = br.readLine();
      } catch (IOException e) {
        SystemUtils.println("%s is not found. Falling back to default configuration.",
            IP_CONFIG_FILE.toString());
      }
    } else if (TG_CONFIG_FILE.toFile().exists()) {
      try {
        JSONObject json = new JSONObject(new String(Files.readAllBytes(TG_CONFIG_FILE)));
        serverPort = json.getJSONObject("GSQL").getInt("Port");
      } catch (Exception e) {
        SystemUtils.println("Error while reading GSQL Port. Falling back to default port %d.",
            DEFAULT_PUBLIC_PORT);
      }
    }

    // init client, must do in the beginning since it set the log4j
    Client client = new Client(cli, serverConnRaw);

    RetryableHttpConnection conn = null;
    try {
      conn = new RetryableHttpConnection(
          cli.hasCacert() || cli.hasSsl(), cli.getCacert(), serverPort);

      // parse IP and port from argument/IP_CONFIG_FILE in case of remote connection
      // format is ip:port,ip,ip:port
      if (serverConnRaw != null && !serverConnRaw.isEmpty()) {
        String[] servers = serverConnRaw.split(",");
        for (String server : servers) {
          String[] serverConn = server.split(":");
          String ip = serverConn[0];
          int port;
          if (serverConn.length > 1) {
            try {
              port = Integer.parseInt(serverConn[1]);
            } catch (NumberFormatException e) {
              SystemUtils.println("Invalid port %s. Falling back to default port %d.",
                  serverConn[1], DEFAULT_PUBLIC_PORT);
              port = DEFAULT_PUBLIC_PORT;
            }
            SystemUtils.println("Connecting to %s:%d", ip, port);
          } else {
            port = DEFAULT_PUBLIC_PORT;
            SystemUtils.println("Connecting to %s", ip);
          }
          conn.addIpPort(ip, port);
        }
      } else {
        conn.addLocalHost(serverPort);
      }

      // must set retryable http connection before start
      client.setRetryableHttpConn(conn);

      // start Client
      client.start(cli);
    } catch (SecurityException se) {
      throw new SecurityException(se);
    } catch (Exception e) {
      SystemUtils.exit(ExitStatus.UNKNOWN_ERROR, e);
    }
  }
}
