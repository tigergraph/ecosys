package com.tigergraph.spark.write.upsert;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.expressions.GenericInternalRow;
import org.apache.spark.sql.catalyst.util.GenericArrayData;
import org.apache.spark.sql.catalyst.util.ArrayBasedMapData;
import org.apache.spark.sql.types.*;
import org.apache.spark.unsafe.types.UTF8String;
import org.junit.jupiter.api.Test;

public class AttributeMapperTest {
  @Test
  public void testPrimitiveTypes() {
    StructType schema =
        new StructType()
            .add("str_col", DataTypes.StringType)
            .add("int_col", DataTypes.IntegerType)
            .add("long_col", DataTypes.LongType)
            .add("bool_col", DataTypes.BooleanType)
            .add("float_col", DataTypes.FloatType)
            .add("double_col", DataTypes.DoubleType)
            .add("byte_col", DataTypes.ByteType)
            .add("short_col", DataTypes.ShortType)
            .add("decimal_col", DataTypes.createDecimalType(10, 2))
            .add("date_col", DataTypes.DateType)
            .add("timestamp_col", DataTypes.TimestampType);

    Object[] values =
        new Object[] {
          UTF8String.fromString("hello"), // str_col
          42, // int_col
          1234567890123L, // long_col
          true, // bool_col
          3.14f, // float_col
          2.718, // double_col
          (byte) 7, // byte_col
          (short) 8, // short_col
          Decimal.apply(new BigDecimal("12345.67"), 10, 2), // decimal_col
          3, // date_col (days since epoch)
          // timestamp_col (microseconds): 2024-06-12 15:30:45.123 UTC
          // Spark expects microseconds since epoch, so:
          // 2024-06-12 15:30:45.123 UTC = epoch seconds 1718206245, nanos 123000000
          // microseconds = 1718206245_123000
          1718206245123000L
        };
    InternalRow row = new GenericInternalRow(values);

    for (int i = 0; i < schema.fields().length; i++) {
      StructField field = schema.fields()[i];
      AttributeMapper mapping = new AttributeMapper(i, field, field.name(), null);
      ObjectNode attrNode = mapping.getAttributeValue(row);
      assertNotNull(attrNode, "Attribute node should not be null for " + field.name());
      JsonNode valueNode = attrNode.get("value");
      assertNotNull(valueNode, "Value node should not be null for " + field.name());
      switch (field.name()) {
        case "str_col":
          assertTrue(valueNode.isTextual(), "str_col should be textual");
          assertEquals("hello", valueNode.textValue());
          break;
        case "int_col":
          assertTrue(valueNode.isInt() || valueNode.isLong(), "int_col should be numeric");
          assertEquals(42, valueNode.intValue());
          break;
        case "long_col":
          assertTrue(valueNode.isLong() || valueNode.isInt(), "long_col should be numeric");
          assertEquals(1234567890123L, valueNode.longValue());
          break;
        case "bool_col":
          assertTrue(valueNode.isBoolean(), "bool_col should be boolean");
          assertTrue(valueNode.booleanValue());
          break;
        case "float_col":
          assertTrue(
              valueNode.isFloat() || valueNode.isDouble() || valueNode.isNumber(),
              "float_col should be numeric");
          assertEquals(3.14f, (float) valueNode.doubleValue(), 0.0001);
          break;
        case "double_col":
          assertTrue(
              valueNode.isDouble() || valueNode.isFloat() || valueNode.isNumber(),
              "double_col should be numeric");
          assertEquals(2.718, valueNode.doubleValue(), 0.0001);
          break;
        case "byte_col":
          assertTrue(valueNode.isInt() || valueNode.isLong(), "byte_col should be numeric");
          assertEquals(7, valueNode.intValue());
          break;
        case "short_col":
          assertTrue(valueNode.isInt() || valueNode.isLong(), "short_col should be numeric");
          assertEquals(8, valueNode.intValue());
          break;
        case "decimal_col":
          assertTrue(valueNode.isNumber(), "decimal_col should be numeric");
          assertEquals(12345.67, valueNode.doubleValue(), 0.01);
          break;
        case "date_col":
          assertTrue(valueNode.isTextual(), "date_col should be textual");
          assertEquals("1970-01-04", valueNode.textValue());
          break;
        case "timestamp_col":
          assertTrue(valueNode.isTextual(), "timestamp_col should be textual");
          assertEquals("2024-06-12 15:30:45.123", valueNode.textValue());
          break;
        default:
          fail("Unexpected field: " + field.name());
      }
    }
  }

