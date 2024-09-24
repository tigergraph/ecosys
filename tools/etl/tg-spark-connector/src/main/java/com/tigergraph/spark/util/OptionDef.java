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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class OptionDef implements Serializable {

  // Identify whether the option has default value
  public enum DefaultVal {
    NON_DEFAULT
  };

  // Options' definitions
  private final Map<String, OptionKey> optionKeys = new HashMap<>();

  public Map<String, OptionKey> optionKeys() {
    return optionKeys;
  }

  /**
   * Define a new option
   *
   * @param name the name of the option
   * @param type the type of the option
   * @param defaultValue the default value to use if this option isn't present
   * @param required Whether this option must have
   * @param validator To use in checking the correctness of the option
   * @param group the group this option belongs to
   * @return This OptionDef so you can chain calls
   */
  public OptionDef define(
      String name,
      Type type,
      Serializable defaultValue,
      boolean required,
      Validator validator,
      String group) {
    OptionKey key = new OptionKey(name, type, defaultValue, required, validator, group);
    optionKeys.put(name, key);
    return this;
  }

  public OptionDef define(String name, Type type, String group) {
    return define(name, type, false, group);
  }

  public OptionDef define(String name, Type type, boolean required, String group) {
    return define(name, type, DefaultVal.NON_DEFAULT, required, null, group);
  }

  /*
   * the definition for option
   */
  public static class OptionKey implements Serializable {
    public final String name;
    public final Type type;
    public final Serializable defaultValue;
    public final boolean required;
    public final Validator validator;
    public final String group;

    /**
     * @param name the name of the option
     * @param type the type of the option
     * @param defaultValue the default value to use if this option isn't present
     * @param required Whether this option must have
     * @param validator To use in checking the correctness of the option
     * @param group the group this option belongs to
     */
    public OptionKey(
        String name,
        Type type,
        Serializable defaultValue,
        boolean required,
        Validator validator,
        String group) {
      this.name = name;
      this.type = type;
      this.defaultValue =
          DefaultVal.NON_DEFAULT.equals(defaultValue) ? DefaultVal.NON_DEFAULT : defaultValue;
      this.required = required;
      this.validator = validator;
      this.group = group;
    }

    public boolean hasDefault() {
      return !DefaultVal.NON_DEFAULT.equals(this.defaultValue);
    }
  }

  public enum Type {
    BOOLEAN,
    STRING,
    INT,
    SHORT,
    LONG,
    DOUBLE;
  }

  public interface Validator extends Serializable {
    void ensureValid(String name, Serializable value);
  }

  public static class ValidVersion implements Validator {

    public static ValidVersion INSTANCE = new ValidVersion();
    private static final String VERSION_PATTERN = "^(\\d+)\\.(\\d+)\\.(\\d+)$";

    @Override
    public void ensureValid(String name, Serializable value) {
      if (!Pattern.matches(VERSION_PATTERN, String.valueOf(value))) {
        throw new IllegalArgumentException(
            "Option("
                + name
                + ") must follow the pattern: MAJOR.MINOR.PATCH, got "
                + String.valueOf(value));
      }
    }
  }

  /*
   * validate for String type Option
   */
  public static class ValidString implements Validator {
    final List<String> validStrings;

    private ValidString(List<String> validStrings) {
      this.validStrings = validStrings;
    }

    public static ValidString in(String... validStrings) {
      return new ValidString(Arrays.asList(validStrings));
    }

    @Override
    public void ensureValid(String name, Serializable value) {
      if (validStrings.size() > 0 && !validStrings.contains(value)) {
        if (validStrings.size() == 1) {
          throw new IllegalArgumentException(
              "Option(" + name + ") must be: " + validStrings.get(0));
        } else {
          throw new IllegalArgumentException(
              "Option(" + name + ") must be one of: " + String.join(", ", validStrings));
        }
      }
    }
  }
}
