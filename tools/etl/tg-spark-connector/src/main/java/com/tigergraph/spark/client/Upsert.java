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

import com.tigergraph.spark.client.common.RestppResponse;

import feign.Body;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;

/** Upsert endpoint declaration used for direct vertex/edge upsertion. */
public interface Upsert {
  @RequestLine("POST /restpp/graph/{graph}")
  @Headers({"Content-Type: application/json"})
  @Body("{data}")
  UpsertResponse upsert(
      @Param("graph") String graph,
      @Param("data") String data,
      @QueryMap Map<String, Object> queryMap);

  public class UpsertResponse extends RestppResponse {
    /**
     * Get the entire results as string
     *
     * @return the results as string
     */
    public String getResultsAsString() {
      if (results != null) {
        return results.toString();
      }
      return "";
    }
  }
}