  @Test
  public void testArrayTypes() {
    StructType schema =
        new StructType()
            .add("int_array", DataTypes.createArrayType(DataTypes.IntegerType))
            .add("str_array", DataTypes.createArrayType(DataTypes.StringType))
            .add("double_array", DataTypes.createArrayType(DataTypes.DoubleType))
            .add("date_array", DataTypes.createArrayType(DataTypes.DateType))
            .add("decimal_array", DataTypes.createArrayType(DataTypes.createDecimalType(10, 2)));

    Object[] values =
        new Object[] {
          new GenericArrayData(new int[] {1, 2, 3}),
          new GenericArrayData(
              new UTF8String[] {UTF8String.fromString("a"), UTF8String.fromString("b")}),
          new GenericArrayData(new double[] {1.1, 2.2}),
          new GenericArrayData(
              new int[] {3, 4}), // date_array: days since epoch (1970-01-04, 1970-01-05)
          new GenericArrayData(
              new Object[] {
                Decimal.apply(new BigDecimal("123.45"), 10, 2),
                Decimal.apply(new BigDecimal("67.89"), 10, 2)
              })
        };
    InternalRow row = new GenericInternalRow(values);

    for (int i = 0; i < schema.fields().length; i++) {
      StructField field = schema.fields()[i];
      AttributeMapper mapping = new AttributeMapper(i, field, field.name(), null);
      ObjectNode attrNode = mapping.getAttributeValue(row);
      assertNotNull(attrNode, "Attribute node should not be null for " + field.name());
      JsonNode valueNode = attrNode.get("value");
      assertNotNull(valueNode, "Value node should not be null for " + field.name());
      String valueJson = valueNode.toString();
      switch (field.name()) {
        case "int_array":
          assertEquals("[1,2,3]", valueJson);
          break;
        case "str_array":
          assertEquals("[\"a\",\"b\"]", valueJson);
          break;
        case "double_array":
          assertEquals("[1.1,2.2]", valueJson);
          break;
        case "date_array":
          // Should be array of date strings
          assertEquals("[\"1970-01-04\",\"1970-01-05\"]", valueJson);
          break;
        case "decimal_array":
          // Should be array of numbers
          assertEquals("[123.45,67.89]", valueJson);
          break;
        default:
          fail("Unexpected field: " + field.name());
      }
    }
  }

  @Test
  public void testStructType() {
    // Struct with primitive fields only
    StructType personStruct =
        new StructType()
            .add("name", DataTypes.StringType)
            .add("age", DataTypes.IntegerType)
            .add("height", DataTypes.DoubleType);

    StructType schema = new StructType().add("person", personStruct);

    // Create struct row
    InternalRow personRow =
        new GenericInternalRow(new Object[] {UTF8String.fromString("John"), 30, 175.5});

    Object[] values = new Object[] {personRow};
    InternalRow row = new GenericInternalRow(values);

    StructField field = schema.fields()[0];
    AttributeMapper mapping = new AttributeMapper(0, field, field.name(), null);
    ObjectNode attrNode = mapping.getAttributeValue(row);

    assertNotNull(attrNode, "Attribute node should not be null");
    JsonNode valueNode = attrNode.get("value");
    assertNotNull(valueNode, "Value node should not be null");

    assertTrue(valueNode.has("keylist"), "Should have keylist");
    assertTrue(valueNode.has("valuelist"), "Should have valuelist");

    JsonNode keylist = valueNode.get("keylist");
    JsonNode valuelist = valueNode.get("valuelist");

    assertEquals(3, keylist.size(), "Should have 3 keys");
    assertEquals(3, valuelist.size(), "Should have 3 values");

    assertEquals("name", keylist.get(0).asText());
    assertEquals("age", keylist.get(1).asText());
    assertEquals("height", keylist.get(2).asText());

    assertEquals("John", valuelist.get(0).asText());
    assertEquals(30, valuelist.get(1).asInt());
    assertEquals(175.5, valuelist.get(2).asDouble(), 0.001);
  }

