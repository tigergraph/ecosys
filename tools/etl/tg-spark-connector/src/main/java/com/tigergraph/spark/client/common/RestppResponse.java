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
package com.tigergraph.spark.client.common;

import com.fasterxml.jackson.databind.JsonNode;

/** Standard TG RESTPP response POJO */
public class RestppResponse {
  public String code;
  public boolean error;
  public String message;
  public JsonNode results;

  /** Throw exception when HTTP status code is 200 but RESTPP error=true */
  public void panicOnFail() {
    if (error) {
      throw new RestppErrorException(code, message);
    }
  }
}
