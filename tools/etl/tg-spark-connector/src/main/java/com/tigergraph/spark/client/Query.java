/**
 * Copyright (c) 2024 TigerGraph Inc.
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

import com.tigergraph.spark.client.common.RestppStreamResponse;
import com.tigergraph.spark.util.Utils;
import feign.Body;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import java.util.Map;

/** Query endpoint declaration used for Spark Read. */
public interface Query {
  /**
   * Run an installed query:
   * https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_run_an_installed_query_post
   */
  @RequestLine("POST /restpp/query/{graph}/{query}")
  @Headers({"Content-Type: application/json"})
  @Body("{param}")
  RestppStreamResponse installedQuery(
      @Param("graph") String graph,
      @Param("query") String query,
      @Param("param") String param,
      @QueryMap Map<String, Object> queryMap);

  /**
   * Run an interpreted query:
   * https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_run_an_interpreted_query
   *
   * @deprecated TG 4.1.0
   */
  @RequestLine("POST /gsqlserver/interpreted_query")
  @Headers({"Content-Type: text/plain"})
  @Body("{query}")
  RestppStreamResponse interpretedQueryV0(
      @Param("query") String query, @QueryMap Map<String, Object> param);

  /**
   * Run an interpreted query thru GSQL REST API v1:
   * https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_run_an_interpreted_query
   *
   * @since TG 4.1.0
   */
  @RequestLine("POST /gsql/v1/queries/interpret")
  @Headers({"Content-Type: text/plain"})
  @Body("{query}")
  RestppStreamResponse interpretedQueryV1(
      @Param("query") String query, @QueryMap Map<String, Object> param);

  default RestppStreamResponse interpretedQuery(
      String version, String query, Map<String, Object> param) {
    if (Utils.versionCmp(version, "4.1.0") >= 0) {
      return interpretedQueryV1(query, param);
    } else {
      return interpretedQueryV0(query, param);
    }
  }

  /**
   * List vertices:
   * https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_list_vertices
   */
  @RequestLine("GET /restpp/graph/{graph}/vertices/{vertexType}")
  RestppStreamResponse getVertices(
      @Param("graph") String graph,
      @Param("vertexType") String vertexType,
      @QueryMap Map<String, Object> queryMap);

  /**
   * Get a vertex:
   * https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_retrieve_a_vertex
   */
  @RequestLine("GET /restpp/graph/{graph}/vertices/{vertexType}/{vertexId}")
  RestppStreamResponse getVertex(
      @Param("graph") String graph,
      @Param("vertexType") String vertexType,
      @Param("vertexId") String vertexId,
      @QueryMap Map<String, Object> queryMap);

  /**
   * List edges of a vertex:
   * https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_list_edges_of_a_vertex
   */
  @RequestLine("GET /restpp/graph/{graph}/edges/{srcVertexType}/{srcVertexId}")
  RestppStreamResponse getEdgesBySrcVertex(
      @Param("graph") String graph,
      @Param("srcVertexType") String srcVertexType,
      @Param("srcVertexId") String srcVertexId,
      @QueryMap Map<String, Object> queryMap);

  /**
   * List edges of a vertex by edge type:
   * https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_list_edges_of_a_vertex_by_edge_type
   */
  @RequestLine("GET /restpp/graph/{graph}/edges/{srcVertexType}/{srcVertexId}/{edgeType}")
  RestppStreamResponse getEdgesBySrcVertexEdgeType(
      @Param("graph") String graph,
      @Param("srcVertexType") String srcVertexType,
      @Param("srcVertexId") String srcVertexId,
      @Param("edgeType") String edgeType,
      @QueryMap Map<String, Object> queryMap);

  /**
   * List edges of a vertex by edge type and target type:
   * https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_list_edges_of_a_vertex_by_edge_type_and_target_type
   */
  @RequestLine(
      "GET /restpp/graph/{graph}/edges/{srcVertexType}/{srcVertexId}/{edgeType}/{tgtVertexType}")
  RestppStreamResponse getEdgesBySrcVertexEdgeTypeTgtType(
      @Param("graph") String graph,
      @Param("srcVertexType") String srcVertexType,
      @Param("srcVertexId") String srcVertexId,
      @Param("edgeType") String edgeType,
      @Param("tgtVertexType") String tgtVertexType,
      @QueryMap Map<String, Object> queryMap);

  /**
   * Retrieve edge by source, target, and edge type:
   * https://docs.tigergraph.com/tigergraph-server/current/api/built-in-endpoints#_retrieve_edge_by_source_target_and_edge_type
   */
  @RequestLine(
      "GET /restpp/graph/{graph}/edges/{srcVertexType}/{srcVertexId}/{edgeType}/{tgtVertexType}/{tgtVertexId}")
  RestppStreamResponse getEdgeBySrcVertexEdgeTypeTgtVertex(
      @Param("graph") String graph,
      @Param("srcVertexType") String srcVertexType,
      @Param("srcVertexId") String srcVertexId,
      @Param("edgeType") String edgeType,
      @Param("tgtVertexType") String tgtVertexType,
      @Param("tgtVertexId") String tgtVertexId,
      @QueryMap Map<String, Object> queryMap);
}
