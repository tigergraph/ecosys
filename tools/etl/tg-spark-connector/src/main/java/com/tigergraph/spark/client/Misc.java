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

import com.fasterxml.jackson.databind.JsonNode;
import com.tigergraph.spark.client.common.RestppResponse;
import com.tigergraph.spark.util.Utils;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * Restpp API delaration used for connectivity check, token request and cluster basic info
 * detection.
 */
public interface Misc {
  @RequestLine("GET /restpp/version")
  RestppResponse version();

  /** Prior to TG v4.1.0 */
  @RequestLine("GET /gsqlserver/gsql/loading-jobs?action=getprogress&graph={graph}&jobId={jobId}")
  RestppResponse loadingProgressV0(@Param("graph") String graph, @Param("jobId") String jobId);

  /** Since TG v4.1.0 */
  @RequestLine("GET /gsql/v1/loading-jobs/status?graph={graph}&jobIds={jobId}")
  RestppResponse loadingProgressV1(@Param("graph") String graph, @Param("jobId") String jobId);

  default RestppResponse loadingProgress(String version, String graph, String jobId) {
    if (Utils.versionCmp(version, "4.1.0") >= 0) {
      return loadingProgressV1(graph, jobId);
    } else {
      return loadingProgressV0(graph, jobId);
    }
  }

  /** Prior to TG v4.1.0 */
  @RequestLine("GET /gsqlserver/gsql/schema?graph={graph}&type={type}")
  RestppResponse graphSchemaV0(@Param("graph") String graph, @Param("type") String type);

  /** Since TG v4.1.0 */
  @RequestLine("GET /gsql/v1/schema/graphs/{graph}?type={type}")
  RestppResponse graphSchemaV1(@Param("graph") String graph, @Param("type") String type);

  default RestppResponse graphSchema(String version, String graph, String type) {
    if (Utils.versionCmp(version, "4.1.0") >= 0) {
      return graphSchemaV1(graph, type);
    } else {
      return graphSchemaV0(graph, type);
    }
  }

  /** Prior to TG v4.1.0 */
  @RequestLine("GET /gsqlserver/gsql/queryinfo?graph={graph}&query={query}")
  QueryMetaResponse queryMetaV0(@Param("graph") String graph, @Param("query") String query);

  /** Since TG v4.1.0 */
  @RequestLine("POST /gsql/v1/queries/signature?graph={graph}&queryName={query}")
  @Headers({"Content-Type: text/plain"})
  QueryMetaResponse queryMetaV1(@Param("graph") String graph, @Param("query") String query);

  default QueryMetaResponse queryMeta(String version, String graph, String query) {
    if (Utils.versionCmp(version, "4.1.0") >= 0) {
      return queryMetaV1(graph, query);
    } else {
      return queryMetaV0(graph, query);
    }
  }

  /**
   * POST /gsql/v1/queries/signature <br>
   * The output contains the schema of each row
   */
  public class QueryMetaResponse extends RestppResponse {
    public JsonNode output;
  }
}
