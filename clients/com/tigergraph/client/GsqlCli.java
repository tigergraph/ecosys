package com.tigergraph.client;

import org.apache.commons.cli.*;
import java.io.File;

/**
 * Class to parse gsql command line arguments.
 */
public class GsqlCli {
  // CLI options
  private Options options;
  private CommandLine cli;

  public GsqlCli() {
    options = new Options();
    // add CLI options
    // -v shows simple version
    options.addOption(Option.builder("v").hasArg(false).build());
    // -version/--version shows complete version
    options.addOption(Option.builder("version").longOpt("version").hasArg(false).build());
    options.addOption(Option.builder("h").longOpt("help").hasArg(false).build());
    options.addOption(Option.builder("reset").longOpt("reset").hasArg(false).build());
    options.addOption(
        Option.builder("u").longOpt("user").hasArg(true).build());
    options.addOption(
        Option.builder("p").longOpt("password").hasArg(true).build());
    options.addOption(
        Option.builder("f").longOpt("file").hasArg(true).build());
    // support login to a specified graph
    options.addOption(
        Option.builder("g").longOpt("graph").hasArg(true).build());
    // support unlimited arguments for -c option
    options.addOption(
        Option.builder("c").longOpt("command").hasArgs().hasArg(true).numberOfArgs(Option.UNLIMITED_VALUES).build());
    options.addOption(Option.builder("i").longOpt("ip").hasArg(true).build());
    options.addOption(Option.builder("cacert").longOpt("cacert").hasArg(true).build());
    options.addOption(Option.builder("graphstudio").longOpt("graphstudio").hasArg(false).build());
    options.addOption(Option.builder("logdir").longOpt("logdir").hasArg(true).build());
  }

  public boolean parse(String[] args) {
    CommandLineParser parser = new DefaultParser();
    try {
      // The third arg tells the parser to stop (don't report error) when seen unknown option
      // Doing this, we will not confusing GSQL CLI options with GSQL command options such as -HARD
      cli = parser.parse(options, args, true);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      return false;
    }
    return true;
  }

  public boolean hasVersion() {
    return cli.hasOption("v");
  }

  public boolean hasDetailedVersion() {
    return cli.hasOption("version");
  }

  public boolean hasHelp() {
    return cli.hasOption("h");
  }

  public boolean hasReset() {
    return cli.hasOption("reset");
  }

  public boolean hasFile() {
    return cli.hasOption("f");
  }

  public String getFile() {
    return cli.getOptionValue('f');
  }

  public boolean hasCommand() {
    return cli.hasOption("c");
  }

  public String getCommand() {
    String[] values = cli.getOptionValues('c');
    return combineStr(values);
  }

  public boolean hasUser() {
    return cli.hasOption("u");
  }

  public String getUser() {
    return cli.getOptionValue('u');
  }

  public boolean hasPassword() {
    return cli.hasOption("p");
  }

  public String getPassword() {
    return cli.getOptionValue('p');
  }

  public String getSecret() {
    return cli.getOptionValue('s');
  }

  public boolean hasIp() {
    return cli.hasOption("i");
  }

  public String getIp() {
    return cli.getOptionValue('i');
  }

  public boolean hasGraph() {
    return cli.hasOption("g");
  }

  public String getGraph() {
    return cli.getOptionValue('g');
  }

  public boolean hasCacert() {
    return cli.hasOption("cacert");
  }

  public String getCacert() {
    return cli.getOptionValue("cacert");
  }

  public boolean isFromGraphStudio() { return cli.hasOption("graphstudio"); }

  public boolean hasLogdir() {
    return cli.hasOption("logdir");
  }

  public String getLogdir() {
    return cli.getOptionValue("logdir");
  }

  /**
   * return the command argument for gsql, e.g. in "gsql -u chengjie show user",
   * "show user" is the command argument.
   * @return concatenated command argument separated by single space.
   */
  public String getArgument() {
    String[] args = cli.getArgs();
    return combineStr(args);
  }

  /**
   * test whether a file name is provided as gsql command argument
   * @return
   */
  public boolean isReadingFile() {
    String[] args = cli.getArgs();
    if (args == null || args.length == 0) {
      return false;
    }
    return args.length == 1 && (new File(args[0])).isFile();
  }

  /**
   * Helper function to combine string array into a single string separated
   * by space.
   */
  private String combineStr(String[] strs) {
    String str = "";
    if (strs != null && strs.length != 0) {
      for (int i = 0; i < strs.length; i++) {
        if (i != 0)
          str += " ";
        str += strs[i];
      }
    }
    return str;
  }
}
