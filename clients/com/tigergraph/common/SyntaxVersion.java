package com.tigergraph.common;
import java.util.List;
import java.util.ArrayList;

/**
 * Represent the syntax version 
 */
public enum SyntaxVersion {
  //v1 is syntax <=2.3, -(E)- == - (E) ->
  //v2 is syntax >=2.4, -(E)- is pattern match. 
  V1("v1"), 
  V2("v2"),           //default version, pattern sytnax, no log
  _V2("_v2"),           //internal use. show transformed query
  UNKNOWN("unknown");

  private String version;
  //in  3.0, we should change below to V2. 
  static public SyntaxVersion defaultVersion = V1;

  private SyntaxVersion(String version) {
    this.version = version;
  }

  public String getVersion() {
    return version;
  }
  
  //Given a version string, return its corresponding enum obj
  public static SyntaxVersion getInstance(String version) {
    for (SyntaxVersion obj: SyntaxVersion.values()) {
      if (obj.version.compareTo(version) == 0) {
        return obj;
      }
    }

    return SyntaxVersion.UNKNOWN;
  }

  //set the default version
  public static boolean setDefaultSyntaxVersion(String version) {
    defaultVersion = getInstance(version);
    return defaultVersion != UNKNOWN;
  }

  //Use the lastest version as default version
  public static SyntaxVersion defaultSyntax() {
    return defaultVersion;
  }

  public static String defaultVersion() {
    return defaultSyntax().getVersion();
  }

  // check whether a version string is valid or not
  public static boolean isValidVersion(String version) {
    return getInstance(version) != SyntaxVersion.UNKNOWN;
  }

  public static List<String> supportVersions() {
    List<String> vNames = new ArrayList<>();

    for (SyntaxVersion obj: SyntaxVersion.values()) {
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
  public String toString(){
    return version;
  }
}
