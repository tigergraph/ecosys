/**
 * Copyright (c) 2023 TigerGraph Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tigergraph.spark.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Utilities */
public class Utils {
  public static final String DEFAULT_VERSION = "999.999.999";
  public static final Pattern VERSION_PARTTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+)");
  public static final int MASK_LEN_MAX = 8;
  private static final char BACKTICK_QUOTE = '`';
  private static final char COLON = ':';
  private static final char COMMA = ',';

  /***************** VERSION *****************/

  /** Extract the TG version from the response msg of /version endpoint */
  public static String extractVersion(String input) {
    Matcher matcher = VERSION_PARTTERN.matcher(input);
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      return DEFAULT_VERSION;
    }
  }

  /**
   * Compare the two input versions
   *
   * @return positive v1 > v2; 0 if v1 == v2; negative if v1 < v2
   */
  public static int versionCmp(String v1, String v2) {
    return fmtVersion(v1).compareTo(fmtVersion(v2));
  }

  /** Format version string to fixed length: 3.10.1 => 003010001 */
  private static String fmtVersion(String version) {
    final List<String> ver = new ArrayList<>();
    final String[] verSplit = version.split("\\.");
    // major, minor, patch
    for (int i = 0; i < 3; i++) {
      if (i >= verSplit.length) {
        ver.add("0");
      } else {
        ver.add(verSplit[i]);
      }
    }
    return ver.stream()
        .map(v -> String.format("%03d", Integer.valueOf(v)))
        .collect(Collectors.joining());
  }

  /***************** STRING *****************/

  public static boolean isEmpty(String s) {
    return s == null || s.length() == 0;
  }

  /**
   * Mask the string input(sensitive info). You can keep several chars of the head and tail:
   * maskString("abcdef123456", 2) => "ab********56"
   *
   * @param s string to be mask
   * @param keepHeadTail how many chars of the head and tail of the string will be kept
   * @return masked string
   */
  public static String maskString(String s, int keepHeadTail) {
    if (s == null) {
      return "";
    }
    if (2 * keepHeadTail >= s.length()) {
      return s;
    }
    return s.substring(0, keepHeadTail)
        + String.join(
            "", Collections.nCopies(Math.min(MASK_LEN_MAX, s.length() - 2 * keepHeadTail), "*"))
        + s.substring(s.length() - keepHeadTail);
  }

  /** Avoid exposing user data from the loading stats to log. */
  public static void removeUserData(JsonNode in) {
    // prior to v3.9.0
    List<JsonNode> badDataV1 = in.findParents("invalidAttributeLinesData");
    badDataV1.forEach(json -> ((ObjectNode) json).remove("invalidAttributeLinesData"));
    // after v3.9.0
    List<JsonNode> badDataV2 = in.findParents("lineData");
    badDataV2.forEach(json -> ((ObjectNode) json).remove("lineData"));
  }

  /**
   * The query type of vertex/edge query is determined by the field count: query.vertex = Comment.1
   * It has two fields: Comment and 1, separated by dot
   */
  public static int countQueryFields(String query, String fieldSep) {
    return extractQueryFields(query, fieldSep).size();
  }

  /**
   * The query type of vertex/edge query is determined by the field count: query.vertex = Comment.1
   * It has two fields: Comment and 1, return ["Comment", "1"] TODO: should be able to quote field
   * which contain '.'
   */
  public static List<String> extractQueryFields(String query, String fieldSep) {
    StringTokenizer tknz = new StringTokenizer(query, fieldSep);
    List<String> tokenList = new ArrayList<>();
    while (tknz.hasMoreTokens()) {
      tokenList.add(tknz.nextToken());
    }
    return tokenList;
  }

  /** Defines the internal states of the parser. */
  private enum ParserState {
    PARSING_KEY,
    PARSING_VALUE
  }

  /**
   * Parses the custom key-value formatted string into a Map. If the key or value contains commas,
   * colons, or spaces, they should be enclosed in backticks (`); if a backtick is needed in the key
   * or value, it should be escaped by doubling it (e.g., ``).
   *
   * @param input The string to parse.
   * @return A {@link Map} containing the parsed key-value pairs. Returns an empty map if the input
   *     is null or empty.
   * @throws IllegalArgumentException if the input string is malformed (e.g., unclosed quotes,
   *     invalid structure).
   */
  public static Map<String, String> parseKeyValueString(String input) {
    if (input == null || input.trim().isEmpty()) {
      return new HashMap<>();
    }

    Map<String, String> resultMap = new HashMap<>();
    ParserState state = ParserState.PARSING_KEY;

    StringBuilder currentToken = new StringBuilder();
    String currentKey = null;

    char activeQuoteChar = 0; // 0 indicates not inside quotes

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);

      if (activeQuoteChar != 0) { // We are inside a quoted section
        if (c == activeQuoteChar) {
          // Check for an escaped backtick (``)
          if (i + 1 < input.length() && input.charAt(i + 1) == activeQuoteChar) {
            currentToken.append(activeQuoteChar);
            i++; // Skip the second backtick of the pair
          } else {
            activeQuoteChar = 0; // Closing quote found
          }
        } else {
          currentToken.append(c);
        }
      } else { // We are not inside a quoted section
        switch (c) {
          case BACKTICK_QUOTE:
            activeQuoteChar = c; // Opening quote found
            break;

          case COLON:
            if (state != ParserState.PARSING_KEY) {
              throw new IllegalArgumentException(
                  "Malformed input: Unexpected colon at index " + i + ". Input: \"" + input + "\"");
            }
            currentKey = currentToken.toString().trim();
            currentToken.setLength(0); // Reset for the value
            state = ParserState.PARSING_VALUE;
            break;

          case COMMA:
            if (state != ParserState.PARSING_VALUE || currentKey == null) {
              throw new IllegalArgumentException(
                  "Malformed input: Unexpected comma at index "
                      + i
                      + ". Missing key or value. Input: \""
                      + input
                      + "\"");
            }
            resultMap.put(currentKey, currentToken.toString().trim());
            currentToken.setLength(0);
            currentKey = null;
            state = ParserState.PARSING_KEY;
            break;

          default:
            // Skip leading whitespace before a new token starts
            if (Character.isWhitespace(c) && currentToken.length() == 0) {
              continue;
            }
            currentToken.append(c);
            break;
        }
      }
    }

    // After the loop, handle the final state of the parser
    if (activeQuoteChar != 0) {
      throw new IllegalArgumentException(
          "Malformed input: Unclosed quote at end of string. Input: \"" + input + "\"");
    }

    if (state == ParserState.PARSING_VALUE) {
      // Finished while parsing a value, which is a valid final state.
      resultMap.put(currentKey, currentToken.toString().trim());
    } else if (currentToken.length() > 0) {
      // Finished with a token but not in a value-parsing state means a dangling key.
      throw new IllegalArgumentException(
          "Malformed input: Dangling key without a value. Input: \"" + input + "\"");
    } else if (!resultMap.isEmpty() && input.trim().endsWith(",")) {
      // The string ends with a comma after at least one valid pair.
      throw new IllegalArgumentException(
          "Malformed input: Trailing comma at end of string. Input: \"" + input + "\"");
    }

    return resultMap;
  }

  /**
   * Parses a comma-delimited string into a List of field names for composite keys. Supports
   * backquoted (`) field names. To include a literal backtick, it must be escaped by doubling it
   * (e.g., `a``b`).
   *
   * @param input The comma-delimited string to parse (e.g., "id1,id2,`field name`").
   * @return A {@link List} containing the parsed field names. Returns an empty list if the input is
   *     null or empty.
   * @throws IllegalArgumentException if the input string is malformed (e.g., empty field names,
   *     trailing commas, unclosed quotes).
   */
  public static List<String> parseCommaDelimitedKeys(String input) {
    if (input == null || input.trim().isEmpty()) {
      return new ArrayList<>();
    }

    List<String> fieldNames = new ArrayList<>();
    StringBuilder currentToken = new StringBuilder();
    char activeQuoteChar = 0; // 0 indicates not inside quotes

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);

      if (activeQuoteChar != 0) { // We are inside a quoted section
        if (c == activeQuoteChar) {
          // Check for an escaped backtick (``)
          if (i + 1 < input.length() && input.charAt(i + 1) == activeQuoteChar) {
            currentToken.append(activeQuoteChar);
            i++; // Skip the second backtick of the pair
          } else {
            activeQuoteChar = 0; // Closing quote found
          }
        } else {
          currentToken.append(c);
        }
      } else { // We are not inside a quoted section
        switch (c) {
          case BACKTICK_QUOTE:
            activeQuoteChar = c; // Opening quote found
            break;

          case COMMA:
            String fieldName = currentToken.toString().trim();
            if (fieldName.isEmpty()) {
              throw new IllegalArgumentException(
                  "Malformed input: Empty field name at position "
                      + fieldNames.size()
                      + ". Input: \""
                      + input
                      + "\"");
            }
            fieldNames.add(fieldName);
            currentToken.setLength(0); // Reset for next field
            break;

          default:
            // Skip leading whitespace before a new token starts
            if (Character.isWhitespace(c) && currentToken.length() == 0) {
              continue;
            }
            currentToken.append(c);
            break;
        }
      }
    }

    // After the loop, handle the final state
    if (activeQuoteChar != 0) {
      throw new IllegalArgumentException(
          "Malformed input: Unclosed quote at end of string. Input: \"" + input + "\"");
    }

    // Add the final field name
    String finalFieldName = currentToken.toString().trim();
    if (finalFieldName.isEmpty()) {
      if (fieldNames.isEmpty()) {
        throw new IllegalArgumentException(
            "Malformed input: No valid field names found. Input: \"" + input + "\"");
      } else {
        throw new IllegalArgumentException(
            "Malformed input: Trailing comma found. Input: \"" + input + "\"");
      }
    }
    fieldNames.add(finalFieldName);

    return fieldNames;
  }
}
