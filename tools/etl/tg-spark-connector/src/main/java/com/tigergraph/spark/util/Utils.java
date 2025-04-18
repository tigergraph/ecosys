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
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Utilities */
public class Utils {
  public static final String DEFAULT_VERSION = "999.999.999";
  public static final Pattern VERSION_PARTTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+)");
  public static final int MASK_LEN_MAX = 8;

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
}
