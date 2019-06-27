package com.tigergraph.client;

import com.tigergraph.common.Constant;
import com.tigergraph.common.GSQL_LOG;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Properties;

import jline.console.ConsoleReader;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * This class define several static utility methods.
 *
 * <p>This class is a final class and cannot be extended.
 */
public final class Util {
  public static String LOG_DIR;
  public static String SESSION;

  /** Grep IUM config from gsql.cfg file. */
  public static String getIUMConfig(String config) {
    String cmd = "grep " + config + " ~/.gsql/gsql.cfg | cut -d ' ' -f 2";
    return getStringFromBashCmd(cmd);
  }

  /**
   * load the CA and use it in the https connection
   * @param filename the CA filename
   * @return the SSL context
   */
  public static SSLContext getSSLContext(String filename) throws Exception {
    try {
      // Load CAs from an InputStream
      // (could be from a resource or ByteArrayInputStream or ...)
      // X.509 is a standard that defines the format of public key certificates, used in TLS/SSL.
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      InputStream caInput = new BufferedInputStream(new FileInputStream(filename));
      Certificate ca = cf.generateCertificate(caInput);

      // Create a KeyStore containing our trusted CAs
      String keyStoreType = KeyStore.getDefaultType();
      KeyStore keyStore = KeyStore.getInstance(keyStoreType);
      keyStore.load(null, null);
      keyStore.setCertificateEntry("ca", ca);

      // Create a TrustManager that trusts the CAs in our KeyStore
      String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
      tmf.init(keyStore);

      // Create an SSLContext that uses our TrustManager
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, tmf.getTrustManagers(), null);
      return context;
    } catch (Exception e) {
      throw new Exception("Failed to load the CA file: " + e.getMessage(), e);
    }
  }

  public static void LogText(String text) {
    if (GSQL_LOG.LOG_FILE == null) return;
    try {
      GSQL_LOG.LogInfo(text, SESSION, 1);
    } catch (Exception e) {
    }
  }

  public static void LogExceptions(Throwable e) {
    if (GSQL_LOG.LOG_FILE == null) return;
    try {
      GSQL_LOG.LogExceptions(e, SESSION, 1);
    } catch (Exception ioe) {
    }
  }

  public static void setClientLogFile(String username, String serverip, boolean printerror) {
    try {
      GSQL_LOG.LOG_FILE = Util.getClientLogFile("log", username, serverip, true).getPath();
      PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(GSQL_LOG.LOG_FILE, true)));
      writer.print("");
    } catch (Exception e) {
      GSQL_LOG.LOG_FILE = null;
      if (printerror) System.out.println("Can't open log file, " + e.getMessage());
    }
    Util.LogText("Client commit: " + Util.getCommitClient());
  }

  public static File getClientLogFile(String file, String username, String serverip,
      boolean rotate) throws IOException {
    if (Util.LOG_DIR == null) {
      throw new IOException("LogDir is null");
    }

    String filename = Util.LOG_DIR + "/" + file + ".";
    filename += Math.abs((username + "." + serverip).hashCode()) % 1000000;
    File f = new File(filename);
    if (!f.getParentFile().isDirectory()) {
      f.getParentFile().mkdirs();
    }

    if (!f.exists()) {
      //create file if it does not exist
      f.createNewFile();
    } else if (rotate && f.length() > Constant.LogRotateSize) {
      // rotate log file when file is large than 500M
      long current = System.currentTimeMillis();
      // copy the log to a new file with timestamp
      String LogCopy = filename + "." + current / 1000;
      Files.move(f.toPath(), Paths.get(LogCopy), StandardCopyOption.REPLACE_EXISTING);
    }

    return f;
  }

  /**
   * Create a prompt with color and customized prompt text.
   * @param text, the prompt text
   * @return input string
   */
  public static String ColorPrompt(String text) {
    String ANSI_BLUE = "\u001B[1;34m";
    String ANSI_RESET = "\u001B[0m";
    String input = null;
    try {
      ConsoleReader tempConsole = new ConsoleReader();
      String prmpt = ANSI_BLUE + text + ANSI_RESET;
      tempConsole.setPrompt(prmpt);
      input = tempConsole.readLine();
    } catch (IOException e) {
      LogExceptions(e);
      System.out.println("Prompt input error!");
    }
    return input;
  }

  /**
   * function to prompt to generate a prompt for user to input username
   * @return input user name
   */
  public static String Prompt4UserName() {
    return ColorPrompt("User Name : ");
  }

  /**
   * function to generate a prompt for user to input username
   * @param doubleCheck, notes whether the password should be confirmed one more time.
   * @param isNew, indicates whether it is inputting a new password
   * @return SHA-1 hashed password on success, null on error
   */
  public static String Prompt4Password(boolean doubleCheck, boolean isNew, String username) {
    String ANSI_BLUE = "\u001B[1;34m";
    String ANSI_RESET = "\u001B[0m";
    String pass = null;
    try {
      ConsoleReader tempConsole = new ConsoleReader();
      String prompttext = isNew ? "New Password : " : "Password for " + username + " : ";
      String prompt = ANSI_BLUE + prompttext + ANSI_RESET;
      tempConsole.setPrompt(prompt);
      tempConsole.setExpandEvents(false);
      String pass1 = tempConsole.readLine(new Character('*'));
      if (doubleCheck) {
        String pass2 = pass1;
        prompt = ANSI_BLUE + "Re-enter Password : " + ANSI_RESET;
        tempConsole.setPrompt(prompt);
        pass2 = tempConsole.readLine(new Character('*'));
        if (!pass1.equals(pass2)) {
          System.out.println("The two passwords do not match.");
          return null;
        }
      }
      // need to hash the password so that we do not store it as plain text
      pass = pass1;
    } catch (Exception e) {
      LogExceptions(e);
      System.out.println("Error while inputting password.");
    }
    return pass;
  }

  public static int getTerminalWidth() {
    int width = 80;
    // for windows system, just use a default value
    if (isWindows()) {
      return width;
    }

    // NOTE: The "tput" commands computes terminal length only when the stderr is redirected to
    // current tty ---- /dev/tty. This will not work for Windows.
    String result = Util.getStringFromBashCmd("tput cols 2> /dev/tty");
    if (result != null && !result.isEmpty()) {
      try {
        width = Integer.parseInt(result);
      } catch (NumberFormatException e) {
        width = 80;
      }
    }
    return width;
  }

  public static boolean isWindows() {
    String OS = System.getProperty("os.name").toLowerCase();
    return (OS.indexOf("win") >= 0);
  }

  public static String getStringFromBashCmd(String cmd) {
    try {

      ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
      Process p = pb.start();
      BufferedReader output = new BufferedReader(new InputStreamReader(p.getInputStream()));
      p.waitFor();

      //get output (only one line)
      String result = output.readLine();

      return result;
    } catch (Exception e) {
      LogExceptions(e);
      return null;
    }
  }

  public static String getCommitClient() {
    String clientCommitHash = null;
    try {
      Properties props = new Properties();
      InputStream in = Util.class.getClassLoader().getResourceAsStream("Version.prop");
      props.load(in);
      in.close();
      clientCommitHash = props.getProperty("commit_client");
    } catch (Exception e) {
      LogExceptions(e);
      System.out.println("Can't find Version.prop.");
    }

    return clientCommitHash;
  }

}

