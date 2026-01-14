/**
 * Copyright (c) 2025 TigerGraph Inc.
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
package com.tigergraph.spark.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.tigergraph.spark.TigerGraphConnection;
import com.tigergraph.spark.util.Options;
import com.tigergraph.spark.write.upsert.TigerGraphUpsertDataWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.expressions.GenericInternalRow;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.unsafe.types.UTF8String;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TigerGraphUpsertDataWriter verifying request correctness for various upsert
 * options and configurations.
 */
@WireMockTest
public class TigerGraphUpsertRequestTest {

  private static final String TEST_VERSION = "99.99.99";
  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Set stubs with hard-coded response since here we are only verifying the request correctness.
   */
  @BeforeEach
  void setStub() {
    // Set version to 4.1.0 to ensure we are testing the latest connector feature
    stubFor(
        get("/restpp/version")
            .atPriority(1)
            .willReturn(okJson("{\"error\":\"false\", \"message\":\"Version: 99.99.99\"}")));
    // Assume RESTPP auth is disabled
    stubFor(get("/restpp/requesttoken").atPriority(1).willReturn(notFound()));
    // Return successful upsert response - match any query parameters
    stubFor(
        post(urlMatching("/restpp/graph/.*"))
            .atPriority(1)
            .willReturn(okJson("{\"error\":\"false\", \"message\":\"Success\", \"results\":[]}")));
  }

  /** Create a TigerGraphConnection with the given options. */
  private TigerGraphConnection createConnection(Map<String, String> opts, String baseUrl) {
    if (!opts.containsKey(Options.VERSION)) {
      opts.put(Options.VERSION, TEST_VERSION);
    }
    opts.put(Options.URL, baseUrl);
    return new TigerGraphConnection(new Options(opts, false));
  }

  /** Create a simple schema for Person vertex testing. */
  private StructType createPersonSchema() {
    return new StructType(
        new StructField[] {
          new StructField("id", DataTypes.StringType, false, null),
          new StructField("name", DataTypes.StringType, false, null),
          new StructField("age", DataTypes.IntegerType, false, null),
          new StructField("email", DataTypes.StringType, true, null)
        });
  }

  /** Create a simple schema for relationship edge testing. */
  private StructType createRelationshipSchema() {
    return new StructType(
        new StructField[] {
          new StructField("source_id", DataTypes.StringType, false, null),
          new StructField("target_id", DataTypes.StringType, false, null),
          new StructField("strength", DataTypes.DoubleType, false, null),
          new StructField("since_year", DataTypes.IntegerType, false, null)
        });
  }

  /** Create a sample Person row for testing. */
  private InternalRow createPersonRow(String id, String name, int age, String email) {
    Object[] values = {
      UTF8String.fromString(id),
      UTF8String.fromString(name),
      age,
      email != null ? UTF8String.fromString(email) : null
    };
    return new GenericInternalRow(values);
  }

  /** Create a sample Relationship row for testing. */
  private InternalRow createRelationshipRow(
      String sourceId, String targetId, double strength, int sinceYear) {
    Object[] values = {
      UTF8String.fromString(sourceId), UTF8String.fromString(targetId), strength, sinceYear
    };
    return new GenericInternalRow(values);
  }

