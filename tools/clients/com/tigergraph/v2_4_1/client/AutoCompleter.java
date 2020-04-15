package com.tigergraph.v2_4_1.client;

import java.util.ArrayList;
import java.util.List;
import jline.console.completer.Completer;

/** 
 * TigerGraph shell's auto complete inteface implementation 
 */
class AutoCompleter implements Completer {

  private String[] keyWords; //for auto-complete

  public AutoCompleter(String[] keyWords) {
    this.keyWords = keyWords;
  }

  public int complete(String buffer, int cursor, List<CharSequence> candidates) {
    int identifierStart = findIdentifierStart(buffer, cursor);
    String identifierPrefix =
      identifierStart != -1 ? buffer.substring(identifierStart, cursor) : "";

    if (identifierStart != -1) {
      List<CharSequence> myCandidates = findMatchingVariables(identifierPrefix);
      if (myCandidates.size() > 0) {
        candidates.addAll(myCandidates);
        return identifierStart;
      }
    }
    return -1;
  }

  /**
   * Build a list of variables defined in the shell that match a given prefix.
   *
   * @param prefix the prefix to match
   * @return the list of variables that match the prefix
   */
  List<CharSequence> findMatchingVariables(String prefix) {
    List<CharSequence> ret = new ArrayList<CharSequence>();

    for (String candidate : keyWords) {
      String keyWord = candidate.trim();
      if (keyWord.startsWith(prefix)) ret.add((CharSequence) keyWord);
    }

    return ret;
  }

  /**
   * Parse a buffer to determine the start index of the groovy identifier
   *
   * @param buffer the buffer to parse
   * @param endingAt the end index with the buffer
   * @return the start index of the identifier, or -1 if the buffer does not contain a valid
   *     identifier that ends at endingAt
   */
  int findIdentifierStart(String buffer, int endingAt) {
    //if the string is empty then there is no expression
    if (endingAt == 0) return -1;
    // if the last character is not valid then there is no expression
    char lastChar = buffer.charAt(endingAt - 1);
    if (!Character.isJavaIdentifierPart(lastChar)) return -1;
    // scan backwards until the beginning of the expression is found
    int startIndex = endingAt - 1;
    while (startIndex > 0 && Character.isJavaIdentifierPart(buffer.charAt(startIndex - 1)))
      --startIndex;
    return startIndex;
  }
}
