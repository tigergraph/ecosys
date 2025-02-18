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
package com.tigergraph.spark.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.tigergraph.spark.TigerGraphTable;
import com.tigergraph.spark.TigerGraphTableProvider;
import com.tigergraph.spark.read.TigerGraphBatch;
import com.tigergraph.spark.read.TigerGraphPartitionReaderFactory;
import com.tigergraph.spark.read.TigerGraphScan;
import com.tigergraph.spark.read.TigerGraphScanBuilder;
import com.tigergraph.spark.util.Options;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.apache.spark.SparkConf;
import org.apache.spark.serializer.JavaSerializer;
import org.apache.spark.serializer.SerializerInstance;
import org.apache.spark.sql.connector.read.InputPartition;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Given read options, and verify the ultimate RESTPP request url which should match the query and
 * operator options.
 */
@WireMockTest
public class TigerGraphReadRequestTest {

  private static final String TEST_VERSION = "99.99.99";

  /**
   * Set stubs with hard-coded response since here we are only verifying the request correctness.
   */
  @BeforeEach
  void setStub() {
    // Set version to 99.99.99 to ensure we are always testing the latest connector feature
    stubFor(
        get("/restpp/version")
            .atPriority(1)
            .willReturn(okJson("{\"error\":\"false\", \"message\":\"Version: 99.99.99\"}")));
    // Assume RESTPP auth is disabled
    stubFor(get("/restpp/requesttoken").atPriority(1).willReturn(notFound()));
    // Return empty results as we are only verifying the request
    stubFor(
        any(urlMatching(".*"))
            .atPriority(100)
            .willReturn(okJson("{\"error\":\"false\", \"results\":[]}")));
  }

  /**
   * This static call will accept the read options and simulate the full cycle of a Spark read
   * operation til sending the request to RESTPP. We can verify the requests after executing it.
   *
   * @param opts Spark read options
   */
  static void mockSparkExecution(Map<String, String> opts, StructType schema) {
    if (!opts.containsKey(Options.VERSION)) {
      opts.put(Options.VERSION, TEST_VERSION);
    }
    TigerGraphTableProvider provider = new TigerGraphTableProvider();
    TigerGraphTable table = provider.getTable(schema, null, opts);
    TigerGraphScanBuilder scanBuilder = table.newScanBuilder(new CaseInsensitiveStringMap(opts));
    TigerGraphScan scan = scanBuilder.build();
    TigerGraphBatch batchRead = scan.toBatch();
    InputPartition[] partitions = batchRead.planInputPartitions();
    TigerGraphPartitionReaderFactory readerFactory = serdes(batchRead.createReaderFactory());
    for (int i = 0; i < partitions.length; i++) {
      readerFactory.createReader(partitions[i]);
    }
    // after `createReader`, the RESTPP request is already sent and we can directly verify it
  }

  // Spark serialize it in driver, and deserialize in executor
  static TigerGraphPartitionReaderFactory serdes(TigerGraphPartitionReaderFactory factory) {
    SparkConf conf = new SparkConf().setAppName("TestSerialization").setMaster("local[*]");
    conf.set("spark.serializer", "org.apache.spark.serializer.JavaSerializer");
    JavaSerializer javaSerializer = new JavaSerializer(conf);
    SerializerInstance serializer = javaSerializer.newInstance();
    ByteBuffer serializedData =
        serializer.serialize(
            factory, scala.reflect.ClassTag.apply(TigerGraphPartitionReaderFactory.class));
    return (TigerGraphPartitionReaderFactory)
        serializer.deserialize(
            serializedData, scala.reflect.ClassTag.apply(TigerGraphPartitionReaderFactory.class));
  }

  @Test
  void testSendVertexQueryWithOperator(WireMockRuntimeInfo wmRuntimeInfo) {
    Map<String, String> opts =
        new HashMap<String, String>() {
          {
            put("url", wmRuntimeInfo.getHttpBaseUrl());
            put("graph", "Social_Net");
            put("query.vertex", "Comment.123");
            put("query.op.sort", "id,time");
            put("query.op.select", "content");
          }
        };

    mockSparkExecution(opts, StructType.fromDDL("v_id int, content String"));
    verify(
        exactly(1),
        getRequestedFor(
            urlEqualTo(
                "/restpp/graph/Social_Net/vertices/Comment/123?select=content&sort=id%2Ctime")));
  }