  @Test
  void testBasicVertexUpsert(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    Map<String, String> opts = new HashMap<>();
    opts.put(Options.GRAPH, "test_graph");
    opts.put(Options.UPSERT_VERTEX_TYPE, "Person");
    opts.put(Options.UPSERT_VERTEX_ID_FIELD, "id");
    opts.put(Options.UPSERT_BATCH_SIZE_ROWS, "2");

    TigerGraphConnection conn = createConnection(opts, wmRuntimeInfo.getHttpBaseUrl());
    StructType schema = createPersonSchema();
    TigerGraphUpsertDataWriter writer = new TigerGraphUpsertDataWriter(schema, conn, 0, 1);

    // Add test data
    writer.write(createPersonRow("person1", "Alice", 28, "alice@example.com"));
    writer.write(createPersonRow("person2", "Bob", 35, "bob@example.com"));

    // Trigger batch processing
    writer.write(createPersonRow("person3", "Carol", 42, "carol@example.com"));
    writer.commit();

    // Verify the request was made correctly
    verify(
        postRequestedFor(urlPathEqualTo("/restpp/graph/test_graph"))
            .withHeader("Content-Type", equalTo("application/json")));

    // Verify the JSON payload structure
    List<com.github.tomakehurst.wiremock.verification.LoggedRequest> requests =
        findAll(postRequestedFor(urlPathEqualTo("/restpp/graph/test_graph")));
    assertTrue(requests.size() == 2);

    String requestBody = requests.get(0).getBodyAsString();
    JsonNode json = mapper.readTree(requestBody);

    assertTrue(json.has("vertices"));
    assertTrue(json.get("vertices").has("Person"));
    JsonNode personVertices = json.get("vertices").get("Person");
    assertTrue(personVertices.size() == 2);

    requestBody = requests.get(1).getBodyAsString();
    json = mapper.readTree(requestBody);
    assertTrue(json.has("vertices"));
    assertTrue(json.get("vertices").has("Person"));
    JsonNode companyVertices = json.get("vertices").get("Person");
    assertTrue(companyVertices.size() == 1);
  }

  @Test
  void testVertexUpsertWithAttributeMapping(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    Map<String, String> opts = new HashMap<>();
    opts.put(Options.GRAPH, "test_graph");
    opts.put(Options.UPSERT_VERTEX_TYPE, "Person");
    opts.put(Options.UPSERT_VERTEX_ID_FIELD, "id");
    opts.put(Options.UPSERT_ATTRIBUTE_MAPPING, "name:full_name,age:person_age,email:contact_email");
    opts.put(Options.UPSERT_BATCH_SIZE_ROWS, "1");

    TigerGraphConnection conn = createConnection(opts, wmRuntimeInfo.getHttpBaseUrl());
    StructType schema = createPersonSchema();
    TigerGraphUpsertDataWriter writer = new TigerGraphUpsertDataWriter(schema, conn, 0, 1);

    writer.write(createPersonRow("person1", "Alice", 28, "alice@example.com"));
    writer.commit();

    List<com.github.tomakehurst.wiremock.verification.LoggedRequest> requests =
        findAll(postRequestedFor(urlPathEqualTo("/restpp/graph/test_graph")));
    String requestBody = requests.get(0).getBodyAsString();
    JsonNode json = mapper.readTree(requestBody);

    JsonNode person1 = json.get("vertices").get("Person").get("person1");
    assertTrue(person1.has("full_name"));
    assertTrue(person1.has("person_age"));
    assertTrue(person1.has("contact_email"));
  }

  @Test
  void testBasicEdgeUpsert(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    Map<String, String> opts = new HashMap<>();
    opts.put(Options.GRAPH, "test_graph");
    opts.put(Options.UPSERT_EDGE_TYPE, "KNOWS");
    opts.put(Options.UPSERT_EDGE_SOURCE_TYPE, "Person");
    opts.put(Options.UPSERT_EDGE_SOURCE_ID_FIELD, "source_id");
    opts.put(Options.UPSERT_EDGE_TARGET_TYPE, "Person");
    opts.put(Options.UPSERT_EDGE_TARGET_ID_FIELD, "target_id");
    opts.put(Options.UPSERT_BATCH_SIZE_ROWS, "1");

    TigerGraphConnection conn = createConnection(opts, wmRuntimeInfo.getHttpBaseUrl());
    StructType schema = createRelationshipSchema();
    TigerGraphUpsertDataWriter writer = new TigerGraphUpsertDataWriter(schema, conn, 0, 1);

    writer.write(createRelationshipRow("person1", "person2", 0.8, 2020));
    writer.commit();

    List<com.github.tomakehurst.wiremock.verification.LoggedRequest> requests =
        findAll(postRequestedFor(urlPathEqualTo("/restpp/graph/test_graph")));
    String requestBody = requests.get(0).getBodyAsString();
    JsonNode json = mapper.readTree(requestBody);

    assertTrue(json.has("edges"));
    assertTrue(json.get("edges").has("Person"));
    assertTrue(json.get("edges").get("Person").has("person1"));
    assertTrue(json.get("edges").get("Person").get("person1").has("KNOWS"));
    assertTrue(json.get("edges").get("Person").get("person1").get("KNOWS").has("Person"));
    assertTrue(
        json.get("edges").get("Person").get("person1").get("KNOWS").get("Person").has("person2"));
  }

