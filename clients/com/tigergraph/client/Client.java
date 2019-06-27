package com.tigergraph.client;

import com.tigergraph.common.Constant;
import com.tigergraph.common.util.error.ReturnCode;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.bind.DatatypeConverter;
import java.util.List;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.FileNameCompleter;
import jline.console.history.FileHistory;
import jline.console.history.History;
import org.json.JSONObject;

public class Client {

  private static final String DEFAULT_SERVER_IP = "http://127.0.0.1";
  private static final String DEFAULT_SERVER_ENDPOINT = DEFAULT_SERVER_IP + ":"
      + Constant.GSQLSHELL_REST_SERVER_PORT + "/gsql/";
  private static final String PUBLIC_PORT = "14240";
  private String serverEndpoint = DEFAULT_SERVER_ENDPOINT;
  private String commandEndpoint = DEFAULT_SERVER_ENDPOINT + "command";
  private String versionEndpoint = DEFAULT_SERVER_ENDPOINT + "version";
  private String helpEndpoint = DEFAULT_SERVER_ENDPOINT + "help";
  private String loginEndpoint = DEFAULT_SERVER_ENDPOINT + "login";
  private String resetEndpoint = DEFAULT_SERVER_ENDPOINT + "reset";
  private String fileEndpoint = DEFAULT_SERVER_ENDPOINT + "file";
  private String dialogEndpoint = DEFAULT_SERVER_ENDPOINT + "dialog";
  private String getinfoEndpoint = DEFAULT_SERVER_ENDPOINT + "getinfo";
  private String abortclientsessionEndpoint = DEFAULT_SERVER_ENDPOINT + "abortclientsession";
  private static final String DEFAULT_USER = "tigergraph";
  private static final String DEFAULT_PASSWORD = "tigergraph";
  private String username = DEFAULT_USER;
  private String password = DEFAULT_PASSWORD;
  private String graphName = null; // login graphname, optional at login
  private String userSession = null;
  private String propertiesString = null; // session parameters
  private boolean isLocal = false;
  private boolean isShell = false;
  private boolean isHttps = false;
  private boolean isFromGraphStudio = false;
  private String theCAFilename = null;
  private String welcomeMessage = null;
  private String shellPrompt = null;
  private static final String CONNECTION_ERR_MSG
      = "Connection refused.\n"
      + "Please check the status of GSQL server using \"gadmin status gsql\".\n"
      + "If it's down, please start it on server first by \"gadmin start gsql\".\n"
      + "If you are on a client machine and haven't configured the GSQL server IP address yet,\n"
      + "please create a file called gsql_server_ip_config in the same directory as \n"
      + "gsql_client.jar, containing one item: the GSQL server IP, e.g. 192.168.1.1\n"
      + "Please also make sure the versions of the client and the server are the same.\n";
  /**
   * Default constructor when connecting to the localhost.
   */
  public Client() {
    // Step 1. If there is no ip address given to the client,
    //         the client try to get the configured port.
    // Step 2. If no configured port is gotten, use the default one.
    isLocal = true;
    String configuredLocalPort = Util.getIUMConfig("gsql.server.private_port");
    if (configuredLocalPort != null) {
      serverEndpoint = DEFAULT_SERVER_IP + ":" + configuredLocalPort + "/gsql/";
      refreshEndpoints();
    }
  }

  /**
   * Constructor when use the IP address in the config file
   * @param serverIpAddress the IP in the config file
   */
  public Client(String serverIpAddress, GsqlCli cliArgs) {
    // Step 1. If an ip address is given to the client,
    //         the client try to get the configured port.
    // Step 2. If no configured port is gotten, use the default PUBLIC_PORT.
    isHttps = cliArgs.hasCacert();
    String protocol = isHttps ? "https://" : "http://";
    if (isHttps) {
      theCAFilename = cliArgs.getCacert();
    }
    if (serverIpAddress.contains(":")) {
      serverEndpoint = protocol + serverIpAddress + "/gsqlserver/gsql/";
    } else {
      serverEndpoint = protocol + serverIpAddress + ":"
          + PUBLIC_PORT + "/gsqlserver/gsql/";
    }

    try {
      URL urlChecker = new URL(serverEndpoint);
    } catch (MalformedURLException e) {
      Util.LogText("Illegal IP: " + serverEndpoint);
      System.out.println("The given ip " + serverIpAddress + " is illegal");
      System.exit(0);
    }

    refreshEndpoints();
    isLocal = false;
  }

