package com.tigergraph.v3_1_1.client.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import jline.console.ConsoleReader;

/**
 * Utility methods reltaed to console.
 */
public class ConsoleUtils {

  /**
   * Generate a prompt for user to input username.
   *
   * @return input user name
   */
  public static String prompt4UserName() {
    return colorPrompt("User Name : ");
  }

  /**
   * Create a prompt with color and customized prompt text.
   *
   * @param text, the prompt text
   * @return input string
   */
  public static String colorPrompt(String text) {
    String ANSI_BLUE = "\u001B[1;34m";
    String ANSI_RESET = "\u001B[0m";
    String input = null;
    try {
      ConsoleReader tempConsole = new ConsoleReader();
      String prmpt = ANSI_BLUE + text + ANSI_RESET;
      tempConsole.setPrompt(prmpt);
      input = tempConsole.readLine();
    } catch (IOException e) {
      SystemUtils.logger.error(e);
      System.out.println("Prompt input error!");
    }
    return input;
  }

  /**
   * Generate a prompt for user to input username
   *
   * @param doubleCheck notes whether the password should be confirmed one more time.
   * @param isNew indicates whether it is inputting a new password
   * @param username user name
   * @return SHA-1 hashed password on success, null on error
   */
  public static String prompt4Password(boolean doubleCheck, boolean isNew, String username) {
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
      SystemUtils.logger.error(e);
      System.out.println("Error while inputting password.");
    }
    return pass;
  }

  /**
   * Get console width based on OS.
   *
   * @return Width of console
   */
  public static int getConsoleWidth() {
    int width = 80;
    // for windows system, just use a default value
    if (isWindows()) {
      return width;
    }

    // NOTE: The "tput" commands computes terminal length only when the stderr is redirected to
    // current tty ---- /dev/tty. This will not work for Windows.
    String result = getStringFromBashCmd("tput cols 2> /dev/tty");
    if (result != null && !result.isEmpty()) {
      try {
        width = Integer.parseInt(result);
      } catch (NumberFormatException e) {
        width = 80;
      }
    }
    return width;
  }

  /**
   * Get the first line of the output by running a bash command.
   *
   * @param cmd Command to run in bash
   * @return First line of the output
   */
  public static String getStringFromBashCmd(String cmd) {
    try {

      ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
      Process p = pb.start();
      BufferedReader output = new BufferedReader(new InputStreamReader(p.getInputStream()));
      p.waitFor();

      // get output (only one line)
      String result = output.readLine();

      return result;
    } catch (Exception e) {
      SystemUtils.logger.error(e);
      return null;
    }
  }

  /**
   * Check whether the OS is Windows.
   *
   * @return {@code true} if it's Windows; {@code false} otherwise
   */
  public static boolean isWindows() {
    String OS = System.getProperty("os.name").toLowerCase();
    return (OS.indexOf("win") >= 0);
  }
}