  @Test
  void testVertexUpsertWithNewOnlyOption(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    Map<String, String> opts = new HashMap<>();
    opts.put(Options.GRAPH, "test_graph");
    opts.put(Options.UPSERT_VERTEX_TYPE, "Person");
    opts.put(Options.UPSERT_VERTEX_ID_FIELD, "id");
    opts.put(Options.UPSERT_VERTEX_NEW_ONLY, "true");
    opts.put(Options.UPSERT_BATCH_SIZE_ROWS, "1");

    TigerGraphConnection conn = createConnection(opts, wmRuntimeInfo.getHttpBaseUrl());
    StructType schema = createPersonSchema();
    TigerGraphUpsertDataWriter writer = new TigerGraphUpsertDataWriter(schema, conn, 0, 1);

    writer.write(createPersonRow("person1", "Alice", 28, "alice@example.com"));
    writer.commit();

    verify(
        exactly(1),
        postRequestedFor(urlPathEqualTo("/restpp/graph/test_graph"))
            .withQueryParam("new_vertex_only", equalTo("true")));
  }

  @Test
  void testVertexUpsertWithAttributeOperations(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    Map<String, String> opts = new HashMap<>();
    opts.put(Options.GRAPH, "test_graph");
    opts.put(Options.UPSERT_VERTEX_TYPE, "Person");
    opts.put(Options.UPSERT_VERTEX_ID_FIELD, "id");
    opts.put(Options.UPSERT_ATTRIBUTE_OP, "age:max,name:set");
    opts.put(Options.UPSERT_BATCH_SIZE_ROWS, "1");

    TigerGraphConnection conn = createConnection(opts, wmRuntimeInfo.getHttpBaseUrl());
    StructType schema = createPersonSchema();
    TigerGraphUpsertDataWriter writer = new TigerGraphUpsertDataWriter(schema, conn, 0, 1);

    writer.write(createPersonRow("person1", "Alice", 28, "alice@example.com"));
    writer.commit();

    List<com.github.tomakehurst.wiremock.verification.LoggedRequest> requests =
        findAll(postRequestedFor(urlPathEqualTo("/restpp/graph/test_graph")));
    String requestBody = requests.get(0).getBodyAsString();
    JsonNode json = mapper.readTree(requestBody);

    JsonNode person1 = json.get("vertices").get("Person").get("person1");
    JsonNode ageAttr = person1.get("age");
    JsonNode nameAttr = person1.get("name");

    assertTrue(ageAttr.has("value"));
    assertTrue(ageAttr.has("op"));
    assertEquals("max", ageAttr.get("op").asText());

    assertTrue(nameAttr.has("value"));
    assertTrue(nameAttr.has("op"));
    assertEquals("set", nameAttr.get("op").asText());
  }