  private void refreshEndpoints() {
    commandEndpoint = serverEndpoint + "command";
    versionEndpoint = serverEndpoint + "version";
    helpEndpoint = serverEndpoint + "help";
    loginEndpoint = serverEndpoint + "login";
    resetEndpoint = serverEndpoint + "reset";
    fileEndpoint = serverEndpoint + "file";
    dialogEndpoint = serverEndpoint + "dialog";
    //this endpoint can take different parameters, for each parameter, server return
    //different info string.
    getinfoEndpoint = serverEndpoint + "getinfo";
    abortclientsessionEndpoint = serverEndpoint + "abortclientsession";
  }

  /**
   * create GSQL CLI instance to parse the CLI arguments
   * @param cli parsed client args
   */
  public void start(GsqlCli cli) throws Exception {

    // version and help actions
    if (cli.hasVersion()) {
      executePost(versionEndpoint, "v");
      System.exit(0);
    } else if (cli.hasDetailedVersion()) {
      executePost(versionEndpoint, "version");
      System.exit(0);
    } else if (cli.hasHelp()) {
      executePost(helpEndpoint, "help");
      System.exit(0);
    }

    isFromGraphStudio = cli.isFromGraphStudio();

    // login with specified user
    login(cli);

    // Add a shutdown hook, for example, when Ctrl + C, abort loading progress
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        sendPost(abortclientsessionEndpoint, "abortclientsession");
      }
    });

    if (cli.hasReset()) {
      // Reset can be executed only after login.
      executePost(resetEndpoint, "reset");
    } else if (cli.hasFile() || cli.isReadingFile()) {
      // read from file, either -f option or file name provided right after gsql,
      // e.g. gsql a.gsql
      String inputFileContent = readFile(cli);
      String urlParameters = URLEncoder.encode(inputFileContent, "UTF-8");
      executePost(fileEndpoint, urlParameters);
    } else if (cli.hasCommand()) {
      // read from linux shell, -c option provided right after gsql,
      // e.g. gsql -c ls
      String urlParameters = URLEncoder.encode(cli.getCommand(), "UTF-8");
      executePost(fileEndpoint, urlParameters);
    } else if (!cli.getArgument().isEmpty()) {
      // no option provided and gsql command followed, e.g. gsql ls
      String urlParameters = URLEncoder.encode(cli.getArgument(), "UTF-8");
      executePost(fileEndpoint, urlParameters);
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

    Util.setClientLogFile(username, serverEndpoint, !isFromGraphStudio);

    JSONObject json;
    // for default user
    if (username.equals(DEFAULT_USER)) {
      json = executeAuth(loginEndpoint);
      if (!cli.hasPassword() && json != null && json.optBoolean("error", true)) {
        // if password is changed, let user input password
        if (json.optString("message").contains("Wrong password!")) {
          password = Util.Prompt4Password(false, false, username);
          json = executeAuth(loginEndpoint);
        }
      }
    } else {
      // other users
      if (!cli.hasPassword()) {
        // If is not the default user, just ask the user to type in password.
        password = Util.Prompt4Password(false, false, username);
      }
      json = executeAuth(loginEndpoint);
    }
    handleServerResponse(json);
  }

  private void handleServerResponse(JSONObject json) {
    if (json != null) System.out.print(json.optString("message"));
    if (json == null || json.optBoolean("error", true)) {
      System.exit(ReturnCode.LOGIN_OR_AUTH_ERROR);
    } else if (!json.has("isClientCompatible")) {
      // if server is outdated that doesn't have compatibility check logic,
      // isClientCompatible won't be available and message will be null so manually handle here
      System.out.print("GSQL client is not compatible with GSQL server. "
          + "Please contact support@tigergraph.com\n");
      System.exit(ReturnCode.CLIENT_COMPATIBILITY_ERROR);
    } else if (!json.optBoolean("isClientCompatible", true)) {
      System.exit(ReturnCode.CLIENT_COMPATIBILITY_ERROR);
    }
    // if not caught by any error, retrieve welcomeMessage and shellPrompt
    welcomeMessage = json.getString("welcomeMessage");
    shellPrompt = json.getString("shellPrompt");
  }

  /**
   * read files based on client args
   * @param cli parsed client args
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
   * read files based on file name
   * @param fileName parsed client args
   * @param fileNameSet keep the stack of the file names, which helps detect dead loop.
   * @return the string of the file content
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
   * gsql interactive shell
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
      console =
        new ConsoleReader(
            null, new FileInputStream(FileDescriptor.in), System.out, null, "UTF-8");
    } catch (IOException e) {
      System.out.println("Got error: " + e.getMessage());
      Util.LogExceptions(e);
      System.exit(ReturnCode.UNKNOWN_ERROR);
    }
    console.setPrompt(prompt);

    //get auto-complete keys, set auto-completer
    try{
      String keys = getInfo(getinfoEndpoint, URLEncoder.encode("autokeys", "UTF-8"));
      if (keys != null) {
        console.addCompleter(new AutoCompleter(keys.split(",")));
      }
    } catch (Exception e){
      Util.LogExceptions(e);
    }

    console.addCompleter(new ArgumentCompleter(new FileNameCompleter()));
    //disable bash function
    console.setExpandEvents(false);

    File cmdlogFile = null;
    try {
      cmdlogFile = Util.getClientLogFile("history", username, serverEndpoint, false);

      //bind .gsql_shell_history file to console
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
          "The file .gsql_shell_history cannot be created! Thus, "
              + "shell command history will not be logged.");
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
        if (inputCommand.equalsIgnoreCase("exit")
            || inputCommand.equalsIgnoreCase("quit")) {

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
            String urlParameters = URLEncoder.encode(inputCommand, "UTF-8");
            executePost(fileEndpoint, urlParameters);
            continue;
          } catch (Exception e) {
            System.out.println("Can't read file " + commandFileName);
            Util.LogExceptions(e);
          }
        }

        String urlParameters = URLEncoder.encode(inputCommand, "UTF-8");
        executePost(commandEndpoint, urlParameters);
      } catch (IOException e) {
        Util.LogExceptions(e);
      }
    }
  }

  /**
   * send a command to targetURL , return a string of info
   * @param targetURL the server url to be connected
   * @param urlParameters the command to be sent
   * @return the info in a string. the caller should be 
   * responsible to parse the string.
   */
  public String getInfo(String targetURL, String urlParameters) {
    URL url;
    HttpURLConnection connection = null;
    String retLine = null;

    try {
      //Create connection
      url = new URL(targetURL);
      connection = isHttps ? (HttpsURLConnection)url.openConnection()
          : (HttpURLConnection)url.openConnection();
      initConnection(connection);

      // authentication
      String userPass = username + ":" + password;
      String basicAuth = "Basic "
        + DatatypeConverter.printBase64Binary(userPass.getBytes(StandardCharsets.UTF_8));
      connection.setRequestProperty("Authorization", basicAuth);

      connection.setRequestProperty("Content-Length", ""
          + Integer.toString(urlParameters.getBytes().length));
      connection.setRequestProperty("Cookie", userSession);

      //Send request
      PrintStream ps = new PrintStream(connection.getOutputStream());
      ps.print(urlParameters);
      ps.close();

      if (connection.getResponseCode() == 401) {
        Util.LogText("Authentication failed. " + connection.getResponseMessage());
        System.out.println("Authentication failed.");
        if (connection != null) {
          connection.disconnect();
        }
        return null;
      } else if (connection.getResponseCode() != 200) {
        String errMsg = "Connection Error.\n"
          + "Response Code : " + connection.getResponseCode() + "\n"
          + URLEncoder.encode(urlParameters, "UTF-8");
        Util.LogText("Connection error: " + connection.getResponseMessage());
        System.out.println(errMsg);
        if (connection != null) {
          connection.disconnect();
        }
        return null;
      }

      // Get response and print it.
      InputStream is = connection.getInputStream();
      Scanner sc = new Scanner(is);
      if (sc.hasNextLine()) {
        retLine = sc.nextLine();
      }
      sc.close();
      is.close();

    } catch (ConnectException e) {
      System.out.println(CONNECTION_ERR_MSG);
      System.exit(ReturnCode.CONNECTION_ERROR);
    } catch (Exception e) {
      Util.LogExceptions(e);
    } finally {

      if (connection != null) {
        connection.disconnect();
      }
    }

    return retLine;
  }



  /**
   * send a command to targetURL and print out the response
   * @param targetURL the server url to be connected
   * @param urlParameters the command to be sent
   */
  public void executePost(String targetURL, String urlParameters) {
    URL url;
    HttpURLConnection connection = null;
    try {
      //Create connection
      url = new URL(targetURL);
      connection = isHttps ? (HttpsURLConnection)url.openConnection()
          : (HttpURLConnection)url.openConnection();
      initConnection(connection);

      // authentication
      String userPass = username + ":" + password;
      String basicAuth = "Basic "
        + DatatypeConverter.printBase64Binary(userPass.getBytes(StandardCharsets.UTF_8));
      connection.setRequestProperty("Authorization", basicAuth);

      connection.setRequestProperty("Content-Length", ""
          + Integer.toString(urlParameters.getBytes().length));
      connection.setRequestProperty("Cookie", generateCookies());

      //Send request
      PrintStream ps = new PrintStream(connection.getOutputStream());
      ps.print(urlParameters);
      ps.close();

      if (connection.getResponseCode() == 401) {
        Util.LogText("Authentication failed. " + connection.getResponseMessage());
        System.out.println("Authentication failed.");
        if (connection != null) {
          connection.disconnect();
        }
        System.exit(ReturnCode.LOGIN_OR_AUTH_ERROR);
      } else if (connection.getResponseCode() != 200) {
        String errMsg = "Connection Error.\n"
          + "Response Code : " + connection.getResponseCode() + "\n"
          + URLEncoder.encode(urlParameters, "UTF-8") + "\n";
        System.out.println(errMsg);
        if (connection != null) {
          connection.disconnect();
        }
        return;
      }

      // Get response and print it.
      InputStream is = connection.getInputStream();
      Scanner sc = new Scanner(is);
      String line;
      String cursorMoveup = "__GSQL__MOVE__CURSOR___UP__";
      String cleanLine = "__GSQL__CLEAN__LINE__";
      String progressBarPattern = "\\[=*\\s*\\]\\s[0-9]+%.*";
      String progressBarCompletePattern = "\\[=*\\s*\\]\\s100%[^l]*";

      while (sc.hasNextLine()) {
        line = sc.nextLine();
        if (line.startsWith("__GSQL__RETURN__CODE__")) {
          if (!isShell) {
            String [] words = line.split(",", 2);
            try {
              int errCode = Integer.valueOf(words[1]);
              System.exit(errCode);
            } catch (NumberFormatException e) {
              Util.LogText("Can't parse return code: " + words[1]);
              System.out.println("Cannot parse the return code");
              System.exit(ReturnCode.NUM_FORMAT_ERROR);
            }
          }
        } else if (line.startsWith("__GSQL__INTERACT__")) {
          // request an interaction with the user
          dialogBox(line);
        } else if (line.startsWith("__GSQL__COOKIES__")){
          String [] words = line.split(",", 2);
          deserializeCookies(words[1]);
        } else if (line.startsWith(cursorMoveup)) {
          String [] tokens = line.split(",");
          // print a progress bar
          System.out.print("\u001b[" + tokens[1] +"A");//move up tokens[1] lines
        } else if (line.startsWith(cleanLine)) {
          // print a progress bar
          System.out.print("\u001b[" + "2K");//clean the entire current line
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
      is.close();

    } catch (ConnectException e) {
      System.out.println(CONNECTION_ERR_MSG);
      System.exit(ReturnCode.CONNECTION_ERROR);
    } catch (Exception e) {
      System.out.println("Got error: " + e.getMessage());
      Util.LogExceptions(e);
    } finally {

      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  /**
   * handle the situation that the server response indicates
   * the user type in an interactive message
   * @param input the response from the server
   */
  public void dialogBox(String input) {
    String [] inputs = input.split(",", 4);
    String qb = inputs[1];
    String dialogId = inputs[2];
    switch (qb) {
      case "DecryptQb":
        try {
          ConsoleReader console = new ConsoleReader();
          String pass = console.readLine(new Character((char) 0));
          sendPost(dialogEndpoint, URLEncoder.encode(dialogId + "," + pass, "UTF-8"));
        } catch (IOException e) {
          Util.LogExceptions(e);
        }
        break;

      case "AlterPasswordQb":
        try {
          String pass = Util.Prompt4Password(true, true, null);
          sendPost(dialogEndpoint, URLEncoder.encode(dialogId + "," + pass, "UTF-8"));
        } catch (UnsupportedEncodingException e1) {
          Util.LogExceptions(e1);
        }
        break;

      case "CreateUserQb":
        try {
          String name = Util.Prompt4UserName();
          String pass = Util.Prompt4Password(true, true, null);
          sendPost(dialogEndpoint, URLEncoder.encode(dialogId + "," + name + ":" + pass, "UTF-8"));
        } catch (UnsupportedEncodingException e1) {
          Util.LogExceptions(e1);
        }
        break;

      case "CreateTokenQb":
        try {
          String secret = Util.ColorPrompt("Secret : ");
          sendPost(dialogEndpoint, URLEncoder.encode(dialogId + "," + secret, "UTF-8"));
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
          sendPost(dialogEndpoint, URLEncoder.encode(dialogId + "," + command, "UTF-8"));
        } catch (IOException e) {
          Util.LogExceptions(e);
        }
        break;

      case "DropDataSourceQb":
        try {
          ConsoleReader console = new ConsoleReader();
          console.setPrompt("Are you sure to drop data source ("
                            + inputs[3] + ")? [y/N]:");
          String command = console.readLine().trim().toLowerCase();
          if (!command.equals("y")) {
            command = "n";
          }
          sendPost(dialogEndpoint, URLEncoder.encode(dialogId + "," + command, "UTF-8"));
        } catch (IOException e) {
          Util.LogExceptions(e);
        }
        break;

      default:
        System.out.println("Please add the post action of " + qb
                           + " in com/tigergraph/client/Client.java");
        System.exit(0);

    }
  }

  /**
   *  Login to targetURL, if success, return null,
   *  else return error message.
   * @param targetURL the url to be connected
   * @return null if success, error message if fail
   */
  public JSONObject executeAuth(String targetURL) {
    URL url;
    HttpURLConnection connection = null;

    String cookie = generateCookies();

    try {
      //Create connection
      url = new URL(targetURL);
      connection = isHttps ? (HttpsURLConnection)url.openConnection()
          : (HttpURLConnection)url.openConnection();
      initConnection(connection);

      String userPass = username + ":" + password;
      String basicAuth = DatatypeConverter.printBase64Binary(
          userPass.getBytes(StandardCharsets.UTF_8));

      connection.setRequestProperty("Content-Length", ""
          + Integer.toString(basicAuth.getBytes().length));
      connection.setRequestProperty("Cookie", "" + cookie);

      //Send request
      PrintStream ps = new PrintStream(connection.getOutputStream());
      ps.println(basicAuth);
      ps.close();

      //Get Response

      InputStream is = connection.getInputStream();
      Scanner sc = new Scanner(is);
      String result = "";
      while (sc.hasNextLine()) {
        result += sc.nextLine();
      }
      sc.close();
      is.close();
      JSONObject json = new JSONObject(result);

      List<String> cookies = connection.getHeaderFields().get("Set-cookie");
      if (cookies != null && cookies.size() > 0) {
        userSession = cookies.get(0);
        Util.SESSION = userSession;
      }
      if (connection != null) {
        connection.disconnect();
      }
      return json;

    } catch (ConnectException e) {
      System.out.println(CONNECTION_ERR_MSG);
      System.exit(ReturnCode.CONNECTION_ERROR);
    } catch (Exception e) {
      System.out.println("Got error: " + e.getMessage());
      System.out.println("If SSL/TLS is enabled for TigerGraph, please use -cacert with a "
          + "certificate file, check TigerGraph document for details.");
      Util.LogExceptions(e);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
    return null;
  }

  /**
   * send a message to targetURL, do not handle response
   * @param targetURL url to be connected
   * @param urlParameters the message to send
   */
  public void sendPost(String targetURL, String urlParameters) {
    URL url;
    HttpURLConnection connection = null;
    try {
      //Create connection
      url = new URL(targetURL);
      connection = isHttps ? (HttpsURLConnection)url.openConnection()
          : (HttpURLConnection)url.openConnection();
      initConnection(connection);

      String userPass = username + ":" + password;
      String basicAuth = "Basic "
        + DatatypeConverter.printBase64Binary(userPass.getBytes(StandardCharsets.UTF_8));
      connection.setRequestProperty("Authorization", basicAuth);

      connection.setRequestProperty("Content-Length",
          "" + Integer.toString(urlParameters.getBytes().length));
      connection.setRequestProperty("Cookie", generateCookies());
      
      //Send request
      PrintStream ps = new PrintStream(connection.getOutputStream());
      ps.print(urlParameters);
      ps.close();

      if (connection.getResponseCode() == 401) {
        Util.LogText("Authentication failed. " + connection.getResponseMessage());
        System.out.println("Authentication failed.");
        if (connection != null) {
          connection.disconnect();
        }
        return;
      }

    } catch (ConnectException e) {
      System.out.println(CONNECTION_ERR_MSG);
      System.exit(ReturnCode.CONNECTION_ERROR);
    } catch (Exception e) {
      System.out.println("Got error: " + e.getMessage());
      Util.LogExceptions(e);
    } finally {

      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private void initConnection(HttpURLConnection connection) throws Exception {
    if (isHttps) {
      ((HttpsURLConnection)connection).setSSLSocketFactory(
          Util.getSSLContext(theCAFilename).getSocketFactory());
    }
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type",
        "application/x-www-form-urlencoded");

    connection.setRequestProperty("Content-Language", "en-US");

    connection.setUseCaches(false);
    connection.setDoInput(true);
    connection.setDoOutput(true);
  }

  private String generateCookies() {
    JSONObject cookieJSON = new JSONObject();

    if (isLocal) {
      cookieJSON.put("CLIENT_PATH", System.getProperty("user.dir"));
    }
    cookieJSON.put("GSHELL_TEST", System.getenv("GSHELL_TEST"));
    cookieJSON.put("COMPILE_THREADS", System.getenv("GSQL_COMPILE_THREADS"));
    cookieJSON.put("TERMINAL_WIDTH", String.valueOf(Util.getTerminalWidth()));

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
}
