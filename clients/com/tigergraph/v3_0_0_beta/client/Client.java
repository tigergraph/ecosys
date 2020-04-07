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

import com.tigergraph.v3_0_0_beta.common.util.error.ReturnCode;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.bind.DatatypeConverter;

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

  private static final String REL_PATH_INFO =
      "If there is any relative path, it is relative to <System.AppRoot>/dev/gdk/gsql";

  /**
   * Maximum number of retry to connect to GSQL server.
   */
  private static final int RETRY_NUM_MAX = 5;

  /**
   * Interval (seconds) of retry to connect to GSQL server.
   */
  private static final int RETRY_INTERVAL = 1;

  /**
   * Base endpoint URL to GSQL server. e.g. http://127.0.0.1:8123/gsql (local)
   * e.g. https://192.168.0.10:14240/gsqlserver/gsql (remote)
   */
  private URI baseEndpoint;

  /**
   * {@code true} if client connects from local; {@code false} in case of remote connection.
   */
  private boolean isLocal;

  /**
   * Path to CA Certificate. {@code null} if not given.
   */
  private String certPath = null;

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
  private String userSession = null;
  private String propertiesString = null; // session parameters
  private String welcomeMessage = null;
  private String shellPrompt = null;

  /**
   * Default GSQL {@code Client} constructor.
   * 
   * @param baseEndpoint Base endpoint URL
   * @param isLocal {@code true} if client is running in localhost; {@code false} otherwise
   */
  public Client(URI baseEndpoint, boolean isLocal) {
    this.baseEndpoint = baseEndpoint;
    this.isLocal = isLocal;
  }

  /**
   * GSQL {@code Client} constructor with CA Certificate.
   * 
   * @param baseEndpoint Base endpoint URL
   * @param isLocal {@code true} if client is running in localhost; {@code false} otherwise
   * @param certPath Path to CA Certificate
   */
  public Client(URI baseEndpoint, boolean isLocal, String certPath) {
    this(baseEndpoint, isLocal);
    this.certPath = certPath;
  }

  /**
   * Build endpoint URL for the given {@code name} based on {@code baseEndpoint}.
   * 
   * @param name Endpoint name
   * @return Full {@code URL} for the endpoint
   */
  private URL getEndpointURL(String name) {
    URL endpoint = null;
    try {
      endpoint = new URL(String.join("/", baseEndpoint.toString(), name));
    } catch (MalformedURLException e) {
      // this does not validate URL and only detects unknown protocol
      System.out.println("Unknown protocol: " + baseEndpoint);
      System.exit(ReturnCode.CLIENT_ARGUMENT_ERROR);
    }
    return endpoint;
  }

  /**
   * Create GSQL CLI instance to parse the CLI arguments.
   * 
   * @param cli {@code GsqlCli} with parsed client args
   */
  public void start(GsqlCli cli) throws Exception {
    // version and help actions
    if (cli.hasVersion()) {
      executePost(getEndpointURL(ENDPOINT_VERSION), "v");
      System.exit(0);
    } else if (cli.hasDetailedVersion()) {
      executePost(getEndpointURL(ENDPOINT_VERSION), "version");
      System.exit(0);
    } else if (cli.hasHelp()) {
      executePost(getEndpointURL(ENDPOINT_HELP), "help");
      System.exit(0);
    } else if (cli.hasLock()) {
      // check the current lock status, internal use only
      executePost(getEndpointURL(ENDPOINT_LOCK), "lock");
      System.exit(0);
    }

    isFromGraphStudio = cli.isFromGraphStudio();

    // login with specified user
    login(cli);

    // Add a shutdown hook, for example, when Ctrl + C, abort loading progress
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        sendPost(getEndpointURL(ENDPOINT_ABORT_CLIENT_SESSION), "abortclientsession");
      }
    });

    if (cli.hasReset()) {
      // Reset can be executed only after login.
      executePost(getEndpointURL(ENDPOINT_RESET), "reset");
    } else if (cli.hasFile() || cli.isReadingFile()) {
      // read from file, either -f option or file name provided right after gsql,
      // e.g. gsql a.gsql
      String inputFileContent = readFile(cli);
      String msg = URLEncoder.encode(inputFileContent, "UTF-8");
      executePost(getEndpointURL(ENDPOINT_FILE), msg);
    } else if (cli.hasCommand()) {
      // read from linux shell, -c option provided right after gsql,
      // e.g. gsql -c ls
      String msg = URLEncoder.encode(cli.getCommand(), "UTF-8");
      executePost(getEndpointURL(ENDPOINT_FILE), msg);
    } else if (!cli.getArgument().isEmpty()) {
      // no option provided and gsql command followed, e.g. gsql ls
      String msg = URLEncoder.encode(cli.getArgument(), "UTF-8");
      executePost(getEndpointURL(ENDPOINT_FILE), msg);
    } else {
      // without anything, gsql shell is started.
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
    if (cli.hasUser()) {
      username = cli.getUser();
    }
    if (cli.hasPassword()) {
      password = cli.getPassword();
    }
    if (cli.hasGraph()) {
      graphName = cli.getGraph();
    }

    if (cli.hasLogdir()) {
      Util.LOG_DIR = cli.getLogdir();
    } else {
      Util.LOG_DIR = System.getProperty("user.home") + "/.gsql_client_log";
    }

    Util.setClientLogFile(username, baseEndpoint.toString(), !isFromGraphStudio);

    JSONObject json;
    // for default user
    if (username.equals(DEFAULT_USER)) {
      json = executeAuth(getEndpointURL(ENDPOINT_LOGIN));
      if (!cli.hasPassword() && json != null && json.optBoolean("error", true)) {
        // if password is changed, let user input password
        if (json.optString("message").contains("Wrong password!")) {
          password = Util.Prompt4Password(false, false, username);
          json = executeAuth(getEndpointURL(ENDPOINT_LOGIN));
        }
      }
    } else {
      // other users
      if (!cli.hasPassword()) {
        // If is not the default user, just ask the user to type in password.
        password = Util.Prompt4Password(false, false, username);
      }
      json = executeAuth(getEndpointURL(ENDPOINT_LOGIN));
    }
    handleLoginResponse(json);
  }

  /**
   * Handle response after login.
   * 
   * @param json {@code JSONObject} of HTTP response
   */
  private void handleLoginResponse(JSONObject json) {
    if (json != null) {System.out.print(json.optString("message")); if (json.optString("message").contains("License expired")){ System.exit(ReturnCode.LOGIN_OR_AUTH_ERROR);} }
    if (json == null || json.optBoolean("error", true)) {
      throw new SecurityException();
    } else if (!json.has("isClientCompatible")) {
      // if server is outdated that doesn't have compatibility check logic,
      // isClientCompatible won't be available and message will be null so manually handle here
      System.out.print("It's most likely your TigerGraph server has been "
          + "upgraded, please follow this document \nto obtain the "
          + "corresponding gsql client from your server installation place. \n"  
          + "https://docs.tigergraph.com/dev/using-a-remote-gsql-client\n");
      System.exit(ReturnCode.CLIENT_COMPATIBILITY_ERROR);
    } else if (!json.optBoolean("isClientCompatible", true)) {
      System.exit(ReturnCode.CLIENT_COMPATIBILITY_ERROR);
    }
    // if not caught by any error, retrieve welcomeMessage, shellPrompt, and serverPath
    welcomeMessage = json.getString("welcomeMessage");
    shellPrompt = json.getString("shellPrompt");
    if (!isLocal) System.out.println(REL_PATH_INFO);
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
      System.out.println("File \"" + fileName + "\" does not exist!");
      return "";
    }

    if (fileNameSet.contains(fileName)) {
      System.out.println("There is an endless loop by using @" + fileName + " cmd recursively.");
      System.exit(ReturnCode.RUNTIME_ERROR);
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
      System.out.println(e.getMessage());
      Util.LogExceptions(e);
      System.exit(ReturnCode.UNKNOWN_ERROR);
    }
    console.setPrompt(prompt);

    // get auto-complete keys, set auto-completer
    try {
      String keys = getInfo(
          getEndpointURL(ENDPOINT_GET_INFO),
          URLEncoder.encode("autokeys", "UTF-8"));
      if (keys != null) {
        console.addCompleter(new AutoCompleter(keys.split(",")));
      }
    } catch (Exception e) {
      Util.LogExceptions(e);
    }

    console.addCompleter(new ArgumentCompleter(new FileNameCompleter()));
    // disable bash function
    console.setExpandEvents(false);

    File cmdlogFile = null;
    try {
      cmdlogFile = Util.getClientLogFile("history", username, baseEndpoint.toString(), false);

      // bind .gsql_shell_history file to console
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
            Util.LogExceptions(e);
          }
        }
      });
    } catch (Exception e) {
      console.setHistoryEnabled(false);
      System.out.println(
          "The file .gsql_shell_history cannot be created! "
          + "Thus, shell command history will not be logged.");
      Util.LogExceptions(e);
    }

    while (true) {
      try {
        String inputCommand = console.readLine();
        if (inputCommand == null) {
          System.exit(ReturnCode.NO_ERROR);
        } else {
          inputCommand = inputCommand.trim();
        }

        if (inputCommand.isEmpty()) {
          continue;
        }

        // exit command
        if (inputCommand.equalsIgnoreCase("exit") || inputCommand.equalsIgnoreCase("quit")) {
          System.exit(0);
        }

        // begin ... end block
        if (inputCommand.equalsIgnoreCase("begin")) {
          String InputBlock = "";
          String subInputCommand = console.readLine();
          while (!subInputCommand.equalsIgnoreCase("end")
              && !subInputCommand.equalsIgnoreCase("abort")) {
            InputBlock += subInputCommand + " ";
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
            executePost(getEndpointURL(ENDPOINT_FILE), msg);
            continue;
          } catch (Exception e) {
            System.out.println("Can't read file " + commandFileName);
            Util.LogExceptions(e);
          }
        }
        String msg = URLEncoder.encode(inputCommand, "UTF-8");
        executePost(getEndpointURL(ENDPOINT_COMMAND), msg);
      } catch (IOException e) {
        Util.LogExceptions(e);
      }
    }
  }

  /**
   * Login to {@code endpoint}.
   * 
   * @param endpoint Endpoint URL to connect
   * @return Server response in {@code JSONObject}
   */
  public JSONObject executeAuth(URL endpoint) {
    HttpURLConnection conn = null;
    JSONObject json = null;
    try {
      // basic auth will become the message in case of login
      String msg = getBasicAuth(true);

      // open connection
      conn = createConnection(endpoint, msg, true);

      // send request
      sendRequestWithRetry(conn, msg);

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
        userSession = cookies.get(0);
        Util.SESSION = userSession;
      }
    } catch (ConnectException e) {
      reportConnectExceptionAndExit(conn);
    } catch (Exception e) {
      json = null; // reset json if there's an exception
      System.out.println(e.getMessage());
      System.out.println(
          "If SSL/TLS is enabled for TigerGraph, please use -cacert with a certificate file, "
          + "check TigerGraph document for details.");
      Util.LogExceptions(e);
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
  public String getInfo(URL endpoint, String msg) {
    HttpURLConnection conn = null;
    String retLine = null;
    try {
      // open connection
      conn = createConnection(endpoint, msg, false);

      // send request
      sendRequestWithRetry(conn, msg);

      // handle response
      if (conn.getResponseCode() == 401) {
        reportUnauthorized(conn.getResponseMessage());
      } else if (conn.getResponseCode() != 200) {
        reportNotOK(msg, conn.getResponseCode(), conn.getResponseMessage());
      } else {
        // get information from response
        Scanner sc = new Scanner(conn.getInputStream());
        if (sc.hasNextLine()) {
          retLine = sc.nextLine();
        }
        sc.close();
      }
    } catch (ConnectException e) {
      reportConnectExceptionAndExit(conn);
    } catch (Exception e) {
      Util.LogExceptions(e);
    } finally {
      disconnectSafely(conn);
    }

    return retLine;
  }

  /**
   * Send {@code msg} to {@code endpoint}, without handling response.
   * 
   * @param endpoint Endpoint URL to connect
   * @param msg Message to send
   */
  public void sendPost(URL endpoint, String msg) {
    HttpURLConnection conn = null;
    try {
      // open connection
      conn = createConnection(endpoint, msg, false);

      // send request
      sendRequestWithRetry(conn, msg);

      // handle response
      if (conn.getResponseCode() == 401) {
        reportUnauthorized(conn.getResponseMessage());
      }
    } catch (ConnectException e) {
      reportConnectExceptionAndExit(conn);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      Util.LogExceptions(e);
    } finally {
      disconnectSafely(conn);
    }
  }

  /**
   * Send {@code msg} to {@code endpoint}, and handle response.
   * 
   * @param endpoint Endpoint URL to connect
   * @param msg Message to send
   */
  public void executePost(URL endpoint, String msg) {
    HttpURLConnection conn = null;
    try {
      // open connection
      conn = createConnection(endpoint, msg, false);

      // send request
      sendRequestWithRetry(conn, msg);

      // handle response
      if (conn.getResponseCode() == 401) {
        reportUnauthorizedAndExit(conn);
      } else if (conn.getResponseCode() != 200) {
        reportNotOK(msg, conn.getResponseCode(), conn.getResponseMessage());
      } else {
        // print out response
        Scanner sc = new Scanner(conn.getInputStream());
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
                Util.LogText("Can't parse return code: " + words[1]);
                System.out.println("Cannot parse the return code");
                System.exit(ReturnCode.NUM_FORMAT_ERROR);
              }
            }
          } else if (line.startsWith("__GSQL__INTERACT__")) {
            // request an interaction with the user
            dialogBox(line);
          } else if (line.startsWith("__GSQL__COOKIES__")) {
            String[] words = line.split(",", 2);
            deserializeCookies(words[1]);
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
      }
    } catch (ConnectException e) {
      reportConnectExceptionAndExit(conn);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      Util.LogExceptions(e);
    } finally {
      disconnectSafely(conn);
    }
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
              getEndpointURL(ENDPOINT_DIALOG),
              URLEncoder.encode(dialogId + "," + pass, "UTF-8"));
        } catch (IOException e) {
          Util.LogExceptions(e);
        }
        break;

      case "AlterPasswordQb":
        try {
          String pass = Util.Prompt4Password(true, true, null);
          sendPost(
              getEndpointURL(ENDPOINT_DIALOG),
              URLEncoder.encode(dialogId + "," + pass, "UTF-8"));
        } catch (UnsupportedEncodingException e1) {
          Util.LogExceptions(e1);
        }
        break;

      case "ExportGraphQb":
        try {
          String pass = Util.Prompt4Password(true, true, null);
          sendPost(
              getEndpointURL(ENDPOINT_DIALOG),
              URLEncoder.encode(dialogId + "," + pass, "UTF-8"));
        } catch (UnsupportedEncodingException e1) {
          Util.LogExceptions(e1);
        }
        break;

      case "ImportGraphQb":
        try {
          String pass = Util.Prompt4Password(true, true, null);
          sendPost(
              getEndpointURL(ENDPOINT_DIALOG),
              URLEncoder.encode(dialogId + "," + pass, "UTF-8"));
        } catch (UnsupportedEncodingException e1) {
          Util.LogExceptions(e1);
        }
        break;

      case "CreateUserQb":
        try {
          String name = Util.Prompt4UserName();
          String pass = Util.Prompt4Password(true, true, null);
          sendPost(
              getEndpointURL(ENDPOINT_DIALOG),
              URLEncoder.encode(dialogId + "," + name + ":" + pass, "UTF-8"));
        } catch (UnsupportedEncodingException e1) {
          Util.LogExceptions(e1);
        }
        break;

      case "CreateTokenQb":
        try {
          String secret = Util.ColorPrompt("Secret : ");
          sendPost(
              getEndpointURL(ENDPOINT_DIALOG),
              URLEncoder.encode(dialogId + "," + secret, "UTF-8"));
        } catch (UnsupportedEncodingException e1) {
          Util.LogExceptions(e1);
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
              getEndpointURL(ENDPOINT_DIALOG),
              URLEncoder.encode(dialogId + "," + command, "UTF-8"));
        } catch (IOException e) {
          Util.LogExceptions(e);
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
              getEndpointURL(ENDPOINT_DIALOG),
              URLEncoder.encode(dialogId + "," + command, "UTF-8"));
        } catch (IOException e) {
          Util.LogExceptions(e);
        }
        break;

      default:
        System.out.println("Undefined action " + qb);
        System.exit(1);
    }
  }

  /**
   * Open a {@code HttpURLConnection} based on {@code endpoint} and set necessary properties.
   * 
   * @param endpoint Endpoint URL
   * @param msg HTTP message body
   * @param isLogin {@code true} if this connection is for login; {@code false} otherwise
   * @return {@code HttpURLConnection}
   * @throws ConnectException if the connection is not created successfully
   */
  private HttpURLConnection createConnection(URL endpoint, String msg, boolean isLogin) 
      throws ConnectException {
    HttpURLConnection conn = null;
    try {
      // open HttpsURLConnection when CA Certificate is given
      if (certPath != null) {
        HttpsURLConnection connSecured = (HttpsURLConnection) endpoint.openConnection();
        connSecured.setSSLSocketFactory(Util.getSSLContext(certPath).getSocketFactory());
        conn = connSecured;
      } else {
        conn = (HttpURLConnection) endpoint.openConnection();
      }
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      conn.setRequestProperty("Content-Language", "en-US");
      conn.setRequestProperty("Content-Length", Integer.toString(msg.getBytes().length));
      conn.setRequestProperty("Cookie", generateCookies());
      conn.setUseCaches(false);
      conn.setDoInput(true);
      conn.setDoOutput(true);
      // skip Authorization if this connection is for login (it should be the param)
      if (!isLogin) {
        conn.setRequestProperty("Authorization", getBasicAuth(false));
      }
    } catch (Exception e) {
      // log exception here; print out error message in caller
      Util.LogExceptions(e);
      throw new ConnectException();
    }
    return conn;
  }

  /**
   * Generate cookies.
   * 
   * @return Serialized {@cod JSONObject}
   */
  private String generateCookies() {
    JSONObject cookieJSON = new JSONObject();

    if (isLocal) {
      cookieJSON.put("CLIENT_PATH", System.getProperty("user.dir"));
    }
    cookieJSON.put("GSHELL_TEST", System.getenv("GSHELL_TEST"));
    cookieJSON.put("COMPILE_THREADS", System.getenv("GSQL_COMPILE_THREADS"));
    cookieJSON.put("TERMINAL_WIDTH", String.valueOf(Util.getTerminalWidth()));
    cookieJSON.put("FromGsqlClient", "true"); // used in handler

    if (graphName != null) {
      cookieJSON.put("graph", graphName);
    }

    if (userSession != null) {
      cookieJSON.put("session", userSession);
    }

    if (propertiesString != null) {
      cookieJSON.put("properties", propertiesString);
    }

    // Add to cookie if caller is from Graph Studio
    if (isFromGraphStudio) {
      cookieJSON.put("FromGraphStudio", "true");
    }

    // get client commit hash to check compatibility
    String commitClient = Util.getCommitClient();
    if (commitClient != null) {
      cookieJSON.put("commitClient", commitClient);
    }

    return cookieJSON.toString();
  }

  /**
   * Deserialize {@code cookies} and retrieve info.
   * 
   * @param cookies Serialized {@code JSONObject}
   */
  private void deserializeCookies(String cookies) {
    JSONObject cookieJSON = new JSONObject(cookies);

    try {
      userSession = cookieJSON.getString("session");
    } catch (Exception e) {
      userSession = null;
    }

    try {
      graphName = cookieJSON.getString("graph");
    } catch (Exception e) {
      graphName = null;
    }

    try {
      propertiesString = cookieJSON.getString("properties");
    } catch (Exception e) {
      propertiesString = null;
    }
  }

  /**
   * Build basic auth for {@code HttpURLConnection}.
   * 
   * @param isLogin {@code true} if this auth is for login; {@code false} otherwise
   * @return Basic auth in {@code String}
   */
  private String getBasicAuth(boolean isLogin) {
    String auth = String.join(":", username, password);
    String auth64 = DatatypeConverter.printBase64Binary(auth.getBytes(StandardCharsets.UTF_8));
    // When logging in, we don't need to put the prfix "Basic"
    return isLogin ? auth64 : String.join(" ", "Basic", auth64);
  }

  /**
   * Try to send HTTP request for {@code RETRY_NUM_MAX} times with {@code RETRY_INTERVAL}.
   * 
   * @param conn {@code HttpURLConnection} to send {@code msg}
   * @param msg HTTP message body
   * @throws InterruptedException from {@code java.util.concurrent.TimeUnit.sleep}
   * @throws ConnectException if the connection is not established successfully
   */
  private void sendRequestWithRetry(HttpURLConnection conn, String msg)
      throws InterruptedException, ConnectException {
    OutputStream os = null;
    for (int i = 0; i < RETRY_NUM_MAX; i++) {
      if (i > 0) {
        TimeUnit.SECONDS.sleep(RETRY_INTERVAL);
      }
      try {
        os = conn.getOutputStream();
        break;
      } catch (IOException e) {
        // silently retry to connect
      }
    }
    if (os == null) {
      throw new ConnectException();
    }
    PrintStream ps = new PrintStream(os);
    ps.print(msg);
    ps.close();
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
    Util.LogText(String.join(" ", errMsg, responseMsg));
    System.out.println(errMsg);
  }

  /**
   * Print out and log error message when the response is HTTP 401 Unauthorized, and then exit.
   * 
   * @param conn {@code HttpURLConnection} to disconnect
   */
  private void reportUnauthorizedAndExit(HttpURLConnection conn) throws IOException {
    reportUnauthorized(conn.getResponseMessage());
    disconnectSafely(conn);
      throw new SecurityException();
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
    Util.LogText(String.join(" ", errMsg, responseMsg));
    System.out.println(errMsg);
    System.out.println("Response Code: " + responseCode);
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
    String[] errMsg = {
      "Connection refused.",
      "Please check the status of GSQL server using \"gadmin status gsql\".",
      "If it's down, please start it on server first by \"gadmin start gsql\".",
      "If you are on a client machine and haven't configured the GSQL server IP address yet,",
      "please create a file called gsql_server_ip_config in the same directory as",
      "gsql_client.jar, containing one item: the GSQL server IP, e.g. 192.168.1.1",
      "Please also make sure the versions of the client and the server are the same.\n"};
    System.out.println(String.join("\n", errMsg));
    //disconnectSafely(conn);
    System.exit(ReturnCode.CONNECTION_ERROR);
  }
}
