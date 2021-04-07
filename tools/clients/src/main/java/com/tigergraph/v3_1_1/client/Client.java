/**
 * ***************************************************************************
 * Copyright (c) 2017, TigerGraph Inc.
 * All rights reserved
 * Unauthorized copying of this file, via any medium is
 * strictly prohibited
 * Proprietary and confidential
 * ****************************************************************************
 */
package com.tigergraph.v3_1_1.client;

import static com.tigergraph.v3_1_1.client.util.SystemUtils.logger;

import com.tigergraph.v3_1_1.client.util.ConsoleUtils;
import com.tigergraph.v3_1_1.client.util.HttpResponseOperator;
import com.tigergraph.v3_1_1.client.util.IOUtils;
import com.tigergraph.v3_1_1.client.util.RetryableHttpConnection;
import com.tigergraph.v3_1_1.client.util.SystemUtils;
import com.tigergraph.v3_1_1.client.util.SystemUtils.ExitStatus;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import javax.management.RuntimeErrorException;

import org.json.JSONObject;

import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.FileNameCompleter;
import jline.console.history.FileHistory;
import jline.console.history.History;

public class Client {
  private static final String DEFAULT_USER = "tigergraph";
  private static final String DEFAULT_PASSWORD = "tigergraph";

  private static final String ENDPOINT_COMMAND = "command";
  private static final String ENDPOINT_VERSION = "version";
  private static final String ENDPOINT_HELP = "help";
  private static final String ENDPOINT_LOGIN = "login";
  private static final String ENDPOINT_RESET = "reset";
  private static final String ENDPOINT_LOCK = "lock";
  private static final String ENDPOINT_FILE = "file";
  private static final String ENDPOINT_DIALOG = "dialog";
  private static final String ENDPOINT_GET_INFO = "getinfo";
  private static final String ENDPOINT_ABORT_CLIENT_SESSION = "abortclientsession";
  private static final String ENDPOINT_UDF = "userdefinedfunction";

  /** Maximum number of retry to connect to GSQL server. */
  private static final int RETRY_NUM_MAX = 10;

  /**
   * {@code true} if using interactive shell; {@code false} otherwise.
   */
  private boolean isShell = false;

  /**
   * {@code true} if from GraphStudio; {@code false} otherwise.
   */
  private boolean isFromGraphStudio = false;

  private String username = DEFAULT_USER;
  private String password = DEFAULT_PASSWORD;
  private String graphName = null; // login graphname, optional at login
  private String userCookie = null;
  private String welcomeMessage = null;
  private String shellPrompt = null;
  private RetryableHttpConnection retryableHttpConn;
  private String ipString;
  // client session idle timeout second
  private int sessionTimeoutSec = -1;

  /**
   * GSQL {@code Client} constructor
   *
   * @param cli the command line arguments
   */
  public Client(GsqlCli cli, String ipString) {
    if (cli.hasUser()) {
      username = cli.getUser();
    }
    if (cli.hasPassword()) {
      password = cli.getPassword();
    }
    if (cli.hasGraph()) {
      graphName = cli.getGraph();
    }
    this.ipString = ipString;

    System.setProperty("LOG_FILE_NAME", String.format("log.%d", getUniqueIdentifier()));
  }

  /**
   * @param retryableHttpConn a retryable http connection instance
   */
  public void setRetryableHttpConn(RetryableHttpConnection retryableHttpConn) {
    this.retryableHttpConn = retryableHttpConn;
    retryableHttpConn.setMaxRetry(RETRY_NUM_MAX);
  }