  @Test
  public void testMapType() {
    // Map with string keys and integer values
    MapType mapType = DataTypes.createMapType(DataTypes.StringType, DataTypes.IntegerType);
    StructType schema = new StructType().add("scores", mapType);

    // Create map data
    Object[] keys = new Object[] {UTF8String.fromString("math"), UTF8String.fromString("science")};
    Object[] values = new Object[] {95, 87};
    ArrayBasedMapData mapData =
        new ArrayBasedMapData(new GenericArrayData(keys), new GenericArrayData(values));

    Object[] rowValues = new Object[] {mapData};
    InternalRow row = new GenericInternalRow(rowValues);

    StructField field = schema.fields()[0];
    AttributeMapper mapping = new AttributeMapper(0, field, field.name(), null);
    ObjectNode attrNode = mapping.getAttributeValue(row);

    assertNotNull(attrNode, "Attribute node should not be null");
    JsonNode valueNode = attrNode.get("value");
    assertNotNull(valueNode, "Value node should not be null");

    assertTrue(valueNode.has("keylist"), "Should have keylist");
    assertTrue(valueNode.has("valuelist"), "Should have valuelist");

    JsonNode keylist = valueNode.get("keylist");
    JsonNode valuelist = valueNode.get("valuelist");

    assertEquals(2, keylist.size(), "Should have 2 keys");
    assertEquals(2, valuelist.size(), "Should have 2 values");

    assertEquals("math", keylist.get(0).asText());
    assertEquals("science", keylist.get(1).asText());
    assertEquals(95, valuelist.get(0).asInt());
    assertEquals(87, valuelist.get(1).asInt());
  }

  @Test
  public void testArrayOfStruct() {
    // Array of structs
    StructType personStruct =
        new StructType().add("name", DataTypes.StringType).add("age", DataTypes.IntegerType);

    ArrayType arrayType = DataTypes.createArrayType(personStruct);
    StructType schema = new StructType().add("people", arrayType);

    // Create struct elements for array
    InternalRow person1 = new GenericInternalRow(new Object[] {UTF8String.fromString("Alice"), 25});
    InternalRow person2 = new GenericInternalRow(new Object[] {UTF8String.fromString("Bob"), 30});

    Object[] arrayValues = new Object[] {person1, person2};
    GenericArrayData arrayData = new GenericArrayData(arrayValues);

    Object[] rowValues = new Object[] {arrayData};
    InternalRow row = new GenericInternalRow(rowValues);

    StructField field = schema.fields()[0];
    AttributeMapper mapping = new AttributeMapper(0, field, field.name(), null);
    ObjectNode attrNode = mapping.getAttributeValue(row);

    assertNotNull(attrNode, "Attribute node should not be null");
    JsonNode valueNode = attrNode.get("value");
    assertNotNull(valueNode, "Value node should not be null");

    assertTrue(valueNode.isArray(), "Should be an array");
    assertEquals(2, valueNode.size(), "Should have 2 elements");

    // Check array elements - they should be struct objects with keylist/valuelist
    JsonNode elem1 = valueNode.get(0);
    JsonNode elem2 = valueNode.get(1);

    assertTrue(
        elem1.has("keylist") && elem1.has("valuelist"),
        "First element should have keylist and valuelist");
    assertTrue(
        elem2.has("keylist") && elem2.has("valuelist"),
        "Second element should have keylist and valuelist");

    assertEquals("Alice", elem1.get("valuelist").get(0).asText());
    assertEquals(25, elem1.get("valuelist").get(1).asInt());
    assertEquals("Bob", elem2.get("valuelist").get(0).asText());
    assertEquals(30, elem2.get("valuelist").get(1).asInt());
  }

