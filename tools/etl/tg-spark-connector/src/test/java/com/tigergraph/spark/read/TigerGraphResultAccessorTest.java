package com.tigergraph.spark.read;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tigergraph.spark.read.TigerGraphResultAccessor.FieldMeta;
import java.util.HashSet;
import java.util.Set;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.Test;

public class TigerGraphResultAccessorTest {

  static final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testMapTGTypeToSparkType() {
    String[] inputs = {
      null,
      "",
      "int",
      "UiNt",
      "floaT",
      "Double",
      "BOOL",
      "fixed_Binary",
      "DATETIME",
      "STRING",
      "SET<INT"
    };
    DataType[] expected = {
      DataTypes.StringType,
      DataTypes.StringType,
      DataTypes.createDecimalType(38, 0),
      DataTypes.createDecimalType(38, 0),
      DataTypes.FloatType,
      DataTypes.DoubleType,
      DataTypes.BooleanType,
      DataTypes.BinaryType,
      DataTypes.StringType,
      DataTypes.StringType,
      DataTypes.StringType
    };
    for (int i = 0; i < inputs.length; i++) {
      assertEquals(expected[i], TigerGraphResultAccessor.mapTGTypeToSparkType(inputs[i]));
    }
  }

  @Test
  public void testParseVertexSchemaBasic() throws Exception {
    JsonNode input =
        objectMapper.readTree(
            "{\"PrimaryId\":{\"AttributeType\":{\"Name\":\"INT\"}},\"Attributes\":[{\"AttributeType\":{\"Name\":\"STRING\"},\"AttributeName\":\"_c0\"},{\"AttributeType\":{\"Name\":\"FLOAT\"},\"AttributeName\":\"_c1\"},{\"AttributeType\":{\"Name\":\"SET<DOUBLE>\"},\"AttributeName\":\"_c2\"}]}");
    StructType schema = TigerGraphResultAccessor.fromVertexMeta(input, null).getSchema();
    assertEquals(
        schema.catalogString(), "struct<v_id:decimal(38,0),_c0:string,_c1:float,_c2:string>");
  }

  @Test
  public void testParseVertexSchemaWithColumnProne() throws Exception {
    JsonNode input =
        objectMapper.readTree(
            "{\"PrimaryId\":{\"AttributeType\":{\"Name\":\"INT\"}},\"Attributes\":[{\"AttributeType\":{\"Name\":\"STRING\"},\"AttributeName\":\"_c0\"},{\"AttributeType\":{\"Name\":\"FLOAT\"},\"AttributeName\":\"_c1\"},{\"AttributeType\":{\"Name\":\"SET<DOUBLE>\"},\"AttributeName\":\"_c2\"}]}");
    // 1.
    Set<String> columnProne1 = new HashSet<>();
    columnProne1.add("_c0");
    columnProne1.add("_c2");
    columnProne1.add("_c3"); // non-exist
    TigerGraphResultAccessor accessor1 =
        TigerGraphResultAccessor.fromVertexMeta(input, columnProne1);
    assertEquals(
        accessor1.getSchema().catalogString(), "struct<v_id:decimal(38,0),_c0:string,_c2:string>");
    // 2. empty select
    Set<String> columnProne2 = new HashSet<>();
    TigerGraphResultAccessor accessor2 =
        TigerGraphResultAccessor.fromVertexMeta(input, columnProne2);
    assertEquals(accessor2.getSchema().catalogString(), "struct<v_id:decimal(38,0)>");
  }

  @Test
  public void testParseVertexSchemaWithoutAttrs() throws Exception {
    JsonNode input =
        objectMapper.readTree(
            "{\"PrimaryId\":{\"AttributeType\":{\"Name\":\"INT\"}},\"Attributes\":[]}");
    StructType out = TigerGraphResultAccessor.fromVertexMeta(input, null).getSchema();
    assertEquals(out.catalogString(), "struct<v_id:decimal(38,0)>");
  }

  @Test
  public void testParseEdgeSchemaBasic() throws Exception {
    JsonNode input =
        objectMapper.readTree(
            "{\"Attributes\":[{\"AttributeType\":{\"Name\":\"BOOL\"},\"AttributeName\":\"_c0\"},{\"AttributeType\":{\"Name\":\"DOUBLE\"},\"AttributeName\":\"_c1\"},{\"AttributeType\":{\"Name\":\"FIXED_BINARY\"},\"AttributeName\":\"_c2\"}]}");
    StructType out = TigerGraphResultAccessor.fromEdgeMeta(input, null).getSchema();
    assertEquals(
        out.catalogString(),
        "struct<from_type:string,from_id:string,to_type:string,to_id:string,_c0:boolean,_c1:double,_c2:binary>");
  }

