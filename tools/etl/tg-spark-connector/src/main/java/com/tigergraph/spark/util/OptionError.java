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

import java.util.ArrayList;
import java.util.List;

/*
 * the result of option validation. there are multiple messages when this option has multiple error.
 */
public class OptionError {
  private String key;
  private Object originalValue;
  private List<String> errorMsgs;

  public OptionError(String key, Object originalValue, String errorMsg) {
    this.key = key;
    this.originalValue = originalValue;
    this.errorMsgs = new ArrayList<>();
    this.errorMsgs.add(errorMsg);
  }

  public String getKey() {
    return key;
  }

  public Object getOriginalValue() {
    return originalValue;
  }

  public List<String> getErrorMsgs() {
    return errorMsgs;
  }

  public String toString() {
    return String.format(
        "Option %s, value: %s, error: %s", key, originalValue, String.join("; ", errorMsgs));
  }
}
