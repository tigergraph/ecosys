package com.tigergraph.spark.write.upsert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.tigergraph.spark.write.upsert.UpsertJsonBuilder.EdgeJsonBuilder;
import com.tigergraph.spark.write.upsert.UpsertJsonBuilder.VertexJsonBuilder;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.expressions.GenericInternalRow;
import org.apache.spark.sql.catalyst.util.GenericArrayData;
import org.apache.spark.sql.types.*;
import org.apache.spark.unsafe.types.UTF8String;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class UpsertJsonBuilderTest {

  @BeforeEach
  void setUp() {}

  private StructType createSimpleSchema() {
    return new StructType()
        .add("id", DataTypes.StringType, false)
        .add("name", DataTypes.StringType, true)
        .add("age", DataTypes.IntegerType, true);
  }

  private StructType createAllTypesSchema() {
    return new StructType()
        .add("v_id", DataTypes.StringType, false)
        .add("str_attr", DataTypes.StringType, true)
        .add("int_attr", DataTypes.IntegerType, true)
        .add("long_attr", DataTypes.LongType, true)
        .add("bool_attr", DataTypes.BooleanType, true)
        .add("double_attr", DataTypes.DoubleType, true)
        .add("float_attr", DataTypes.FloatType, true)
        .add("short_attr", DataTypes.ShortType, true)
        .add("byte_attr", DataTypes.ByteType, true)
        .add("date_attr", DataTypes.DateType, true)
        .add("ts_attr", DataTypes.TimestampType, true)
        .add("dec_attr", DataTypes.createDecimalType(10, 2), true)
        .add("arr_int_attr", DataTypes.createArrayType(DataTypes.IntegerType), true)
        .add("arr_str_attr", DataTypes.createArrayType(DataTypes.StringType), true)
        .add("arr_bool_attr", DataTypes.createArrayType(DataTypes.BooleanType), true)
        .add("arr_ts_attr", DataTypes.createArrayType(DataTypes.TimestampType), true);
  }

  // Helper to convert Java values to Spark's InternalRow representation
  private Object toSparkValue(Object value, DataType type) {
    if (value == null) {
      return null;
    }
    if (type instanceof StringType) {
      return UTF8String.fromString((String) value);
    }
    if (type instanceof DateType && value instanceof LocalDate) {
      return (int) ((LocalDate) value).toEpochDay();
    }
    if (type instanceof TimestampType && value instanceof Instant) {
      // Microseconds from epoch
      return ((Instant) value).getEpochSecond() * 1_000_000L + ((Instant) value).getNano() / 1_000L;
    }
    if (type instanceof TimestampNTZType && value instanceof LocalDateTime) {
      // Microseconds from epoch for TimestampNTZType in Spark 3.x
      // Spark stores TimestampNTZ as long representing microseconds since epoch UTC.
      return ((LocalDateTime) value).toEpochSecond(ZoneOffset.UTC) * 1_000_000L
          + ((LocalDateTime) value).getNano() / 1_000L;
    }
    if (type instanceof DecimalType && value instanceof BigDecimal) {
      return Decimal.apply((BigDecimal) value);
    }
    if (type instanceof ArrayType) {
      DataType elementType = ((ArrayType) type).elementType();
      if (value instanceof java.util.List) {
        java.util.List<?> list = (java.util.List<?>) value;
        Object[] sparkArray = new Object[list.size()];
        for (int i = 0; i < list.size(); i++) {
          sparkArray[i] = toSparkValue(list.get(i), elementType);
        }
        return new GenericArrayData(sparkArray);
      }
    }
    return value;
  }

  @Test
  void testVertexJsonBuilder_Simple() throws JsonProcessingException {
    StructType schema = createSimpleSchema();
    List<String> idFields = Arrays.asList("id");
    VertexJsonBuilder builder = new VertexJsonBuilder("Person", idFields, null, null, schema);

    Object[] values1 =
        new Object[] {
          toSparkValue("v1", DataTypes.StringType),
          toSparkValue("Alice", DataTypes.StringType),
          toSparkValue(30, DataTypes.IntegerType)
        };
    InternalRow row1 = new GenericInternalRow(values1);
    builder.add(row1);

    Object[] values2 =
        new Object[] {
          toSparkValue("v2", DataTypes.StringType),
          toSparkValue("Bob", DataTypes.StringType),
          toSparkValue(25, DataTypes.IntegerType)
        };
    InternalRow row2 = new GenericInternalRow(values2);
    builder.add(row2);

    // Expected JSON:
    // {
    //   "vertices": {
    //     "Person": {
    //       "v1": {
    //         "name": { "value": "Alice" },
    //         "age": { "value": 30 }
    //       },
    //       "v2": {
    //         "name": { "value": "Bob" },
    //         "age": { "value": 25 }
    //       }
    //     }
    //   }
    // }
    JsonNode result = builder.build();
    assertNotNull(result);
    assertTrue(result.has("vertices"));
    JsonNode verticesNode = result.get("vertices");
    assertTrue(verticesNode.has("Person"));
    JsonNode personNode = verticesNode.get("Person");

    assertTrue(personNode.has("v1"));
    JsonNode v1Node = personNode.get("v1");
    assertEquals("Alice", v1Node.get("name").get("value").asText());
    assertEquals(30, v1Node.get("age").get("value").asInt());

    assertTrue(personNode.has("v2"));
    JsonNode v2Node = personNode.get("v2");
    assertEquals("Bob", v2Node.get("name").get("value").asText());
    assertEquals(25, v2Node.get("age").get("value").asInt());
  }

  @Test
  void testVertexJsonBuilder_AllTypes() throws JsonProcessingException {
    StructType schema = createAllTypesSchema();
    List<String> idFields = Arrays.asList("v_id");
    VertexJsonBuilder builder = new VertexJsonBuilder("TestVertex", idFields, null, null, schema);

    Instant testInstant = Instant.parse("2023-01-15T10:30:45.123Z");
    LocalDate testDate = LocalDate.parse("2023-02-20");

    Object[] values =
        new Object[] {
          toSparkValue("vertex1", DataTypes.StringType), // v_id
          toSparkValue("hello", DataTypes.StringType), // str_attr
          toSparkValue(123, DataTypes.IntegerType), // int_attr
          toSparkValue(1234567890L, DataTypes.LongType), // long_attr
          toSparkValue(true, DataTypes.BooleanType), // bool_attr
          toSparkValue(123.456, DataTypes.DoubleType), // double_attr
          toSparkValue(12.34f, DataTypes.FloatType), // float_attr
          toSparkValue((short) 12, DataTypes.ShortType), // short_attr
          toSparkValue((byte) 1, DataTypes.ByteType), // byte_attr
          toSparkValue(testDate, DataTypes.DateType), // date_attr
          toSparkValue(testInstant, DataTypes.TimestampType), // ts_attr
          toSparkValue(new BigDecimal("123.45"), DataTypes.createDecimalType(10, 2)), // dec_attr
          toSparkValue(
              Arrays.asList(1, 2, 3),
              DataTypes.createArrayType(DataTypes.IntegerType)), // arr_int_attr
          toSparkValue(
              Arrays.asList("a", "b", "c"),
              DataTypes.createArrayType(DataTypes.StringType)), // arr_str_attr
          toSparkValue(
              Arrays.asList(true, false, true),
              DataTypes.createArrayType(DataTypes.BooleanType)), // arr_bool_attr
          toSparkValue(
              Arrays.asList(testInstant, testInstant.plusSeconds(60)),
              DataTypes.createArrayType(DataTypes.TimestampType)) // arr_ts_attr
        };
    InternalRow row = new GenericInternalRow(values);
    builder.add(row);

    // Expected JSON:
    // {
    //   "vertices": {
    //     "TestVertex": {
    //       "vertex1": {
    //         "str_attr": { "value": "hello" },
    //         "int_attr": { "value": 123 },
    //         "long_attr": { "value": 1234567890 },
    //         "bool_attr": { "value": true },
    //         "double_attr": { "value": 123.456 },
    //         "float_attr": { "value": 12.34 },
    //         "short_attr": { "value": 12 },
    //         "byte_attr": { "value": 1 },
    //         "date_attr": { "value": "2023-02-20" },
    //         "ts_attr": { "value": "2023-01-15 10:30:45.123" },
    //         "ts_ntz_attr": { "value": "2023-01-16 11:30:00.456..." }, // Value checked with
    // startsWith
    //         "dec_attr": { "value": 123.45 },
    //         "arr_int_attr": { "value": [1, 2, 3] },
    //         "arr_str_attr": { "value": ["a", "b", "c"] },
    //         "arr_bool_attr": { "value": [true, false, true] },
    //         "arr_ts_attr": { "value": ["2023-01-15 10:30:45.123", "2023-01-15 10:31:45.123"] }
    //       }
    //     }
    //   }
    // }
    JsonNode result = builder.build();
    JsonNode vertexNode = result.path("vertices").path("TestVertex").path("vertex1");

    assertEquals("hello", vertexNode.path("str_attr").path("value").asText());
    assertEquals(123, vertexNode.path("int_attr").path("value").asInt());
    assertEquals(1234567890L, vertexNode.path("long_attr").path("value").asLong());
    assertEquals(true, vertexNode.path("bool_attr").path("value").asBoolean());
    assertEquals(123.456, vertexNode.path("double_attr").path("value").asDouble(), 0.001);
    assertEquals(12.34f, vertexNode.path("float_attr").path("value").asDouble(), 0.001);
    assertEquals(12, vertexNode.path("short_attr").path("value").asInt());
    assertEquals(1, vertexNode.path("byte_attr").path("value").asInt());
    assertEquals("2023-02-20", vertexNode.path("date_attr").path("value").asText());
    assertTrue(vertexNode.path("ts_attr").path("value").asText().startsWith("2023-01-15"));
    assertEquals(123.45, vertexNode.path("dec_attr").path("value").asDouble(), 0.001);

    // Check arrays
    JsonNode intArray = vertexNode.path("arr_int_attr").path("value");
    assertTrue(intArray.isArray());
    assertEquals(3, intArray.size());
    assertEquals(1, intArray.get(0).asInt());
    assertEquals(2, intArray.get(1).asInt());
    assertEquals(3, intArray.get(2).asInt());

    JsonNode strArray = vertexNode.path("arr_str_attr").path("value");
    assertTrue(strArray.isArray());
    assertEquals(3, strArray.size());
    assertEquals("a", strArray.get(0).asText());
    assertEquals("b", strArray.get(1).asText());
    assertEquals("c", strArray.get(2).asText());

    JsonNode boolArray = vertexNode.path("arr_bool_attr").path("value");
    assertTrue(boolArray.isArray());
    assertEquals(3, boolArray.size());
    assertEquals(true, boolArray.get(0).asBoolean());
    assertEquals(false, boolArray.get(1).asBoolean());
    assertEquals(true, boolArray.get(2).asBoolean());

    JsonNode tsArray = vertexNode.path("arr_ts_attr").path("value");
    assertTrue(tsArray.isArray());
    assertEquals(2, tsArray.size());
    assertTrue(tsArray.get(0).asText().startsWith("2023-01-15"));
    assertTrue(tsArray.get(1).asText().startsWith("2023-01-15"));
  }

  @Test
  void testVertexJsonBuilder_NullVertexId() {
    StructType schema = createSimpleSchema();
    List<String> idFields = Arrays.asList("id");
    VertexJsonBuilder builder = new VertexJsonBuilder("Person", idFields, null, null, schema);
    Object[] values =
        new Object[] {
          null, toSparkValue("Alice", DataTypes.StringType), toSparkValue(30, DataTypes.IntegerType)
        };
    InternalRow row = new GenericInternalRow(values);
    builder.add(row); // Should be skipped

    // Expected JSON:
    // {
    //   "vertices": {
    //     "Person": {}
    //   }
    // }
    JsonNode result = builder.build();
    assertTrue(result.path("vertices").path("Person").isEmpty());
  }

  @Test
  void testVertexJsonBuilder_NullAttribute() throws JsonProcessingException {
    StructType schema = createSimpleSchema();
    List<String> idFields = Arrays.asList("id");
    VertexJsonBuilder builder = new VertexJsonBuilder("Person", idFields, null, null, schema);
    Object[] values =
        new Object[] {
          toSparkValue("v1", DataTypes.StringType), null, toSparkValue(30, DataTypes.IntegerType)
        };
    InternalRow row = new GenericInternalRow(values);
    builder.add(row);

    // Expected JSON:
    // {
    //   "vertices": {
    //     "Person": {
    //       "v1": {
    //         // "name" attribute is absent due to null value
    //         "age": { "value": 30 }
    //       }
    //     }
    //   }
    // }
    JsonNode result = builder.build();
    JsonNode v1Node = result.path("vertices").path("Person").path("v1");
    assertFalse(v1Node.has("name")); // Null attributes are not added to JSON
    assertEquals(30, v1Node.get("age").get("value").asInt());
  }

  @Test
  void testVertexJsonBuilder_AttributeMapping() throws JsonProcessingException {
    StructType schema =
        new StructType()
            .add("user_id", DataTypes.StringType, false)
            .add("user_name", DataTypes.StringType, true)
            .add("user_age", DataTypes.IntegerType, true);

    String attrMappingStr = "user_id:id,user_name:name,user_age:age";
    String attrOpStr = "age:add";

    List<String> idFields = Arrays.asList("user_id");
    VertexJsonBuilder builder =
        new VertexJsonBuilder("User", idFields, attrMappingStr, attrOpStr, schema);

    Object[] values =
        new Object[] {
          toSparkValue("u123", DataTypes.StringType),
          toSparkValue("TestUser", DataTypes.StringType),
          toSparkValue(40, DataTypes.IntegerType)
        };
    InternalRow row = new GenericInternalRow(values);
    builder.add(row);

    // Expected JSON:
    // {
    //   "vertices": {
    //     "User": {
    //       "u123": {
    //         "name": { "value": "TestUser" }, // "user_name" mapped to "name"
    //         "age": { "value": 40, "op": "add" } // "user_age" mapped to "age" with "add" op
    //       }
    //     }
    //   }
    // }
    JsonNode result = builder.build();
    JsonNode userNode = result.path("vertices").path("User").path("u123");

    assertFalse(userNode.has("user_id")); // Original name should not be present
    assertFalse(userNode.has("user_name"));
    assertFalse(userNode.has("user_age"));

    assertTrue(userNode.has("name")); // Mapped name
    assertEquals("TestUser", userNode.path("name").path("value").asText());

    assertTrue(userNode.has("age")); // Mapped name
    assertEquals(40, userNode.path("age").path("value").asInt());
    assertEquals("add", userNode.path("age").path("op").asText());
  }

  @Test
  void testVertexJsonBuilder_Clear() {
    StructType schema = createSimpleSchema();
    List<String> idFields = Arrays.asList("id");
    VertexJsonBuilder builder = new VertexJsonBuilder("Person", idFields, null, null, schema);
    Object[] values1 =
        new Object[] {
          toSparkValue("v1", DataTypes.StringType),
          toSparkValue("Alice", DataTypes.StringType),
          toSparkValue(30, DataTypes.IntegerType)
        };
    InternalRow row1 = new GenericInternalRow(values1);
    builder.add(row1);

    assertFalse(builder.build().path("vertices").path("Person").isEmpty());
    builder.clear();
    assertTrue(builder.build().path("vertices").path("Person").isEmpty());
  }

  // --- EdgeJsonBuilder Tests ---

  private StructType createEdgeSchema() {
    return new StructType()
        .add("src", DataTypes.StringType, false)
        .add("dst", DataTypes.StringType, false)
        .add("weight", DataTypes.DoubleType, true);
  }

  private StructType createEdgeSchemaAllTypes() {
    return new StructType()
        .add("s_id", DataTypes.StringType, false)
        .add("t_id", DataTypes.StringType, false)
        .add("str_attr", DataTypes.StringType, true)
        .add("int_attr", DataTypes.IntegerType, true)
        .add("long_attr", DataTypes.LongType, true)
        .add("bool_attr", DataTypes.BooleanType, true)
        .add("double_attr", DataTypes.DoubleType, true)
        .add("date_attr", DataTypes.DateType, true)
        .add("ts_attr", DataTypes.TimestampType, true)
        .add("arr_int_attr", DataTypes.createArrayType(DataTypes.IntegerType), true);
  }

  @Test
  void testEdgeJsonBuilder_Simple() throws JsonProcessingException {
    StructType schema = createEdgeSchema();
    List<String> sourceIdFields = Arrays.asList("src");
    List<String> targetIdFields = Arrays.asList("dst");
    EdgeJsonBuilder builder =
        new EdgeJsonBuilder(
            "Interacts", "User", sourceIdFields, "Product", targetIdFields, null, null, schema);

    Object[] values1 =
        new Object[] {
          toSparkValue("u1", DataTypes.StringType),
          toSparkValue("p1", DataTypes.StringType),
          toSparkValue(5.0, DataTypes.DoubleType)
        };
    InternalRow row1 = new GenericInternalRow(values1);
    builder.add(row1);

    Object[] values2 =
        new Object[] {
          toSparkValue("u1", DataTypes.StringType),
          toSparkValue("p2", DataTypes.StringType),
          toSparkValue(3.5, DataTypes.DoubleType)
        };
    InternalRow row2 = new GenericInternalRow(values2);
    builder.add(row2);

    Object[] values3 =
        new Object[] {
          toSparkValue("u2", DataTypes.StringType),
          toSparkValue("p2", DataTypes.StringType),
          toSparkValue(4.0, DataTypes.DoubleType)
        };
    InternalRow row3 = new GenericInternalRow(values3);
    builder.add(row3);

    // Expected JSON:
    // {
    //   "edges": {
    //     "User": { // Source vertex type
    //       "u1": { // Source vertex ID
    //         "Interacts": { // Edge type
    //           "Product": { // Target vertex type
    //             "p1": { // Target vertex ID
    //               "weight": { "value": 5.0 }
    //             },
    //             "p2": {
    //               "weight": { "value": 3.5 }
    //             }
    //           }
    //         }
    //       },
    //       "u2": {
    //         "Interacts": {
    //           "Product": {
    //             "p2": {
    //               "weight": { "value": 4.0 }
    //             }
    //           }
    //         }
    //       }
    //     }
    //   }
    // }
    JsonNode result = builder.build();
    assertTrue(result.has("edges"));
    JsonNode edgesNode = result.get("edges");
    assertTrue(edgesNode.has("User"));
    JsonNode userEdgesNode = edgesNode.get("User");

    // u1 -> p1
    JsonNode u1Edges = userEdgesNode.path("u1").path("Interacts").path("Product").path("p1");
    assertEquals(5.0, u1Edges.path("weight").path("value").asDouble(), 0.001);

    // u1 -> p2
    JsonNode u1Edges2 = userEdgesNode.path("u1").path("Interacts").path("Product").path("p2");
    assertEquals(3.5, u1Edges2.path("weight").path("value").asDouble(), 0.001);

    // u2 -> p2
    JsonNode u2Edges = userEdgesNode.path("u2").path("Interacts").path("Product").path("p2");
    assertEquals(4.0, u2Edges.path("weight").path("value").asDouble(), 0.001);
  }

  @Test
  void testEdgeJsonBuilder_AllTypes() throws JsonProcessingException {
    StructType schema = createEdgeSchemaAllTypes();
    List<String> sourceIdFields = Arrays.asList("s_id");
    List<String> targetIdFields = Arrays.asList("t_id");
    EdgeJsonBuilder builder =
        new EdgeJsonBuilder(
            "RELATED_TO", "TypeA", sourceIdFields, "TypeB", targetIdFields, null, null, schema);

    Instant testInstant = Instant.parse("2023-03-20T12:00:00Z");
    LocalDate testDate = LocalDate.parse("2023-04-10");

    Object[] values =
        new Object[] {
          toSparkValue("source1", DataTypes.StringType),
          toSparkValue("target1", DataTypes.StringType),
          toSparkValue("edge_str", DataTypes.StringType),
          toSparkValue(456, DataTypes.IntegerType),
          toSparkValue(9876543210L, DataTypes.LongType),
          toSparkValue(false, DataTypes.BooleanType),
          toSparkValue(789.012, DataTypes.DoubleType),
          toSparkValue(testDate, DataTypes.DateType),
          toSparkValue(testInstant, DataTypes.TimestampType),
          toSparkValue(Arrays.asList(10, 20, 30), DataTypes.createArrayType(DataTypes.IntegerType))
        };
    InternalRow row = new GenericInternalRow(values);
    builder.add(row);

    JsonNode result = builder.build();
    JsonNode edgeNode =
        result
            .path("edges")
            .path("TypeA")
            .path("source1")
            .path("RELATED_TO")
            .path("TypeB")
            .path("target1");

    assertEquals("edge_str", edgeNode.path("str_attr").path("value").asText());
    assertEquals(456, edgeNode.path("int_attr").path("value").asInt());
    assertEquals(9876543210L, edgeNode.path("long_attr").path("value").asLong());
    assertEquals(false, edgeNode.path("bool_attr").path("value").asBoolean());
    assertEquals(789.012, edgeNode.path("double_attr").path("value").asDouble(), 0.001);
    assertEquals("2023-04-10", edgeNode.path("date_attr").path("value").asText());
    assertTrue(edgeNode.path("ts_attr").path("value").asText().startsWith("2023-03-20"));

    JsonNode intArray = edgeNode.path("arr_int_attr").path("value");
    assertTrue(intArray.isArray());
    assertEquals(3, intArray.size());
    assertEquals(10, intArray.get(0).asInt());
    assertEquals(20, intArray.get(1).asInt());
    assertEquals(30, intArray.get(2).asInt());
  }

  @Test
  void testEdgeJsonBuilder_NullSourceId() {
    StructType schema = createEdgeSchema();
    List<String> sourceIdFields = Arrays.asList("src");
    List<String> targetIdFields = Arrays.asList("dst");
    EdgeJsonBuilder builder =
        new EdgeJsonBuilder(
            "LINK", "TypeS", sourceIdFields, "TypeT", targetIdFields, null, null, schema);
    Object[] values =
        new Object[] {
          null, toSparkValue("t1", DataTypes.StringType), toSparkValue(1.0, DataTypes.DoubleType)
        };
    InternalRow row = new GenericInternalRow(values);
    builder.add(row); // Should be skipped

    JsonNode result = builder.build();
    assertTrue(result.path("edges").isEmpty());
  }

  @Test
  void testEdgeJsonBuilder_NullTargetId() {
    StructType schema = createEdgeSchema();
    List<String> sourceIdFields = Arrays.asList("src");
    List<String> targetIdFields = Arrays.asList("dst");
    EdgeJsonBuilder builder =
        new EdgeJsonBuilder(
            "LINK", "TypeS", sourceIdFields, "TypeT", targetIdFields, null, null, schema);
    Object[] values =
        new Object[] {
          toSparkValue("s1", DataTypes.StringType), null, toSparkValue(1.0, DataTypes.DoubleType)
        };
    InternalRow row = new GenericInternalRow(values);
    builder.add(row); // Should be skipped

    JsonNode result = builder.build();
    assertTrue(result.path("edges").isEmpty());
  }

  @Test
  void testEdgeJsonBuilder_AttributeMapping() throws JsonProcessingException {
    StructType schema =
        new StructType()
            .add("s", DataTypes.StringType, false)
            .add("t", DataTypes.StringType, false)
            .add("w", DataTypes.DoubleType, true)
            .add("c", DataTypes.IntegerType, true);

    String attributeMappingStr = "w:weight,c:count";
    String attributeOpStr = "weight:+,count:max";

    List<String> sourceIdFields = Arrays.asList("s");
    List<String> targetIdFields = Arrays.asList("t");
    EdgeJsonBuilder builder =
        new EdgeJsonBuilder(
            "MY_EDGE",
            "SourceV",
            sourceIdFields,
            "TargetV",
            targetIdFields,
            attributeMappingStr,
            attributeOpStr,
            schema);

    Object[] values =
        new Object[] {
          toSparkValue("src1", DataTypes.StringType),
          toSparkValue("tgt1", DataTypes.StringType),
          toSparkValue(2.5, DataTypes.DoubleType),
          toSparkValue(10, DataTypes.IntegerType)
        };
    InternalRow row = new GenericInternalRow(values);
    builder.add(row);

    JsonNode result = builder.build();
    JsonNode edgeNode =
        result
            .path("edges")
            .path("SourceV")
            .path("src1")
            .path("MY_EDGE")
            .path("TargetV")
            .path("tgt1");

    assertFalse(edgeNode.has("w")); // Original name should not be present
    assertFalse(edgeNode.has("c"));

    assertTrue(edgeNode.has("weight")); // Mapped name
    assertEquals(2.5, edgeNode.path("weight").path("value").asDouble(), 0.001);
    assertEquals("+", edgeNode.path("weight").path("op").asText());

    assertTrue(edgeNode.has("count")); // Mapped name
    assertEquals(10, edgeNode.path("count").path("value").asInt());
    assertEquals("max", edgeNode.path("count").path("op").asText());
  }

  @Test
  void testEdgeJsonBuilder_Clear() {
    StructType schema = createEdgeSchema();
    List<String> sourceIdFields = Arrays.asList("src");
    List<String> targetIdFields = Arrays.asList("dst");
    EdgeJsonBuilder builder =
        new EdgeJsonBuilder(
            "Interacts", "User", sourceIdFields, "Product", targetIdFields, null, null, schema);
    Object[] values1 =
        new Object[] {
          toSparkValue("u1", DataTypes.StringType),
          toSparkValue("p1", DataTypes.StringType),
          toSparkValue(5.0, DataTypes.DoubleType)
        };
    InternalRow row1 = new GenericInternalRow(values1);
    builder.add(row1);

    assertFalse(builder.build().path("edges").isEmpty());
    builder.clear();
    assertTrue(builder.build().path("edges").isEmpty());
  }

  @Test
  void testVertexJsonBuilder_EmptyAttributeMappingString() throws JsonProcessingException {
    StructType schema = createSimpleSchema();
    List<String> idFields = Arrays.asList("id");
    VertexJsonBuilder builder = new VertexJsonBuilder("Person", idFields, "", "", schema);

    Object[] values =
        new Object[] {
          toSparkValue("v1", DataTypes.StringType),
          toSparkValue("Alice", DataTypes.StringType),
          toSparkValue(30, DataTypes.IntegerType)
        };
    InternalRow row = new GenericInternalRow(values);
    builder.add(row);

    JsonNode result = builder.build();
    JsonNode v1Node = result.path("vertices").path("Person").path("v1");
    assertEquals("Alice", v1Node.path("name").path("value").asText());
  }

  @Test
  void testEdgeJsonBuilder_EmptyAttributeMappingString() throws JsonProcessingException {
    StructType schema = createEdgeSchema();
    List<String> sourceIdFields = Arrays.asList("src");
    List<String> targetIdFields = Arrays.asList("dst");
    EdgeJsonBuilder builder =
        new EdgeJsonBuilder(
            "Interacts", "User", sourceIdFields, "Product", targetIdFields, "", "", schema);

    Object[] values =
        new Object[] {
          toSparkValue("u1", DataTypes.StringType),
          toSparkValue("p1", DataTypes.StringType),
          toSparkValue(5.0, DataTypes.DoubleType)
        };
    InternalRow row = new GenericInternalRow(values);
    builder.add(row);

    JsonNode result = builder.build();
    JsonNode edgeNode =
        result.path("edges").path("User").path("u1").path("Interacts").path("Product").path("p1");
    assertEquals(5.0, edgeNode.path("weight").path("value").asDouble(), 0.001);
  }

  @Test
  void testVertexJsonBuilder_CompositeKey_UnsupportedType() {
    StructType schema =
        new StructType()
            .add("user_id", DataTypes.StringType, false)
            .add("tenant_id", DataTypes.BooleanType, false) // Unsupported type for ID
            .add("name", DataTypes.StringType, true);

    List<String> compositeKeyFields = Arrays.asList("user_id", "tenant_id");

    // Should throw exception due to unsupported tenant_id type
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new VertexJsonBuilder("User", compositeKeyFields, null, null, schema);
            });

    assertTrue(exception.getMessage().contains("Unsupported ID field type"));
    assertTrue(exception.getMessage().contains("tenant_id"));
    assertTrue(exception.getMessage().contains("boolean"));
  }

  @Test
  void testVertexJsonBuilder_CompositeKey_SupportedTypes() throws JsonProcessingException {
    StructType schema =
        new StructType()
            .add("str_id", DataTypes.StringType, false)
            .add("int_id", DataTypes.IntegerType, false)
            .add("long_id", DataTypes.LongType, false)
            .add("short_id", DataTypes.ShortType, false)
            .add("date_id", DataTypes.DateType, false)
            .add("ts_id", DataTypes.TimestampType, false)
            .add("name", DataTypes.StringType, true);

    List<String> compositeKeyFields =
        Arrays.asList("str_id", "int_id", "long_id", "short_id", "date_id", "ts_id");

    VertexJsonBuilder builder =
        new VertexJsonBuilder("TestVertex", compositeKeyFields, null, null, schema);

    Object[] values =
        new Object[] {
          toSparkValue("str_val", DataTypes.StringType),
          toSparkValue(123, DataTypes.IntegerType),
          toSparkValue(456L, DataTypes.LongType),
          toSparkValue((short) 789, DataTypes.ShortType),
          toSparkValue(LocalDate.parse("2023-01-01"), DataTypes.DateType),
          toSparkValue(Instant.parse("2023-01-01T12:00:00Z"), DataTypes.TimestampType),
          toSparkValue("TestName", DataTypes.StringType)
        };
    InternalRow row = new GenericInternalRow(values);
    builder.add(row);

    JsonNode result = builder.build();
    JsonNode vertexNode = result.path("vertices").path("TestVertex");

    String expectedKey =
        "str_val,123,456,789,2023-01-01,2023-01-01 12:00:00.000"; // date as days since epoch,
    // timestamp as microseconds
    assertTrue(vertexNode.has(expectedKey));
    JsonNode vertex = vertexNode.path(expectedKey);
    assertEquals("TestName", vertex.path("name").path("value").asText());
  }

  @Test
  void testEdgeJsonBuilder_CompositeKey() throws JsonProcessingException {
    StructType schema =
        new StructType()
            .add("source_user_id", DataTypes.StringType, false)
            .add("source_tenant_id", DataTypes.StringType, false)
            .add("target_product_id", DataTypes.StringType, false)
            .add("target_category_id", DataTypes.StringType, false)
            .add("weight", DataTypes.DoubleType, true)
            .add("timestamp", DataTypes.TimestampType, true);

    // Create composite key maps for source and target
    List<String> sourceIdFields = Arrays.asList("source_user_id", "source_tenant_id");
    List<String> targetIdFields = Arrays.asList("target_product_id", "target_category_id");

    EdgeJsonBuilder builder =
        new EdgeJsonBuilder(
            "Interacts", "User", sourceIdFields, "Product", targetIdFields, null, null, schema);

    Object[] values1 =
        new Object[] {
          toSparkValue("user123", DataTypes.StringType),
          toSparkValue("tenant456", DataTypes.StringType),
          toSparkValue("product789", DataTypes.StringType),
          toSparkValue("category111", DataTypes.StringType),
          toSparkValue(0.8, DataTypes.DoubleType),
          toSparkValue(Instant.parse("2023-01-15T10:30:45.123Z"), DataTypes.TimestampType)
        };
    InternalRow row1 = new GenericInternalRow(values1);
    builder.add(row1);

    Object[] values2 =
        new Object[] {
          toSparkValue("user999", DataTypes.StringType),
          toSparkValue("tenant456", DataTypes.StringType),
          toSparkValue("product789", DataTypes.StringType),
          toSparkValue("category222", DataTypes.StringType),
          toSparkValue(0.9, DataTypes.DoubleType),
          toSparkValue(Instant.parse("2023-01-16T11:30:00.456Z"), DataTypes.TimestampType)
        };
    InternalRow row2 = new GenericInternalRow(values2);
    builder.add(row2);

    // Expected JSON:
    // {
    //   "edges": {
    //     "User": {
    //       "source_user_id,source_tenant_id": {  // Composite source key
    //         "Interacts": {
    //           "Product": {
    //             "target_product_id,target_category_id": {  // Composite target key
    //               "weight": { "value": 0.8 },
    //               "timestamp": { "value": "2023-01-15 10:30:45.123" }
    //             }
    //           }
    //         }
    //       },
    //       "source_user_id,source_tenant_id": {
    //         "Interacts": {
    //           "Product": {
    //             "target_product_id,target_category_id": {
    //               "weight": { "value": 0.9 },
    //               "timestamp": { "value": "2023-01-16 11:30:00.456" }
    //             }
    //           }
    //         }
    //       }
    //     }
    //   }
    // }
    JsonNode result = builder.build();
    JsonNode edgeNode = result.path("edges").path("User");

    assertTrue(edgeNode.has("user123,tenant456"));
    JsonNode sourceNode1 = edgeNode.path("user123,tenant456");

    assertTrue(sourceNode1.path("Interacts").path("Product").has("product789,category111"));
    JsonNode edge1 = sourceNode1.path("Interacts").path("Product").path("product789,category111");
    assertEquals(0.8, edge1.path("weight").path("value").asDouble(), 0.001);
    assertTrue(edge1.path("timestamp").path("value").asText().startsWith("2023-01-15"));

    assertTrue(edgeNode.has("user999,tenant456"));
    JsonNode sourceNode2 = edgeNode.path("user999,tenant456");
    assertTrue(sourceNode2.path("Interacts").path("Product").has("product789,category222"));
    JsonNode edge2 = sourceNode2.path("Interacts").path("Product").path("product789,category222");
    assertEquals(0.9, edge2.path("weight").path("value").asDouble(), 0.001);
    assertTrue(edge2.path("timestamp").path("value").asText().startsWith("2023-01-16"));
  }

  @Test
  void testEdgeJsonBuilder_CompositeKey_UnsupportedType() {
    StructType schema =
        new StructType()
            .add("source_id", DataTypes.StringType, false)
            .add("target_id", DataTypes.BooleanType, false) // Unsupported type for ID
            .add("weight", DataTypes.DoubleType, true);

    List<String> sourceIdFields = Arrays.asList("source_id");

    List<String> targetIdFields = Arrays.asList("target_id");

    // Should throw exception due to unsupported target ID type
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new EdgeJsonBuilder(
                  "TestEdge",
                  "SourceType",
                  sourceIdFields,
                  "TargetType",
                  targetIdFields,
                  null,
                  null,
                  schema);
            });

    assertTrue(exception.getMessage().contains("Unsupported ID field type"));
    assertTrue(exception.getMessage().contains("target_id"));
    assertTrue(exception.getMessage().contains("boolean"));
  }

  // DISCRIMINATOR TESTS

  private StructType createDiscriminatorEdgeSchema() {
    return new StructType()
        .add("src", DataTypes.StringType, false)
        .add("dst", DataTypes.StringType, false)
        .add("str_origin", DataTypes.StringType, true)
        .add("characters", DataTypes.StringType, true)
        .add("translations", DataTypes.StringType, true)
        .add("result", DataTypes.StringType, true)
        .add("attr", DataTypes.StringType, true);
  }

  @Test
  void testEdgeJsonBuilder_NoDiscriminators() throws JsonProcessingException {
    StructType schema = createDiscriminatorEdgeSchema();
    List<String> sourceIdFields = Arrays.asList("src");
    List<String> targetIdFields = Arrays.asList("dst");
    Set<String> discriminators = new HashSet<>(); // Empty set - no discriminators

    EdgeJsonBuilder builder =
        new EdgeJsonBuilder(
            "translate_edge_multi",
            "translate_vertex",
            sourceIdFields,
            "translate_vertex",
            targetIdFields,
            null,
            null,
            schema,
            discriminators);

    Object[] values =
        new Object[] {
          toSparkValue("v1", DataTypes.StringType),
          toSparkValue("v2", DataTypes.StringType),
          toSparkValue("hello", DataTypes.StringType),
          toSparkValue("chars", DataTypes.StringType),
          toSparkValue("trans", DataTypes.StringType),
          toSparkValue("result1", DataTypes.StringType),
          toSparkValue("extra", DataTypes.StringType)
        };
    InternalRow row = new GenericInternalRow(values);
    builder.add(row);

    JsonNode result = builder.build();

    // Should behave like normal edge (no array)
    JsonNode edgeNode =
        result
            .path("edges")
            .path("translate_vertex")
            .path("v1")
            .path("translate_edge_multi")
            .path("translate_vertex")
            .path("v2");
    assertTrue(edgeNode.isObject());
    assertEquals("hello", edgeNode.path("str_origin").path("value").asText());
    assertEquals("extra", edgeNode.path("attr").path("value").asText());
  }

  @Test
  void testEdgeJsonBuilder_WithDiscriminators_SingleEdge() throws JsonProcessingException {
    StructType schema = createDiscriminatorEdgeSchema();
    List<String> sourceIdFields = Arrays.asList("src");
    List<String> targetIdFields = Arrays.asList("dst");
    Set<String> discriminators =
        new HashSet<>(Arrays.asList("str_origin", "characters", "translations", "result"));

    EdgeJsonBuilder builder =
        new EdgeJsonBuilder(
            "translate_edge_multi",
            "translate_vertex",
            sourceIdFields,
            "translate_vertex",
            targetIdFields,
            null,
            null,
            schema,
            discriminators);

    Object[] values =
        new Object[] {
          toSparkValue("v1", DataTypes.StringType),
          toSparkValue("v2", DataTypes.StringType),
          toSparkValue("hello", DataTypes.StringType),
          toSparkValue("chars", DataTypes.StringType),
          toSparkValue("trans", DataTypes.StringType),
          toSparkValue("result1", DataTypes.StringType),
          toSparkValue("extra", DataTypes.StringType)
        };
    InternalRow row = new GenericInternalRow(values);
    builder.add(row);

    JsonNode result = builder.build();

    // Should create array structure for target ID
    JsonNode targetNode =
        result
            .path("edges")
            .path("translate_vertex")
            .path("v1")
            .path("translate_edge_multi")
            .path("translate_vertex")
            .path("v2");
    assertTrue(targetNode.isArray());
    assertEquals(1, targetNode.size());

    JsonNode edgeAttributes = targetNode.get(0);
    assertEquals("hello", edgeAttributes.path("str_origin").path("value").asText());
    assertEquals("chars", edgeAttributes.path("characters").path("value").asText());
    assertEquals("trans", edgeAttributes.path("translations").path("value").asText());
    assertEquals("result1", edgeAttributes.path("result").path("value").asText());
    assertEquals("extra", edgeAttributes.path("attr").path("value").asText());
  }

  @Test
  void testEdgeJsonBuilder_WithDiscriminators_MultipleEdges_SameTargetId()
      throws JsonProcessingException {
    StructType schema = createDiscriminatorEdgeSchema();
    List<String> sourceIdFields = Arrays.asList("src");
    List<String> targetIdFields = Arrays.asList("dst");
    Set<String> discriminators =
        new HashSet<>(Arrays.asList("str_origin", "characters", "translations", "result"));

    EdgeJsonBuilder builder =
        new EdgeJsonBuilder(
            "translate_edge_multi",
            "translate_vertex",
            sourceIdFields,
            "translate_vertex",
            targetIdFields,
            null,
            null,
            schema,
            discriminators);

    // First edge
    Object[] values1 =
        new Object[] {
          toSparkValue("v1", DataTypes.StringType),
          toSparkValue("v2", DataTypes.StringType),
          toSparkValue("hello", DataTypes.StringType),
          toSparkValue("chars1", DataTypes.StringType),
          toSparkValue("trans1", DataTypes.StringType),
          toSparkValue("result1", DataTypes.StringType),
          toSparkValue("extra1", DataTypes.StringType)
        };
    InternalRow row1 = new GenericInternalRow(values1);
    builder.add(row1);

    // Second edge with same source and target but different discriminator values
    Object[] values2 =
        new Object[] {
          toSparkValue("v1", DataTypes.StringType),
          toSparkValue("v2", DataTypes.StringType),
          toSparkValue("hello", DataTypes.StringType),
          toSparkValue("chars2", DataTypes.StringType), // Different discriminator
          toSparkValue("trans2", DataTypes.StringType), // Different discriminator
          toSparkValue("result2", DataTypes.StringType), // Different discriminator
          toSparkValue("extra2", DataTypes.StringType)
        };
    InternalRow row2 = new GenericInternalRow(values2);
    builder.add(row2);

    JsonNode result = builder.build();

    // Should create array with two elements
    JsonNode targetNode =
        result
            .path("edges")
            .path("translate_vertex")
            .path("v1")
            .path("translate_edge_multi")
            .path("translate_vertex")
            .path("v2");
    assertTrue(targetNode.isArray());
    assertEquals(2, targetNode.size());

    // First edge
    JsonNode edge1 = targetNode.get(0);
    assertEquals("hello", edge1.path("str_origin").path("value").asText());
    assertEquals("chars1", edge1.path("characters").path("value").asText());
    assertEquals("trans1", edge1.path("translations").path("value").asText());
    assertEquals("result1", edge1.path("result").path("value").asText());
    assertEquals("extra1", edge1.path("attr").path("value").asText());

    // Second edge
    JsonNode edge2 = targetNode.get(1);
    assertEquals("hello", edge2.path("str_origin").path("value").asText());
    assertEquals("chars2", edge2.path("characters").path("value").asText());
    assertEquals("trans2", edge2.path("translations").path("value").asText());
    assertEquals("result2", edge2.path("result").path("value").asText());
    assertEquals("extra2", edge2.path("attr").path("value").asText());
  }

  @Test
  void testEdgeJsonBuilder_WithDiscriminators_MissingDiscriminatorValue()
      throws JsonProcessingException {
    StructType schema = createDiscriminatorEdgeSchema();
    List<String> sourceIdFields = Arrays.asList("src");
    List<String> targetIdFields = Arrays.asList("dst");
    Set<String> discriminators =
        new HashSet<>(Arrays.asList("str_origin", "characters", "translations", "result"));

    EdgeJsonBuilder builder =
        new EdgeJsonBuilder(
            "translate_edge_multi",
            "translate_vertex",
            sourceIdFields,
            "translate_vertex",
            targetIdFields,
            null,
            null,
            schema,
            discriminators);

    // Row with null discriminator value
    Object[] values =
        new Object[] {
          toSparkValue("v1", DataTypes.StringType),
          toSparkValue("v2", DataTypes.StringType),
          toSparkValue("hello", DataTypes.StringType),
          null, // Missing discriminator value
          toSparkValue("trans", DataTypes.StringType),
          toSparkValue("result1", DataTypes.StringType),
          toSparkValue("extra", DataTypes.StringType)
        };
    InternalRow row = new GenericInternalRow(values);
    builder.add(row);

    JsonNode result = builder.build();

    // Should be empty since the row was skipped due to missing discriminator
    JsonNode edgeNode =
        result
            .path("edges")
            .path("translate_vertex")
            .path("v1")
            .path("translate_edge_multi")
            .path("translate_vertex")
            .path("v2");
    assertTrue(edgeNode.isEmpty());
  }
}