  @Test
  void testSendVerticesQueryWithOperator(WireMockRuntimeInfo wmRuntimeInfo) {
    Map<String, String> opts =
        new HashMap<String, String>() {
          {
            put("url", wmRuntimeInfo.getHttpBaseUrl());
            put("graph", "Social_Net");
            put("query.vertex", "Comment");
            put("query.op.sort", "id,time");
            put("query.op.select", "content");
            put("query.op.filter", "length>0");
          }
        };

    mockSparkExecution(opts, StructType.fromDDL("v_id int, content String"));
    verify(
        exactly(1),
        getRequestedFor(
            urlEqualTo(
                "/restpp/graph/Social_Net/vertices/Comment?filter=length%3E0&select=content&sort=id%2Ctime")));
  }

  @Test
  void testSendQueryWithGSQLHeaders(WireMockRuntimeInfo wmRuntimeInfo) {
    Map<String, String> opts =
        new HashMap<String, String>() {
          {
            put("url", wmRuntimeInfo.getHttpBaseUrl());
            put("graph", "Social_Net");
            put("query.vertex", "Comment");
            put("query.timeout.ms", "12345");
            put("query.max.response.bytes", "54321");
          }
        };

    mockSparkExecution(opts, StructType.fromDDL("v_id int, content String"));
    verify(
        exactly(1),
        getRequestedFor(urlEqualTo("/restpp/graph/Social_Net/vertices/Comment"))
            .withHeader("GSQL-TIMEOUT", matching("12345"))
            .withHeader("RESPONSE-LIMIT", matching("54321")));
  }

  @Test
  void testPartitionedQueryWithFilter(WireMockRuntimeInfo wmRuntimeInfo) {
    Map<String, String> opts =
        new HashMap<String, String>() {
          {
            put("url", wmRuntimeInfo.getHttpBaseUrl());
            put("graph", "Social_Net");
            put("query.vertex", "Comment");
            put("query.op.select", "content");
            put("query.op.filter", "x=y");
            put("query.partition.key", "length");
            put("query.partition.num", "4");
            put("query.partition.lower.bound", "10");
            put("query.partition.upper.bound", "20");
          }
        };

    mockSparkExecution(opts, StructType.fromDDL("v_id int, content String"));
    String[] expected =
        new String[] {
          // length < 10
          "/restpp/graph/Social_Net/vertices/Comment?filter=x%3Dy%2Clength%3C10&select=content",
          // length >=10, length < 15
          "/restpp/graph/Social_Net/vertices/Comment?filter=x%3Dy%2Clength%3E%3D10%2Clength%3C15&select=content",
          // length >=15, length < 20
          "/restpp/graph/Social_Net/vertices/Comment?filter=x%3Dy%2Clength%3E%3D15%2Clength%3C20&select=content",
          // length >=20
          "/restpp/graph/Social_Net/vertices/Comment?filter=x%3Dy%2Clength%3E%3D20&select=content"
        };
    for (int i = 0; i < 4; i++) {
      verify(exactly(1), getRequestedFor(urlEqualTo(expected[i])));
    }
  }

  @Test
  void testListEdgesOfAVertex(WireMockRuntimeInfo wmRuntimeInfo) {
    Map<String, String> opts =
        new HashMap<String, String>() {
          {
            put("url", wmRuntimeInfo.getHttpBaseUrl());
            put("graph", "Social_Net");
            put("query.edge", "Person.tom");
          }
        };

    mockSparkExecution(opts, StructType.fromDDL("a String"));
    verify(exactly(1), getRequestedFor(urlEqualTo("/restpp/graph/Social_Net/edges/Person/tom")));
  }

  @Test
  void testListEdgesOfAVertexByEdgeType(WireMockRuntimeInfo wmRuntimeInfo) {
    Map<String, String> opts =
        new HashMap<String, String>() {
          {
            put("url", wmRuntimeInfo.getHttpBaseUrl());
            put("graph", "Social_Net");
            put("query.edge", "Person.tom.Friendship");
          }
        };

    mockSparkExecution(opts, StructType.fromDDL("a String"));
    verify(
        exactly(1),
        getRequestedFor(urlEqualTo("/restpp/graph/Social_Net/edges/Person/tom/Friendship")));
  }