  @Test
  public void testParseEdgeSchemaWithColumnProne() throws Exception {
    JsonNode input =
        objectMapper.readTree(
            "{\"Attributes\":[{\"AttributeType\":{\"Name\":\"BOOL\"},\"AttributeName\":\"_c0\"},{\"AttributeType\":{\"Name\":\"DOUBLE\"},\"AttributeName\":\"_c1\"},{\"AttributeType\":{\"Name\":\"FIXED_BINARY\"},\"AttributeName\":\"_c2\"}]}");
    // 1.
    Set<String> columnProne1 = new HashSet<>();
    columnProne1.add("_c0");
    columnProne1.add("_c2");
    columnProne1.add("_c3"); // non-exist
    StructType out1 = TigerGraphResultAccessor.fromEdgeMeta(input, columnProne1).getSchema();
    assertEquals(
        out1.catalogString(),
        "struct<from_type:string,from_id:string,to_type:string,to_id:string,_c0:boolean,_c2:binary>");
    // 2. empty select
    Set<String> columnProne2 = new HashSet<>();
    StructType out2 = TigerGraphResultAccessor.fromEdgeMeta(input, columnProne2).getSchema();
    assertEquals(
        out2.catalogString(),
        "struct<from_type:string,from_id:string,to_type:string,to_id:string>");
  }

  @Test
  public void testParseEdgeSchemaWithoutAttrs() throws Exception {
    JsonNode input = objectMapper.readTree("{}");
    StructType out = TigerGraphResultAccessor.fromEdgeMeta(input, null).getSchema();
    assertEquals(
        out.catalogString(), "struct<from_type:string,from_id:string,to_type:string,to_id:string>");
  }

  @Test
  public void testParseQuerySchemaBasic() throws Exception {
    JsonNode schema =
        objectMapper.readTree(
            "[{\"a\":\"string\",\"b\":\"int\",\"c\":\"bool\",\"d\":\"float\",\"e\":\"vertex<Person>\",\"f\":\"SetAccum<string>\"}]");
    StructType out = TigerGraphResultAccessor.fromQueryMeta(schema, null).getSchema();
    assertEquals(
        out.catalogString(),
        "struct<a:string,b:decimal(38,0),c:boolean,d:float,e:string,f:string>");
  }

  @Test
  public void testParseQuerySchemaWithMultipleSimilarSchema() throws Exception {
    // similar but different order
    JsonNode schema =
        objectMapper.readTree(
            "[{\"a\":\"string\",\"b\":\"int\",\"c\":\"bool\",\"d\":\"float\",\"e\":\"vertex<Person>\",\"f\":\"SetAccum<string>\"},"
                + "{\"b\":\"int\",\"c\":\"bool\",\"d\":\"float\",\"e\":\"vertex<Person>\",\"f\":\"SetAccum<string>\",\"a\":\"string\"}]");
    StructType out = TigerGraphResultAccessor.fromQueryMeta(schema, null).getSchema();
    assertEquals(
        out.catalogString(),
        "struct<a:string,b:decimal(38,0),c:boolean,d:float,e:string,f:string>");
  }

  @Test
  public void testParseQuerySchemaWithMultipleDifferentSchema() throws Exception {
    // similar but different order
    JsonNode schema =
        objectMapper.readTree(
            "[{\"a\":\"string\",\"b\":\"int\",\"c\":\"bool\",\"d\":\"float\",\"e\":\"vertex<Person>\",\"f\":\"SetAccum<string>\"},"
                + "{\"b\":\"int\",\"c\":\"bool\",\"d\":\"float\",\"e\":\"vertex<Person>\",\"f\":\"SetAccum<string>\"}]");
    StructType out = TigerGraphResultAccessor.fromQueryMeta(schema, null).getSchema();
    assertEquals(out.catalogString(), "struct<results:string>");
  }

  @Test
  public void testParseQuerySchemaWithEmptySchema() throws Exception {
    StructType out =
        TigerGraphResultAccessor.fromQueryMeta(objectMapper.readTree("[]"), null).getSchema();
    assertEquals(out.catalogString(), "struct<results:string>");
  }

  @Test
  public void testNormalAccessor() throws Exception {
    // column name | JSON path | isQueryable | original JSON obj | expected result
    Object[][] testCases = {
      {"name", "/attr/id", false, "{\"a\":456,\"attr\":{\"id\":123}}", "123"}, // basic
      {"name", "", false, "{\"a\":456,\"b\":123}", "{\"a\":456,\"b\":123}"}, // read the entire obj
      {
        "name", "/known", true, "{\"a\":456,\"attr\":{\"name\":123}}", "123"
      }, // can't find by path, search recursively by col name
    };
    for (int i = 0; i < testCases.length; i++) {
      FieldMeta meta =
          FieldMeta.fromValue(
              (String) testCases[i][0], (String) testCases[i][1], (Boolean) testCases[i][2]);
      JsonNode res = meta.toAccessor().apply(objectMapper.readTree((String) testCases[i][3]));
      assertEquals((String) testCases[i][4], res.toString());
    }
  }
}