  /**
   * Create GSQL CLI instance to parse the CLI arguments.
   *
   * @param cli {@code GsqlCli} with parsed client args
   */
  public void start(GsqlCli cli) throws Exception {
    if (retryableHttpConn == null) {
      throw new NullPointerException("retryableHttpConn is null");
    }

    // version and help actions
    if (cli.hasVersion()) {
      executePost(ENDPOINT_VERSION, "v");
      SystemUtils.exit(ExitStatus.OK);
    } else if (cli.hasDetailedVersion()) {
      executePost(ENDPOINT_VERSION, "version");
      SystemUtils.exit(ExitStatus.OK);
    } else if (cli.hasHelp()) {
      executePost(ENDPOINT_HELP, "help");
      SystemUtils.exit(ExitStatus.OK);
    } else if (cli.hasLock()) {
      // check the current lock status, internal use only
      executePost(ENDPOINT_LOCK, "lock");
      SystemUtils.exit(ExitStatus.OK);
    }

    isFromGraphStudio = cli.isFromGraphStudio();

    // login with specified user
    login(cli);

    // Add a shutdown hook, for example, when Ctrl + C, abort loading progress
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        sendPost(ENDPOINT_ABORT_CLIENT_SESSION, "abortclientsession");
      }
    });

    if (cli.hasReset()) {
      // Reset can be executed only after login.
      executePost(ENDPOINT_RESET, "reset");
    } else if (cli.hasFile() || cli.isReadingFile()) {
      // read from file, either -f option or file name provided right after gsql,
      // e.g. gsql a.gsql
      String inputFileContent = readFile(cli);
      String msg = URLEncoder.encode(inputFileContent, "UTF-8");
      executePost(ENDPOINT_FILE, msg);
    } else if (cli.hasCommand()) {
      // read from linux shell, -c option provided right after gsql,
      // e.g. gsql -c ls
      String msg = URLEncoder.encode(cli.getCommand(), "UTF-8");
      executePost(ENDPOINT_FILE, msg);
    } else if (!cli.getArgument().isEmpty()) {
      // no option provided and gsql command followed, e.g. gsql ls
      String msg = URLEncoder.encode(cli.getArgument(), "UTF-8");
      executePost(ENDPOINT_FILE, msg);
    } else {
      // without anything, gsql shell is started.

      // check session timeout
      String timeout = System.getenv("GSQL_CLIENT_IDLE_TIMEOUT_SEC");
      if (timeout != null) {
        try {
          sessionTimeoutSec = Integer.parseInt(timeout);
          logger.info("idle timeout set to " + sessionTimeoutSec);
        } catch (NumberFormatException e) {
          logger.error(e);
        }
      }
      interactiveShell();
    }
  }

  /**
   * gsql -u $username
   * gsql -g poc_graph
   * or gsql with default username
   *
   * If it is default user, try with default password first.
   * If the default user does not exist, return and exit.
   * If the default password is wrong, ask the user to type in password.
   *
   * @param cli {@code GsqlCli} with parsed client args
   */
  public void login(GsqlCli cli) {
    JSONObject json = new JSONObject();

    // We allow user to retry login twice if unsuccessful password provided
    // If password is provided beforehand, we do not prompt user to retry.
    int retry = cli.hasPassword() ? 1 : 3;
    int count = 0;
    while (count < retry) {
      // for default user
      if (username.equals(DEFAULT_USER)) {
        json = executeAuth(ENDPOINT_LOGIN);
        if (!cli.hasPassword() && json != null && json.optBoolean("error", true)) {
          // if password is changed, let user input password
          if (json.optString("message").contains("Wrong password!")) {
            password = ConsoleUtils.prompt4Password(false, false, username);
            json = executeAuth(ENDPOINT_LOGIN);
          }
        }
      } else {
        // other users
        if (!cli.hasPassword()) {
          // If is not the default user, just ask the user to type in password.
          password = ConsoleUtils.prompt4Password(false, false, username);
        }
        json = executeAuth(ENDPOINT_LOGIN);
      }

      // If login successful we break out of retry loop.
      if (!json.optString("message").contains("Wrong password!")) {
        break;
      }

      // Dont output try again prompt on last attempt. GSQL Server will handle the output to user.
      if (count < retry - 1) {
        SystemUtils.println("Incorrect Password was provided. Please try again.");
      }
      count++;
    }
    handleLoginResponse(json);
  }

  /**
   * Handle response after login.
   *
   * @param json {@code JSONObject} of HTTP response.
   * @return false if incorrect password, this is to handle retry, o/w always return true.
   */
  private boolean handleLoginResponse(JSONObject json) {
    if (json != null) {System.out.print(json.optString("message")); if (json.optString("message").contains("License expired")){ SystemUtils.exit(ExitStatus.LOGIN_OR_AUTH_ERROR);} }
    if (json == null || json.optBoolean("error", true)) {
      logger.error("%s: %s",
          ExitStatus.LOGIN_OR_AUTH_ERROR.toString(),
          json == null ? null : String.valueOf(json.optBoolean("error", true)));
      if (json != null && !json.optBoolean("isClientCompatible", false)) { throw new SecurityException(); } else { SystemUtils.exit(ExitStatus.LOGIN_OR_AUTH_ERROR); }
      return false;
    } else if (!json.has("isClientCompatible")) {
      // if server is outdated that doesn't have compatibility check logic,
      // isClientCompatible won't be available and message will be null so manually handle here
      logger.error("%s: Server is outdated", ExitStatus.LOGIN_OR_AUTH_ERROR.toString());
      SystemUtils.exit(ExitStatus.COMPATIBILITY_ERROR,
          "It's most likely your TigerGraph server has been upgraded.",
          "Please follow this document to obtain the corresponding GSQL client to the server:",
          "https://docs.tigergraph.com/dev/using-a-remote-gsql-client");
    } else if (!json.optBoolean("isClientCompatible", true)) {
      logger.error("%s: Client is incompatible", ExitStatus.LOGIN_OR_AUTH_ERROR.toString());
      SystemUtils.exit(ExitStatus.COMPATIBILITY_ERROR);
    }
    // if not caught by any error, retrieve welcomeMessage, shellPrompt, and serverPath
    welcomeMessage = json.getString("welcomeMessage");
    shellPrompt = json.getString("shellPrompt");
    if (!retryableHttpConn.isLocal()) {
      SystemUtils.println(
          "If there is any relative path, it is relative to <System.AppRoot>/dev/gdk/gsql");
    }
    return true;
  }

  /**
   * Read files based on {@code cli}.
   *
   * @param cli {@code GsqlCli} with parsed client args
   */
  public String readFile(GsqlCli cli) throws Exception {
    String fileName = null;
    String fileValue = cli.getFile();
    if (fileValue == null || fileValue.isEmpty()) {
      fileName = cli.getArgument();
    } else {
      fileName = fileValue;
    }
    return readFile(fileName, new HashSet<String>());
  }

  /**
   * Recursively read the file of {@code fileName}.
   * Keep {@code fileNameSet} to prevent endless loop.
   *
   * @param fileName File name to read
   * @param fileNameSet File names which have been read so far
   * @return Contents in {@code fileName}
   */
  public String readFile(String fileName, Set<String> fileNameSet) throws Exception {
    File file = new File(fileName);
    if (fileName == null || fileName.isEmpty() || !file.isFile()) {
      String errMsgFmt = "File \"%s\" does not exist!";
      logger.error(errMsgFmt, fileName);
      SystemUtils.println(errMsgFmt, fileName);
      return "";
    }

    if (fileNameSet.contains(fileName)) {
      String errMsgFmt = "There is an endless loop by using @%s cmd recursively.";
      logger.error(errMsgFmt, fileName);
      SystemUtils.exit(ExitStatus.RUNTIME_ERROR, errMsgFmt, fileName);
    } else {
      fileNameSet.add(fileName);
    }

    Scanner sc = new Scanner(file);
    String inputFileContent = "";
    String fileNameCmdPattern = "@[^@]*[^;,]";
    while (sc.hasNextLine()) {
      String line = sc.nextLine();
      if (line.trim().matches(fileNameCmdPattern)) {
        inputFileContent += readFile(line.trim().substring(1), fileNameSet) + "\n";
      } else {
        inputFileContent += line + "\n";
      }
    }
    sc.close();
    return inputFileContent;
  }

  /**
   * GSQL interactive shell.
   */
  public void interactiveShell() {
    isShell = true;

    if (!isFromGraphStudio) {
      System.out.println(welcomeMessage);
    }

    // set the console
    String prompt = "\u001B[1;34m" + shellPrompt + "\u001B[0m";

    ConsoleReader console = null;
    try {
      console = new ConsoleReader(
          null, new FileInputStream(FileDescriptor.in), System.out, null, "UTF-8");
    } catch (IOException e) {
      logger.error(e);
      SystemUtils.exit(ExitStatus.UNKNOWN_ERROR, e);
    }
    console.setPrompt(prompt);

    // get auto-complete keys, set auto-completer
    try {
      String keys = getInfo(
          ENDPOINT_GET_INFO,
          URLEncoder.encode("autokeys", "UTF-8"));
      if (keys != null) {
        console.addCompleter(new AutoCompleter(keys.split(",")));
      }
    } catch (Exception e) {
      logger.error(e);
    }

    console.addCompleter(new ArgumentCompleter(new FileNameCompleter()));
    // disable bash function
    console.setExpandEvents(false);

    File cmdlogFile = null;
    try {
      cmdlogFile = Paths.get(
          System.getProperty("LOG_DIR"),
          String.format("history.%d", getUniqueIdentifier())).toFile();

      // bind history file to console
      History cmdHistoryFile = new FileHistory(cmdlogFile);
      console.setHistory(cmdHistoryFile);
      console.setHistoryEnabled(true);

      // Add a shutdown hook, for example, when Ctrl + C, flush the history log.
      final ConsoleReader finalConsole = console;
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          try {
            ((FileHistory) finalConsole.getHistory()).flush();
          } catch (IOException e) {
            logger.error(e);
          }
        }
      });
    } catch (IOException e) {
      console.setHistoryEnabled(false);
      logger.error(e);
      SystemUtils.println(
          "History file %s cannot be created! Thus, shell command history will not be logged.",
          cmdlogFile.getAbsolutePath());
    }

    while (true) {

      Timer timer = getTimer();
      try {
        String inputCommand = console.readLine();
        if (inputCommand == null) {
          SystemUtils.exit(ExitStatus.OK);
        } else {
          inputCommand = inputCommand.trim();
        }

        // reset timer
        if (timer != null) {
          timer.cancel();
        }

        if (inputCommand.isEmpty()) {
          continue;
        }

        // exit command
        if (inputCommand.equalsIgnoreCase("exit") || inputCommand.equalsIgnoreCase("quit")) {
          SystemUtils.exit(ExitStatus.OK);
        }

        // begin ... end block
        if (inputCommand.equalsIgnoreCase("begin")) {
          String InputBlock = "";
          String subInputCommand = console.readLine();
          if (subInputCommand == null) {
            SystemUtils.exit(ExitStatus.OK);
          }
          while (!subInputCommand.equalsIgnoreCase("end")
              && !subInputCommand.equalsIgnoreCase("abort")) {
            InputBlock += subInputCommand + "\n";
            subInputCommand = console.readLine();
          }
          if (subInputCommand.equalsIgnoreCase("abort")) {
            inputCommand = "";
          } else {
            inputCommand = InputBlock;
          }
        }
        String fileNameCmdPattern = "@[^@]*[^;,]";
        if (inputCommand.matches(fileNameCmdPattern)) {
          String commandFileName = inputCommand.substring(1);
          try {
            inputCommand = readFile(commandFileName, new HashSet<String>());
            String msg = URLEncoder.encode(inputCommand, "UTF-8");
            executePost(ENDPOINT_FILE, msg);
            continue;
          } catch (Exception e) {
            logger.error(e);
            SystemUtils.println("Can't read file %s", commandFileName);
          }
        }
        String msg = URLEncoder.encode(inputCommand, "UTF-8");
        executePost(ENDPOINT_COMMAND, msg);
      } catch (IOException e) {
        logger.error(e);
        // reset timer
        if (timer != null) {
          timer.cancel();
        }
      }
    }
  }

  /**
   * Login to {@code endpoint}.
   *
   * @param endpoint Endpoint URL to connect
   * @return Server response in {@code JSONObject}
   */
  public JSONObject executeAuth(String endpoint) {
    HttpURLConnection conn = null;
    JSONObject json = null;
    try {
      logger.info("executeAuth: url: %s", endpoint);
      // basic auth will become the message in case of login
      String msg = getBasicAuth(true);

      // open connection
      conn = createConnection(endpoint, msg, true, "POST", null);

      // handle response
      Scanner sc = new Scanner(conn.getInputStream());
      String result = "";
      while (sc.hasNextLine()) {
        result += sc.nextLine();
      }
      sc.close();
      json = new JSONObject(result);

      List<String> cookies = conn.getHeaderFields().get("Set-cookie");
      if (cookies != null && cookies.size() > 0) {
        userCookie = cookies.get(0);
        logger.setSession(username, retryableHttpConn.getCurrentURI());
        logger.info("Session has been established with cookie %s", userCookie);
      }
    } catch (ConnectException e) {
      logger.error(e);
      reportConnectExceptionAndExit(conn);
    } catch (Exception e) {
      json = null; // reset json if there's an exception
      logger.error(e);
      System.out.println(e.getMessage());
      System.out.println(
          "If SSL/TLS is enabled for TigerGraph, please use -cacert with a certificate file, "
          + "check TigerGraph document for details.");
    } finally {
      disconnectSafely(conn);
    }
    return json;
  }

  /**
   * Send {@code msg} to {@code endpoint}, and return information.
   * Caller is responsible to parse the output.
   *
   * @param endpoint Endpoint URL to connect
   * @param msg Message to send
   * @return Info in {@code String}
   */
  public String getInfo(String endpoint, String msg) {
    StringBuilder retLine = new StringBuilder();
    sendHttpRequest(endpoint, null, msg, "GET", response -> {
      // get information from response
      Scanner sc = new Scanner(response);
      if (sc.hasNextLine()) {
        retLine.append(sc.nextLine());
      }
      sc.close();
    });
    return retLine.toString();
  }

  /**
   * Send a http request to gsql-server, and apply the operator {@code op} with response data.
   * @param endpoint
   * @param param the URL parameters
   * @param data
   * @param method
   * @param op the operator to apply for response data from gsql-server
   * @return true if connection success
   */
  private boolean sendHttpRequest(String endpoint, Map<String, List<String>> param,
      String data, String method, HttpResponseOperator op) {
    HttpURLConnection conn = null;
    try {
      // open connection
      conn = createConnection(endpoint, data, false, method, param);

      // handle response
      if (conn.getResponseCode() == 401) {
        logger.info(conn.getResponseMessage());
        reportUnauthorized(conn.getResponseMessage());
      } else if (conn.getResponseCode() != 200) {
        logger.info(conn.getResponseMessage());
        reportNotOK(data, conn.getResponseCode(), conn.getResponseMessage());
      } else if (op != null) {
        // handle the response
        op.apply(conn.getInputStream());
      }

      return true;
    } catch (ConnectException e) {
      logger.error(e);
      reportConnectExceptionAndExit(conn);
    } catch (Exception e) {
      logger.error(e);
    } finally {
      disconnectSafely(conn);
    }
    return false;
  }

  /**
   * Send {@code msg} to {@code endpoint}, without handling response.
   *
   * @param endpoint Endpoint URL to connect
   * @param msg Message to send
   */
  public void sendPost(String endpoint, String msg) {
    sendHttpRequest(endpoint, null, msg, "POST", null);
  }

  /**
   * Send {@code msg} to {@code endpoint}, and handle response.
   *
   * @param endpoint Endpoint URL to connect
   * @param msg Message to send
   */
  public void executePost(String endpoint, String msg) {
    HttpResponseOperator op = response -> {
      // print out response
      Scanner sc = new Scanner(response);
      String line;
      String cursorMoveup = "__GSQL__MOVE__CURSOR___UP__";
      String cleanLine = "__GSQL__CLEAN__LINE__";
      String progressBarPattern = "\\[=*\\s*\\]\\s[0-9]+%.*";
      String progressBarCompletePattern = "\\[=*\\s*\\]\\s100%[^l]*";

      while (sc.hasNextLine()) {
        line = sc.nextLine();
        if (line.startsWith("__GSQL__RETURN__CODE__")) {
          if (!isShell) {
            String[] words = line.split(",", 2);
            try {
              System.exit(Integer.valueOf(words[1]));
            } catch (NumberFormatException e) {
              String errMsgFmt = "Can't parse return code: %s";
              logger.error(errMsgFmt, words[1]);
              SystemUtils.exit(ExitStatus.UNKNOWN_ERROR, errMsgFmt, words[1]);
            }
          }
        } else if (line.startsWith("__GSQL__INTERACT__")) {
          // request an interaction with the user
          dialogBox(line);
        } else if (line.startsWith("__GSQL__COOKIES__")) {
          String[] words = line.split(",", 2);
          userCookie = words[1];
        } else if (line.startsWith(cursorMoveup)) {
          String[] tokens = line.split(",");
          // print a progress bar
          System.out.print("\u001b[" + tokens[1] + "A"); // move up tokens[1] lines
        } else if (line.startsWith(cleanLine)) {
          // print a progress bar
          System.out.print("\u001b[" + "2K"); // clean the entire current line
        } else if (line.matches(progressBarPattern)) {
          // print a progress bar
          if (line.matches(progressBarCompletePattern)) {
            line += "\n";
          }
          System.out.print("\r" + line);
        } else {
          System.out.println(line);
        }
      }
      sc.close();
    };

    sendHttpRequest(endpoint, null, msg, "POST", op);
  }

  /**
   * Handle server response which requires user interaction in dialog.
   *
   * @param input Server response
   */
  public void dialogBox(String input) {
    String[] inputs = input.split(",", 4);
    String qb = inputs[1];
    String dialogId = inputs[2];
    switch (qb) {
      case "DecryptQb":
        try {
          ConsoleReader console = new ConsoleReader();
          String pass = console.readLine(new Character((char) 0));
          sendPost(
              ENDPOINT_DIALOG,
              URLEncoder.encode(dialogId + "," + pass, "UTF-8"));
        } catch (IOException e) {
          logger.error(e);
        }
        break;

      case "AlterPasswordQb":
      case "ExportGraphQb":
      case "ImportGraphQb":
        try {
          String pass = ConsoleUtils.prompt4Password(true, true, null);
          sendPost(
              ENDPOINT_DIALOG,
              URLEncoder.encode(dialogId + "," + pass, "UTF-8"));
        } catch (UnsupportedEncodingException e1) {
          logger.error(e1);
        }
        break;

      case "CreateUserQb":
        try {
          String name = ConsoleUtils.prompt4UserName();
          String pass = ConsoleUtils.prompt4Password(true, true, null);
          sendPost(
              ENDPOINT_DIALOG,
              URLEncoder.encode(dialogId + "," + name + ":" + pass, "UTF-8"));
        } catch (UnsupportedEncodingException e1) {
          logger.error(e1);
        }
        break;

      case "ClearStoreQb":
        try {
          ConsoleReader console = new ConsoleReader();
          console.setPrompt(
              "Clear store, are you sure? "
              + "This will also shutdown all GSQL services. [y/N]:");
          String command = console.readLine().trim().toLowerCase();
          if (!command.equals("y")) {
            command = "n";
          }
          sendPost(
              ENDPOINT_DIALOG,
              URLEncoder.encode(dialogId + "," + command, "UTF-8"));
        } catch (IOException e) {
          logger.error(e);
        }
        break;

      case "DropDataSourceQb":
        try {
          ConsoleReader console = new ConsoleReader();
          console.setPrompt("Are you sure to drop data source (" + inputs[3] + ")? [y/N]:");
          String command = console.readLine().trim().toLowerCase();
          if (!command.equals("y")) {
            command = "n";
          }
          sendPost(
              ENDPOINT_DIALOG,
              URLEncoder.encode(dialogId + "," + command, "UTF-8"));
        } catch (IOException e) {
          logger.error(e);
        }
        break;

      case "PutFileQb": {
        String filename = inputs[2];
        String path = inputs[3];
        path = checkAndUpdateFilePath(filename, path);
        if (path == null) {
          break;
        }
        String content = IOUtils.get().readFromFile(path);
        if (content == null || !sendHttpRequest(ENDPOINT_UDF,
            Collections.singletonMap("filename", Arrays.asList(filename)),
            content, "PUT", response -> {
              StringBuilder retLine = new StringBuilder();
              // get information from response
              Scanner sc = new Scanner(response);
              if (sc.hasNextLine()) {
                retLine.append(sc.nextLine() + "\n");
              }
              sc.close();
              JSONObject json = new JSONObject(retLine.toString());
              if (json.getBoolean("error")) {
                System.out.println(json.getString("message"));
                System.out.println("PUT " + filename + " failed.");
              } else {
                System.out.println("PUT " + filename + " successfully.");
              }
            })) {
          if (content == null) {
            System.out.println("file " + path + " not exists!");
          }
          System.out.println("PUT " + filename + " failed.");
        }
        break;
      }

      case "GetFileQb": {
        String filename = inputs[2];
        String path = inputs[3];
        String updatedPath = checkAndUpdateFilePath(filename, path);
        if (updatedPath == null) {
          break;
        }
        if (!sendHttpRequest(ENDPOINT_UDF,
            Collections.singletonMap("filename", Arrays.asList(filename)),
            null, "GET", response -> {
              StringBuilder retLine = new StringBuilder();
              // get information from response
              Scanner sc = new Scanner(response);
              if (sc.hasNextLine()) {
                retLine.append(sc.nextLine() + "\n");
              }
              sc.close();
              JSONObject json = new JSONObject(retLine.toString());
              if (json.getBoolean("error")) {
                System.out.println(json.getString("message"));
                System.out.println("GET " + filename + " failed.");
              } else {
                boolean tryWriteToFile = IOUtils.get().writeToFile(
                    updatedPath, json.getString("result"));
                if (tryWriteToFile) {
                  System.out.println("GET " + filename + " successfully.");
                }
              }
            })) {
          System.out.println("GET " + filename + " failed.");
        }
        break;
      }

      default:
        String errMsgFmt = "Undefined action %s";
        logger.error(errMsgFmt, qb);
        SystemUtils.exit(ExitStatus.RUNTIME_ERROR, errMsgFmt, qb);
    }
  }

  /**
   * Get the correct file extension for user-define file, including:
   * TokenBank.cpp, ExprFunctions.hpp, ExprUtil.hpp
   * @param filename
   * @return the corresponding file extension, eg ".hpp"
   */
  private String getUDFFileExtension(String filename) {
    switch (filename) {
      case "TokenBank":
        return ".cpp";
      case "ExprFunctions":
      case "ExprUtil":
        return ".hpp";
      default:
        // should not hit here
        throw new RuntimeException("System Error! Unknown user-define file type: " + filename);
    }
  }
  /**
   * Check and update the file path for user-defined file
   *   1. if a directory is given as end of path, append a file name
   *   2. if a filename is given, check if the file extension is correct
   * @param filename could be TokenBank, ExprFunctions or ExprUtil
   * @param path the given path by user
   * @return the updated path, or null if it's invalid
   */
  private String checkAndUpdateFilePath(String filename, String path) {
    if (path == null || path.isEmpty()) {
      System.out.println("Path cannot be empty!");
      return null;
    }
    Path pathObj = Paths.get(path);
    if (Files.isDirectory(pathObj)) {
      // use a folder here, append the file name
      String fullFileName = filename + getUDFFileExtension(filename);
      path += "/" + fullFileName;
      System.out.println("Path does not include file name. Append it to the path: '" + path + "'.");
      return path;
    } else {
      // use a regular file name, check its extension
      String udfName = pathObj.getFileName().toString();
      String expectedFileExt = getUDFFileExtension(filename);
      if (!udfName.endsWith(expectedFileExt)) {
        String msg = String.format(
            "When call PUT/GET on '%s', must use '%s' as file extension in the path.",
            filename,
            expectedFileExt);
        System.out.println(msg);
        return null;
      }
    }
    return path;
  }
  /**
   * Open a {@code HttpURLConnection} based on {@code endpoint} and set necessary properties.
   *
   * @param endpoint Endpoint URL
   * @param msg HTTP message body
   * @param isLogin {@code true} if this connection is for login; {@code false} otherwise
   * @param method the HttpConnection method
   * @param param the URL parameters
   * @return {@code HttpURLConnection}
   * @throws ConnectException if the connection is not created successfully
   */
  private HttpURLConnection createConnection(String endpoint, String msg, boolean isLogin,
      String method, Map<String, List<String>> param) throws ConnectException {
    try {

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      headers.put("Content-Language", "en-US");
      headers.put("Cookie", generateCookies());
      if (msg != null) {
        headers.put("Content-Length", Integer.toString(msg.getBytes().length));
      }
      // skip Authorization if this connection is for login (it should be the param)
      if (!isLogin) {
        headers.put("Authorization", getBasicAuth(false));
      }

      return retryableHttpConn.connect(endpoint, param, method, msg, headers);
    } catch (Exception e) {
      // log exception here; print out error message in caller
      logger.error(e);
      throw new ConnectException();
    }
  }

  /**
   * Generate cookies.
   *
   * @return Serialized {@cod JSONObject}
   */
  private String generateCookies() {
    if (userCookie != null) {
      // return the cookie got from last connection
      return userCookie;
    }

    // generate new cookies
    JSONObject cookieJSON = new JSONObject();

    if (retryableHttpConn.isLocal()) {
      cookieJSON.put("clientPath", System.getProperty("user.dir"));
    }
    cookieJSON.put("gShellTest", System.getenv("GSHELL_TEST"));
    try {
      cookieJSON.put("compileThread", Integer.valueOf(System.getenv("GSQL_COMPILE_THREADS")));
    } catch (Exception e) {
      // ignore if it is not an integer
    }
    cookieJSON.put("terminalWidth", ConsoleUtils.getConsoleWidth());
    cookieJSON.put("fromGsqlClient", true); // used in handler

    if (graphName != null) {
      cookieJSON.put("graph", graphName);
    }

    // Add to cookie if caller is from Graph Studio
    if (isFromGraphStudio) {
      cookieJSON.put("fromGraphStudio", true);
    }

    // get client commit hash to check compatibility
    String commitClient = getCommitClient();
    if (commitClient != null) {
      cookieJSON.put("clientCommit", commitClient);
    }

    return cookieJSON.toString();
  }

  /**
   * Get commit hash for the client to match with the server.
   *
   * @return Commit hash
   */
  private String getCommitClient() {
  if (true) return "375a182bc03b0c78b489e18a0d6af222916a48d2"; String clientCommitHash = null;
    try {
      Properties props = new Properties();
      InputStream in = Client.class.getClassLoader().getResourceAsStream("Version.prop");
      props.load(in);
      in.close();
      clientCommitHash = props.getProperty("commit_client");
    } catch (Exception e) {
      logger.error(e);
      SystemUtils.println("Can't find Version.prop.");
    }

    return clientCommitHash;
  }

  /**
   * Build basic auth for {@code HttpURLConnection}.
   *
   * @param isLogin {@code true} if this auth is for login; {@code false} otherwise
   * @return Basic auth in {@code String}
   */
  private String getBasicAuth(boolean isLogin) {
    String auth = String.join(":", username, password);
    String auth64 = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    // When logging in, we don't need to put the prfix "Basic"
    return isLogin ? auth64 : String.join(" ", "Basic", auth64);
  }

  /**
   * Disconnect {@code conn} if exists.
   *
   * @param conn {@code HttpURLConnection}
   */
  private void disconnectSafely(HttpURLConnection conn) {
    if (conn != null) {
      conn.disconnect();
    }
  }

  /**
   * Print out and log error message when the response is HTTP 401 Unauthorized.
   *
   * @param responseMsg Message from response
   */
  private void reportUnauthorized(String responseMsg) {
    String errMsg = "Authentication failed.";
    SystemUtils.println(errMsg);
  }

  /**
   * Print out and log error message when the response is HTTP 401 Unauthorized, and then exit.
   *
   * @param conn {@code HttpURLConnection} to disconnect
   */
  private void reportUnauthorizedAndExit(HttpURLConnection conn) throws IOException {
    reportUnauthorized(conn.getResponseMessage());
    disconnectSafely(conn);
    SystemUtils.exit(ExitStatus.LOGIN_OR_AUTH_ERROR);
  }

  /**
   * Print out and log error message when the response is other than HTTP 200 OK.
   *
   * @param requestMsg Message from request
   * @param responseCode Code from response
   * @param responseMsg Message from response
   */
  private void reportNotOK(String requestMsg, int responseCode, String responseMsg) {
    String errMsg = "Connection error.";
    SystemUtils.println(errMsg);
    SystemUtils.println("Response Code: %s", responseCode);
    try {
      System.out.println(URLEncoder.encode(requestMsg, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      // simply ignore the last line if there's an exception
    }
  }

  /**
   * Print out error message when there is an issue in {@code conn}, and then exit.
   *
   * @param conn {@code HttpURLConnection} to disconnect
   */
  private void reportConnectExceptionAndExit(HttpURLConnection conn) {
    String errMsg = String.join("\n",
        "Connection refused.",
        "Please check the status of GSQL server using \"gadmin status gsql\".",
        "If it's down, please start it on server first by \"gadmin start gsql\".",
        "If you are on a client machine and haven't configured the GSQL server IP address yet,",
        "please create a file called gsql_server_ip_config in the same directory as",
        "gsql_client.jar, containing one item: the GSQL server IP, e.g. 192.168.1.1",
        "Please also make sure the versions of the client and the server are the same.\n");
    SystemUtils.exit(ExitStatus.CONNECTION_ERROR, errMsg);
  }

  /**
   * Generate unique identifier based on {@code username} and @{code baseEndpoint}
   * to be used with log/history file name.
   *
   * @return
   */
  private int getUniqueIdentifier() {
    int hash = ipString != null ? ipString.hashCode() : 0;
    return Math.abs((username + "." + hash).hashCode()) % 1000000;
  }

  private Timer getTimer() {
    if (sessionTimeoutSec == -1) {
      // no timeout
      return null;
    }

    // schedule the timer
    TimerTask task = new TimerTask() {
      public void run() {
        logger.info("Session timeout");
        SystemUtils.exit(ExitStatus.SESSION_TIMEOUT,
            "Session timeout after %d seconds idle.", sessionTimeoutSec);
      }
    };
    Timer timer = new Timer();
    timer.schedule(task, sessionTimeoutSec * 1000);
    return timer;
  }
}