  @Test
  public void testArrayOfMap() {
    // Array of maps
    MapType mapType = DataTypes.createMapType(DataTypes.StringType, DataTypes.IntegerType);
    ArrayType arrayType = DataTypes.createArrayType(mapType);
    StructType schema = new StructType().add("scores_list", arrayType);

    // Create map elements for array
    Object[] keys1 = new Object[] {UTF8String.fromString("math")};
    Object[] values1 = new Object[] {95};
    ArrayBasedMapData map1 =
        new ArrayBasedMapData(new GenericArrayData(keys1), new GenericArrayData(values1));

    Object[] keys2 = new Object[] {UTF8String.fromString("science")};
    Object[] values2 = new Object[] {87};
    ArrayBasedMapData map2 =
        new ArrayBasedMapData(new GenericArrayData(keys2), new GenericArrayData(values2));

    Object[] arrayValues = new Object[] {map1, map2};
    GenericArrayData arrayData = new GenericArrayData(arrayValues);

    Object[] rowValues = new Object[] {arrayData};
    InternalRow row = new GenericInternalRow(rowValues);

    StructField field = schema.fields()[0];
    AttributeMapper mapping = new AttributeMapper(0, field, field.name(), null);
    ObjectNode attrNode = mapping.getAttributeValue(row);

    assertNotNull(attrNode, "Attribute node should not be null");
    JsonNode valueNode = attrNode.get("value");
    assertNotNull(valueNode, "Value node should not be null");

    assertTrue(valueNode.isArray(), "Should be an array");
    assertEquals(2, valueNode.size(), "Should have 2 elements");

    // Check array elements - they should be map objects with keylist/valuelist
    JsonNode elem1 = valueNode.get(0);
    JsonNode elem2 = valueNode.get(1);

    assertTrue(
        elem1.has("keylist") && elem1.has("valuelist"),
        "First element should have keylist and valuelist");
    assertTrue(
        elem2.has("keylist") && elem2.has("valuelist"),
        "Second element should have keylist and valuelist");

    assertEquals("math", elem1.get("keylist").get(0).asText());
    assertEquals(95, elem1.get("valuelist").get(0).asInt());
    assertEquals("science", elem2.get("keylist").get(0).asText());
    assertEquals(87, elem2.get("valuelist").get(0).asInt());
  }

  @Test
  public void testInvalidStructWithNonPrimitiveFields() {
    // Struct with nested struct (should fail)
    StructType nestedStruct = new StructType().add("nested", DataTypes.StringType);
    StructType invalidStruct =
        new StructType()
            .add("name", DataTypes.StringType)
            .add("nested_field", nestedStruct); // This should cause validation to fail

    StructType schema = new StructType().add("invalid_struct", invalidStruct);

    Object[] rowValues = new Object[] {null};
    InternalRow row = new GenericInternalRow(rowValues);
    StructField field = schema.fields()[0];

    Exception exception =
        assertThrows(
            RuntimeException.class,
            () -> {
              new AttributeMapper(0, field, field.name(), null);
            });

    assertTrue(
        exception.getCause().getMessage().contains("struct fields can only be primitive types"));
  }

  @Test
  public void testInvalidMapWithNonPrimitiveKey() {
    // Map with non-primitive key type (should fail)
    StructType structKey = new StructType().add("id", DataTypes.IntegerType);
    MapType invalidMapType = DataTypes.createMapType(structKey, DataTypes.StringType);
    StructType schema = new StructType().add("invalid_map", invalidMapType);

    Object[] rowValues = new Object[] {null};
    InternalRow row = new GenericInternalRow(rowValues);
    StructField field = schema.fields()[0];

    Exception exception =
        assertThrows(
            RuntimeException.class,
            () -> {
              new AttributeMapper(0, field, field.name(), null);
            });

    assertTrue(exception.getCause().getMessage().contains("map keys can only be primitive types"));
  }
}
