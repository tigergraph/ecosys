/**
 * ***************************************************************************
 * Copyright (c)  2014-2019, TigerGraph Inc. 
 * All rights reserved 
 * Unauthorized copying of this file, via any medium is
 * strictly prohibited 
 * Proprietary and confidential
 * Author: Mingxi Wu
 * ****************************************************************************
 */

package com.tigergraph.v2_6_2.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Options for syntax version setting at global or query level.
 * <p>
 * Currently, the default sytnax version is v1.
 */
public enum SyntaxVersion {
  /**
   * syntax up until v2.3, where <code>-(E)-</code> is equivalent
   * to <code>-(E)-></code>
   */
  V1("v1"),
  /** syntax after v2.4, i.e. pattern match syntax */
  V2("v2"),
  /**
   * syntax after v2.4 that shows transformed query in GSQL log,
   * internal use only
   */
  _V2("_v2"),
  /** unknown syntax version used for error reporting */
  UNKNOWN("unknown");

  private String version;

  // Note: In  3.0, we should change below to V2.
  /** Default syntax version in 2.4 is V1 */
  static public SyntaxVersion defaultVersion = V1;

  private SyntaxVersion(String version) {
    this.version = version;
  }

  public String getVersion() {
    return version;
  }
  
  /**
   * Returns the <code>SyntaxVersion</code> object corresponding to the
   * given version string.
   * <p>
   * Accepted inputs are "v1", "v2", "V1", "V2", "_v2", "_V2" to allow
   * for case-insensitive setting.
   *
   * @param version the version string provided by user
   * @return corresponding <code>SyntaxVersion</code> object for valid input
   *         or <code>SyntaxVersion.UNKNOWN<code> for invalid input
   */
  public static SyntaxVersion getInstance(String version) {
    for (SyntaxVersion obj: SyntaxVersion.values()) {
      // check for each valid input and return the corresponding object if match
      if (obj.version.equalsIgnoreCase(version)) {
        return obj;
      }
    }
    // return unknown syntax version for invalid input
    return SyntaxVersion.UNKNOWN;
  }

  /**
   * Resets default sytnax version to the one corresponding to the provided
   * version string.
   *
   * @param version the version string provided by user
   * @return true if user input is valid and false otherwise
   */
  public static boolean setDefaultSyntaxVersion(String version) {
    defaultVersion = getInstance(version);
    return defaultVersion != UNKNOWN;
  }

  /** Returns the default syntax version in current session. */
  public static SyntaxVersion defaultSyntax() {
    return defaultVersion;
  }

  /** Returns the version string of the current default syntax version */
  public static String defaultVersion() {
    return defaultSyntax().getVersion();
  }

  /**
   * Checks whether a version string is valid or not.
   * @param version the user-provided version string to check
   * @return true if the user input is valid and false otherwise
   */
  public static boolean isValidVersion(String version) {
    return getInstance(version) != SyntaxVersion.UNKNOWN;
  }

  /** Returns a list of supported syntax version string */
  public static List<String> supportVersions() {
    List<String> vNames = new ArrayList<>();
    // filter out invalid string in SyntaxVersion.values() (e.g. "unknown")
    // also filter out version string for internal usage (e.g. "_v2")
    for (SyntaxVersion obj: SyntaxVersion.values()) {
      if (isValidVersion(obj.version) && obj != SyntaxVersion._V2) {
        vNames.add(obj.version);
      }
    }
    return vNames;
  }

  /** Returns a string representation of all supported syntax version string */
  public static String allSupportVersions() {
    List<String> vNames = supportVersions();
    // will return [v1,v2]
    String res = "[" + String.join(",", vNames) + "]";
    return res;
  }

  /** Returns the syntax version string */
  public String toString(){
    return version;
  }
}
