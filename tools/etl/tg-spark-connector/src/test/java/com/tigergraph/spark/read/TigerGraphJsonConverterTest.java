package com.tigergraph.spark.read;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.types.StructType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tigergraph.spark.util.Options.QueryType;

public class TigerGraphJsonConverterTest {
  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testAllDataTypes() {
    StructType schema =
        StructType.fromDDL(
            new StringBuilder()
                .append("c0 BOOLEAN")
                .append(",c1 BYTE")
                .append(",c2 SHORT")
                .append(",c3 INT")
                .append(",c4 DATE")
                .append(",c5 INTERVAL YEAR TO MONTH")
                .append(",c6 LONG")
                .append(",c7 TIMESTAMP")
                .append(",c8 INTERVAL HOUR TO MINUTE")
                .append(",c9 FLOAT")
                .append(",c10 DOUBLE")
                .append(",c11 DECIMAL(38, 0)")
                .append(",c12 STRING")
                .append(",c13 BINARY")
                .toString());
    ObjectNode json = mapper.createObjectNode();
    json.put("c0", true)
        .put("c1", 97)
        .put("c2", 32767)
        .put("c3", "2147483647")
        .put("c4", 123)
        .put("c5", "123")
        .put("c6", 9223372036854775807L)
        .put("c7", 1e7)
        .put("c8", 1e7)
        .put("c9", 3.402E5)
        .put("c10", 1.7976931348623157E308)
        .put("c11", "99999999999999999999999999999999999999")
        .put("c12", "TigerGraph")
        .put("c13", "Bytes 84 105 103 101 114 71 114 97 112 104");

    TigerGraphResultAccessor accessor = TigerGraphResultAccessor.fromExternalSchema(schema, null);

    TigerGraphJsonConverter converter = new TigerGraphJsonConverter(accessor, QueryType.INSTALLED);
    InternalRow row = converter.convert(json);
    assertEquals(true, row.getBoolean(0));
    assertEquals('a', (char) row.getByte(1));
    assertEquals(32767, row.getShort(2));
    assertEquals(2147483647, row.getInt(3));
    assertEquals(123, row.getInt(4));
    assertEquals(123, row.getInt(5));
    assertEquals(9223372036854775807L, row.getLong(6));
    assertEquals(1e7, row.getLong(7));
    assertEquals(1e7, row.getLong(8));
    assertEquals(3.402E5, row.getFloat(9));
    assertEquals(1.7976931348623157E308, row.getDouble(10));
    assertEquals("99999999999999999999999999999999999999", row.getDecimal(11, 38, 0).toString());
    assertEquals("TigerGraph", row.getString(12));
    assertEquals("TigerGraph", new String(row.getBinary(13)));
  }

  @Test
  public void testUnsupportedTypes() {
    String[] testCases = {
      "c0 ARRAY<STRING>", "c0 MAP<STRING, INT>", "c0 STRUCT<name: STRING>", "c0 INTERVAL"
    };
    for (String testCase : testCases) {
      StructType schema = StructType.fromDDL(testCase);
      TigerGraphResultAccessor accessor = TigerGraphResultAccessor.fromExternalSchema(schema, null);
      assertThrows(
          UnsupportedOperationException.class,
          () -> new TigerGraphJsonConverter(accessor, QueryType.GET_VERTEX));
    }
  }

  @Test
  public void testMissingFieldsInData() throws JsonMappingException, JsonProcessingException {
    // The schema has 'v_id', 'age' and 'name', while the JSON only has 'name'
    StructType schema = StructType.fromDDL("v_id INT, age INT, name STRING");
    TigerGraphResultAccessor accessor = TigerGraphResultAccessor.fromExternalSchema(schema, null);
    JsonNode node = mapper.readTree("{\"attributes\": {\"name\": \"Tom\"}}");
    TigerGraphJsonConverter converter =
        new TigerGraphJsonConverter(accessor, QueryType.GET_VERTICES);
    assertEquals("[null,null,Tom]", converter.convert(node).toString());
  }

  @Test
  public void testMissingFieldsInSchema() throws JsonMappingException, JsonProcessingException {
    // The schema has 'v_id' and 'name', while the JSON has 'v_id', 'age' and 'name'
    StructType schema = StructType.fromDDL("v_id INT, name STRING");
    TigerGraphResultAccessor accessor = TigerGraphResultAccessor.fromExternalSchema(schema, null);
    JsonNode node =
        mapper.readTree("{\"v_id\": 1, \"attributes\": {\"age\": 10, \"name\": \"Tom\"}}");
    TigerGraphJsonConverter converter =
        new TigerGraphJsonConverter(accessor, QueryType.GET_VERTICES);
    assertEquals("[1,Tom]", converter.convert(node).toString());
  }

  @Test
  public void testParsingEdgeFields() throws JsonMappingException, JsonProcessingException {
    // Flattened attributes
    StructType schema =
        StructType.fromDDL(
            "e_type STRING, from_type STRING, from_id INT, to_type STRING, to_id INT, meta STRING");
    TigerGraphResultAccessor accessor = TigerGraphResultAccessor.fromExternalSchema(schema, null);

    JsonNode node =
        mapper.readTree(
            "{\"e_type\": \"Posts\", \"from_id\": \"3\", \"from_type\": \"Person\", \"to_id\":"
                + " \"999\", \"to_type\": \"Post\", \"attributes\": {\"meta\": \"abcd\"}}");
    TigerGraphJsonConverter converter =
        new TigerGraphJsonConverter(accessor, QueryType.GET_EDGES_BY_SRC_VERTEX_EDGE_TYPE_TGT_TYPE);
    assertEquals("[Posts,Person,3,Post,999,abcd]", converter.convert(node).toString());

    // Unflattened attributes
    schema =
        StructType.fromDDL(
            "e_type STRING, from_type STRING, from_id INT, to_type STRING, to_id INT, attributes"
                + " STRING");
    accessor = TigerGraphResultAccessor.fromExternalSchema(schema, null);

    node =
        mapper.readTree(
            "{\"e_type\": \"Posts\", \"from_id\": \"3\", \"from_type\": \"Person\", \"to_id\":"
                + " \"999\", \"to_type\": \"Post\", \"attributes\": {\"meta\": \"abcd\"}}");
    converter = new TigerGraphJsonConverter(accessor, QueryType.GET_EDGES_BY_SRC_VERTEX);
    assertEquals(
        "[Posts,Person,3,Post,999,{\"meta\":\"abcd\"}]", converter.convert(node).toString());
  }

  @Test
  public void testPointer() throws JsonMappingException, JsonProcessingException {
    System.out.println(extractFirstLevelTypes("MapAccum<vertex, ListAccum<vertex>>"));
  }

  public static List<String> extractFirstLevelTypes(String input) {
    List<String> types = new ArrayList<>();
    Stack<Character> stk = new Stack<>();
    StringBuilder currentType = new StringBuilder();

    for (char c : input.toCharArray()) {
      if (c == '<') {
        stk.push(c);
      }
    }

    // Add the last type
    types.add(currentType.toString().trim());

    return types;
  }
}