  @Test
  void testVertexUpsertWithUpdateOnlyOption(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    Map<String, String> opts = new HashMap<>();
    opts.put(Options.GRAPH, "test_graph");
    opts.put(Options.UPSERT_VERTEX_TYPE, "Person");
    opts.put(Options.UPSERT_VERTEX_ID_FIELD, "id");
    opts.put(Options.UPSERT_VERTEX_UPDATE_ONLY, "true");
    opts.put(Options.UPSERT_BATCH_SIZE_ROWS, "1");

    TigerGraphConnection conn = createConnection(opts, wmRuntimeInfo.getHttpBaseUrl());
    StructType schema = createPersonSchema();
    TigerGraphUpsertDataWriter writer = new TigerGraphUpsertDataWriter(schema, conn, 0, 1);

    writer.write(createPersonRow("person1", "Alice", 28, "alice@example.com"));
    writer.commit();

    verify(
        exactly(1),
        postRequestedFor(urlPathEqualTo("/restpp/graph/test_graph"))
            .withQueryParam("update_vertex_only", equalTo("true")));
  }

  @Test
  void testEdgeUpsertWithVertexMustExistOptions(WireMockRuntimeInfo wmRuntimeInfo)
      throws Exception {
    Map<String, String> opts = new HashMap<>();
    opts.put(Options.GRAPH, "test_graph");
    opts.put(Options.UPSERT_EDGE_TYPE, "KNOWS");
    opts.put(Options.UPSERT_EDGE_SOURCE_TYPE, "Person");
    opts.put(Options.UPSERT_EDGE_SOURCE_ID_FIELD, "source_id");
    opts.put(Options.UPSERT_EDGE_TARGET_TYPE, "Person");
    opts.put(Options.UPSERT_EDGE_TARGET_ID_FIELD, "target_id");
    opts.put(Options.UPSERT_EDGE_VERTEX_MUST_EXIST, "true");
    opts.put(Options.UPSERT_EDGE_SOURCE_VERTEX_MUST_EXIST, "true");
    opts.put(Options.UPSERT_EDGE_TARGET_VERTEX_MUST_EXIST, "true");
    opts.put(Options.UPSERT_BATCH_SIZE_ROWS, "1");

    TigerGraphConnection conn = createConnection(opts, wmRuntimeInfo.getHttpBaseUrl());
    StructType schema = createRelationshipSchema();
    TigerGraphUpsertDataWriter writer = new TigerGraphUpsertDataWriter(schema, conn, 0, 1);

    writer.write(createRelationshipRow("person1", "person2", 0.8, 2020));
    writer.commit();

    verify(
        exactly(1),
        postRequestedFor(urlPathEqualTo("/restpp/graph/test_graph"))
            .withQueryParam("vertex_must_exist", equalTo("true"))
            .withQueryParam("source_vertex_must_exist", equalTo("true"))
            .withQueryParam("target_vertex_must_exist", equalTo("true")));
  }

  @Test
  void testUpsertWithAckAllOption(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    Map<String, String> opts = new HashMap<>();
    opts.put(Options.GRAPH, "test_graph");
    opts.put(Options.UPSERT_VERTEX_TYPE, "Person");
    opts.put(Options.UPSERT_VERTEX_ID_FIELD, "id");
    opts.put(Options.UPSERT_ACK, "all");
    opts.put(Options.UPSERT_BATCH_SIZE_ROWS, "1");

    TigerGraphConnection conn = createConnection(opts, wmRuntimeInfo.getHttpBaseUrl());
    StructType schema = createPersonSchema();
    TigerGraphUpsertDataWriter writer = new TigerGraphUpsertDataWriter(schema, conn, 0, 1);

    writer.write(createPersonRow("person1", "Alice", 28, "alice@example.com"));
    writer.commit();

    verify(
        exactly(1),
        postRequestedFor(urlPathEqualTo("/restpp/graph/test_graph"))
            .withQueryParam("ack", equalTo("all")));
  }

