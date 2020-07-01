package com.tigergraph.v3_0_0.client.util;

import java.io.*;

/**
 * This is copied from com.tigergraph.utility.IOUtil, since we don't have a shared
 * package with gsql-server, we will merge with it in the future.
 */
public final class IOUtils {

  public interface StringOperator {
    void apply(String op);
  }

  private static IOUtils util_;

  public static synchronized void init(IOUtils io) {
    if (util_ == null || io != null) {
      util_ = io != null ? io : new IOUtils();
    }
  }

  public static IOUtils get() {
    init(null);
    return util_;
  }

  public String readFromFile(String filename) {
    return readFromFile(new File(filename));
  }

  /**
   * Read from a file {@code filename} and return content as a String.
   * @param filename
   * @throws IOException
   */
  public String readFromFile(File filename) {
    try {
      StringBuilder result = new StringBuilder();
      readFromFileImpl(filename, line -> result.append(line + "\n"));
      return result.toString();
    } catch (IOException e) {
      SystemUtils.logger.error(e);
      return null;
    }
  }

  public void readFromFile(String filename, StringOperator op) throws IOException {
    readFromFileImpl(new File(filename), op);
  }

  public void readFromFile(File filename, StringOperator op) throws IOException {
    readFromFileImpl(filename, op);
  }

  /**
   * Read from a file {@code filename}, for each line apply operator {@code op}.
   * @param filename
   * @param op, the operator applied to each line.
   * @throws IOException
   */
  private void readFromFileImpl(File filename, StringOperator op) throws IOException {
    String line;
    BufferedReader br = new BufferedReader(new FileReader(filename));

    while ((line = br.readLine()) != null) {
      op.apply(line);
    }
    br.close();
  }

  public boolean writeToFile(String filename, String content) {
    return writeToFileImpl(new File(filename), content, false);
  }

  public boolean writeToFile(String filename, String content, boolean append) {
    return writeToFileImpl(new File(filename), content, append);
  }

  public boolean writeToFile(File filename, String content) {
    return writeToFileImpl(filename, content, false);
  }

  public boolean writeToFile(File filename, String content, boolean append) {
    return writeToFileImpl(filename, content, append);
  }

  private boolean writeToFileImpl(File file, String content, boolean append) {
    //create path folders if necessary
    file.getParentFile().mkdirs();

    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(file, append));
      PrintWriter pw = new PrintWriter(bw, false);

      pw.print(content);
      pw.flush();
      pw.close();
      bw.close();
    } catch (IOException e) {
      SystemUtils.logger.error(e);
      return false;
    }
    return true;
  }
}