  @Test
  void testListEdgesOfAVertexByEdgeTypeAndTargetType(WireMockRuntimeInfo wmRuntimeInfo) {
    Map<String, String> opts =
        new HashMap<String, String>() {
          {
            put("url", wmRuntimeInfo.getHttpBaseUrl());
            put("graph", "Social_Net");
            put("query.edge", "Person.tom.Friendship.Person");
          }
        };

    mockSparkExecution(opts, StructType.fromDDL("a String"));
    verify(
        exactly(1),
        getRequestedFor(urlEqualTo("/restpp/graph/Social_Net/edges/Person/tom/Friendship/Person")));
  }

  @Test
  void testRetrieveEdgeBySourceTargetAndEdgeType(WireMockRuntimeInfo wmRuntimeInfo) {
    Map<String, String> opts =
        new HashMap<String, String>() {
          {
            put("url", wmRuntimeInfo.getHttpBaseUrl());
            put("graph", "Social_Net");
            put("query.edge", "Person.tom.Friendship.Person.jack");
          }
        };

    mockSparkExecution(opts, StructType.fromDDL("a String"));
    verify(
        exactly(1),
        getRequestedFor(
            urlEqualTo("/restpp/graph/Social_Net/edges/Person/tom/Friendship/Person/jack")));
  }

  @Test
  void testInstalledQuery(WireMockRuntimeInfo wmRuntimeInfo) {
    Map<String, String> opts =
        new HashMap<String, String>() {
          {
            put("url", wmRuntimeInfo.getHttpBaseUrl());
            put("graph", "Social_Net");
            put("query.installed", "queryA");
            put("query.params", "{\"a\":\"v1\", \"b\": 999}");
          }
        };

    mockSparkExecution(opts, StructType.fromDDL("a String"));
    verify(
        exactly(1),
        postRequestedFor(urlEqualTo("/restpp/query/Social_Net/queryA"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalTo("{\"a\":\"v1\", \"b\": 999}")));
  }

  @Test
  void testInterpretedQuery(WireMockRuntimeInfo wmRuntimeInfo) {
    Map<String, String> opts =
        new HashMap<String, String>() {
          {
            put("url", wmRuntimeInfo.getHttpBaseUrl());
            put("graph", "Social_Net");
            put(
                "query.interpreted",
                "INTERPRET QUERY (STRING a, INT b) FOR GRAPH gsql_demo {PRINT a,b;}");
            put("query.params", "{\"a\":\"v1\", \"b\": [999,1,2]}");
            put("username", "tigergraph");
            put("password", "tigergraph");
            put("version", "3.10.1");
          }
        };

    mockSparkExecution(opts, StructType.fromDDL("a String"));
    verify(
        exactly(1),
        postRequestedFor(urlEqualTo("/gsqlserver/interpreted_query?a=v1&b=999&b=1&b=2"))
            .withRequestBody(
                equalTo("INTERPRET QUERY (STRING a, INT b) FOR GRAPH gsql_demo {PRINT a,b;}")));
  }

  @Test
  void testInterpretedQueryRestV1(WireMockRuntimeInfo wmRuntimeInfo) {
    Map<String, String> opts =
        new HashMap<String, String>() {
          {
            put("url", wmRuntimeInfo.getHttpBaseUrl());
            put("graph", "Social_Net");
            put(
                "query.interpreted",
                "INTERPRET QUERY (STRING a, INT b) FOR GRAPH gsql_demo {PRINT a,b;}");
            put("query.params", "{\"a\":\"v1\", \"b\": [999,1,2]}");
            put("username", "tigergraph");
            put("password", "tigergraph");
            put("version", "4.1.0");
          }
        };

    mockSparkExecution(opts, StructType.fromDDL("a String"));
    verify(
        exactly(1),
        postRequestedFor(urlEqualTo("/gsql/v1/queries/interpret?a=v1&b=999&b=1&b=2"))
            .withRequestBody(
                equalTo("INTERPRET QUERY (STRING a, INT b) FOR GRAPH gsql_demo {PRINT a,b;}")));
  }
}