  @Test
  void testUpsertWithAckNoneOption(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    Map<String, String> opts = new HashMap<>();
    opts.put(Options.GRAPH, "test_graph");
    opts.put(Options.UPSERT_VERTEX_TYPE, "Person");
    opts.put(Options.UPSERT_VERTEX_ID_FIELD, "id");
    opts.put(Options.UPSERT_ACK, "none");
    opts.put(Options.UPSERT_BATCH_SIZE_ROWS, "1");

    TigerGraphConnection conn = createConnection(opts, wmRuntimeInfo.getHttpBaseUrl());
    StructType schema = createPersonSchema();
    TigerGraphUpsertDataWriter writer = new TigerGraphUpsertDataWriter(schema, conn, 0, 1);

    writer.write(createPersonRow("person1", "Alice", 28, "alice@example.com"));
    writer.commit();

    verify(
        exactly(1),
        postRequestedFor(urlPathEqualTo("/restpp/graph/test_graph"))
            .withQueryParam("ack", equalTo("none")));
  }

  @Test
  void testBatchSizeProcessing(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    Map<String, String> opts = new HashMap<>();
    opts.put(Options.GRAPH, "test_graph");
    opts.put(Options.UPSERT_VERTEX_TYPE, "Person");
    opts.put(Options.UPSERT_VERTEX_ID_FIELD, "id");
    opts.put(Options.UPSERT_BATCH_SIZE_ROWS, "2"); // Batch size of 2

    TigerGraphConnection conn = createConnection(opts, wmRuntimeInfo.getHttpBaseUrl());
    StructType schema = createPersonSchema();
    TigerGraphUpsertDataWriter writer = new TigerGraphUpsertDataWriter(schema, conn, 0, 1);

    // Add 3 rows - should trigger one batch of 2 and one final batch of 1
    writer.write(createPersonRow("person1", "Alice", 28, "alice@example.com"));
    writer.write(createPersonRow("person2", "Bob", 35, "bob@example.com")); // Triggers first batch
    writer.write(createPersonRow("person3", "Carol", 42, "carol@example.com"));
    writer.commit(); // Triggers second batch

    // Should have made exactly 2 requests
    verify(exactly(2), postRequestedFor(urlPathEqualTo("/restpp/graph/test_graph")));
  }

  @Test
  void testComplexEdgeJsonStructure(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    Map<String, String> opts = new HashMap<>();
    opts.put(Options.GRAPH, "test_graph");
    opts.put(Options.UPSERT_EDGE_TYPE, "KNOWS");
    opts.put(Options.UPSERT_EDGE_SOURCE_TYPE, "Person");
    opts.put(Options.UPSERT_EDGE_SOURCE_ID_FIELD, "source_id");
    opts.put(Options.UPSERT_EDGE_TARGET_TYPE, "Person");
    opts.put(Options.UPSERT_EDGE_TARGET_ID_FIELD, "target_id");
    opts.put(Options.UPSERT_BATCH_SIZE_ROWS, "2");

    TigerGraphConnection conn = createConnection(opts, wmRuntimeInfo.getHttpBaseUrl());
    StructType schema = createRelationshipSchema();
    TigerGraphUpsertDataWriter writer = new TigerGraphUpsertDataWriter(schema, conn, 0, 1);

    // Add multiple edges to test JSON structure
    writer.write(createRelationshipRow("person1", "person2", 0.8, 2020));
    writer.write(createRelationshipRow("person1", "person3", 0.6, 2019));
    writer.commit();

    List<com.github.tomakehurst.wiremock.verification.LoggedRequest> requests =
        findAll(postRequestedFor(urlPathEqualTo("/restpp/graph/test_graph")));
    String requestBody = requests.get(0).getBodyAsString();
    JsonNode json = mapper.readTree(requestBody);

    // Verify complex edge structure: edges -> Person -> person1 -> KNOWS -> Person -> {person2,
    // person3}
    JsonNode edges = json.get("edges");
    assertNotNull(edges);

    JsonNode personType = edges.get("Person");
    assertNotNull(personType);

    JsonNode person1 = personType.get("person1");
    assertNotNull(person1);

    JsonNode knowsEdge = person1.get("KNOWS");
    assertNotNull(knowsEdge);

    JsonNode targetPersonType = knowsEdge.get("Person");
    assertNotNull(targetPersonType);

    // Should have both person2 and person3 as targets
    assertTrue(targetPersonType.has("person2"));
    assertTrue(targetPersonType.has("person3"));
  }

