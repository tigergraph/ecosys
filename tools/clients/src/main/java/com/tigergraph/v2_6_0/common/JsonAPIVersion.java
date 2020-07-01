/**
 * ***************************************************************************
 * Copyright (c)  2014-2017, TigerGraph Inc. 
 * All rights reserved 
 * Unauthorized copying of this file, via any medium is
 * strictly prohibited 
 * Proprietary and confidential
 * ****************************************************************************
 */
package com.tigergraph.v2_6_0.common;
import java.util.List;
import java.util.ArrayList;

/**
 * Represent the api version of response that is returned to end-user
 * It is extracted from Query text in 
 * AliasNormalizer.java: exitApiClause(), and set into commonVariable.JsonAPI.
 */
public enum JsonAPIVersion {
  //add the version here if json format is changed each time
  //For example: V3, V4 ...
  V1("v1"), 
  V2("v2"),           //default version
  UNKNOWN("unknown");

  private String version;
  static public JsonAPIVersion defaultVersion = V2;

  private JsonAPIVersion(String version) {
    this.version = version;
  }

  public String getVersion() {
    return version;
  }
  
  //Given a version string, return its corresponding enum obj
  public static JsonAPIVersion getInstance(String version) {
    for (JsonAPIVersion obj: JsonAPIVersion.values()) {
      if (obj.version.equalsIgnoreCase(version)) {
        return obj;
      }
    }

    return JsonAPIVersion.UNKNOWN;
  }

  //set the default version
  public static boolean setDefaultAPI(String version) {
    defaultVersion = getInstance(version);
    return defaultVersion != UNKNOWN;
  }

  //Use the lastest version as default version
  public static JsonAPIVersion defaultAPI() {
    return defaultVersion;
  }

  public static String defaultVersion() {
    return defaultAPI().getVersion();
  }

  // check whether a version string is valid or not
  public static boolean isValidVersion(String version) {
    return getInstance(version) != JsonAPIVersion.UNKNOWN;
  }

  public static List<String> supportVersions() {
    List<String> vNames = new ArrayList<>();

    for (JsonAPIVersion obj: JsonAPIVersion.values()) {
      if (isValidVersion(obj.version)) {
        vNames.add(obj.version);
      }
    }
    return vNames;
  }

  public static String allSupportVersions() {
    List<String> vNames = supportVersions();

    // will return [v1,v2]
    String res = "[" + String.join(",", vNames) + "]";
    return res;
  }
}
