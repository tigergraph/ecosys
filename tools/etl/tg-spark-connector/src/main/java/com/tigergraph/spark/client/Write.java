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
package com.tigergraph.spark.client;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.tigergraph.spark.client.common.RestppResponse;

import feign.Body;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;

/** Write endpoint declaration used for data loading. */
public interface Write {
  @RequestLine("POST /restpp/ddl/{graph}")
  @Headers({"Content-Type: text/plain"})
  @Body("{data}")
  LoadingResponse ddl(
      @Param("graph") String graph,
      @Param("data") String data,
      @QueryMap Map<String, Object> queryMap);

  public class LoadingResponse extends RestppResponse {
    public boolean hasInvalidRecord() {
      return hasInvalidRecord(results);
    }

    // For the numeric element whose key is not validLine or validObject but value is non-zero,
    // it should represent the invalid data count, e.g., notEnoughToken: 123
    protected static boolean hasInvalidRecord(JsonNode in) {
      return in.toString().matches(".*\"(?!validLine|validObject)[^\"]+\": *[1-9]\\d*.*");
    }
  }
}