  @Test
  void testUpsertRequestHeaders(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    // Test 1: Only atomic option is given
    Map<String, String> opts1 = new HashMap<>();
    opts1.put(Options.GRAPH, "test_graph");
    opts1.put(Options.UPSERT_VERTEX_TYPE, "Person");
    opts1.put(Options.UPSERT_VERTEX_ID_FIELD, "id");
    opts1.put(Options.UPSERT_ATOMIC, "true");
    opts1.put(Options.UPSERT_BATCH_SIZE_ROWS, "1");

    TigerGraphConnection conn1 = createConnection(opts1, wmRuntimeInfo.getHttpBaseUrl());
    StructType schema = createPersonSchema();
    TigerGraphUpsertDataWriter writer1 = new TigerGraphUpsertDataWriter(schema, conn1, 0, 1);

    writer1.write(createPersonRow("person1", "Alice", 28, "alice@example.com"));
    writer1.commit();

    verify(
        exactly(1),
        postRequestedFor(urlPathEqualTo("/restpp/graph/test_graph"))
            .withHeader("gsql-atomic-level", equalTo("atomic")));

    // Reset WireMock for next test
    reset();
    setStub();

    // Test 2: Only timeout option is given
    Map<String, String> opts2 = new HashMap<>();
    opts2.put(Options.GRAPH, "test_graph");
    opts2.put(Options.UPSERT_VERTEX_TYPE, "Person");
    opts2.put(Options.UPSERT_VERTEX_ID_FIELD, "id");
    opts2.put(Options.UPSERT_TIMEOUT_MS, "30000");
    opts2.put(Options.UPSERT_BATCH_SIZE_ROWS, "1");

    TigerGraphConnection conn2 = createConnection(opts2, wmRuntimeInfo.getHttpBaseUrl());
    TigerGraphUpsertDataWriter writer2 = new TigerGraphUpsertDataWriter(schema, conn2, 0, 1);

    writer2.write(createPersonRow("person2", "Bob", 35, "bob@example.com"));
    writer2.commit();

    verify(
        exactly(1),
        postRequestedFor(urlPathEqualTo("/restpp/graph/test_graph"))
            .withHeader("gsql-timeout", equalTo("30000"))
            .withHeader("gsql-atomic-level", equalTo("nonatomic"))); // Default is nonatomic

    // Reset WireMock for next test
    reset();
    setStub();

    // Test 3: Both atomic and timeout options are given
    Map<String, String> opts3 = new HashMap<>();
    opts3.put(Options.GRAPH, "test_graph");
    opts3.put(Options.UPSERT_VERTEX_TYPE, "Person");
    opts3.put(Options.UPSERT_VERTEX_ID_FIELD, "id");
    opts3.put(Options.UPSERT_ATOMIC, "true");
    opts3.put(Options.UPSERT_TIMEOUT_MS, "60000");
    opts3.put(Options.UPSERT_BATCH_SIZE_ROWS, "1");

    TigerGraphConnection conn3 = createConnection(opts3, wmRuntimeInfo.getHttpBaseUrl());
    TigerGraphUpsertDataWriter writer3 = new TigerGraphUpsertDataWriter(schema, conn3, 0, 1);

    writer3.write(createPersonRow("person3", "Carol", 42, "carol@example.com"));
    writer3.commit();

    verify(
        exactly(1),
        postRequestedFor(urlPathEqualTo("/restpp/graph/test_graph"))
            .withHeader("gsql-atomic-level", equalTo("atomic"))
            .withHeader("gsql-timeout", equalTo("60000")));
  }
}
