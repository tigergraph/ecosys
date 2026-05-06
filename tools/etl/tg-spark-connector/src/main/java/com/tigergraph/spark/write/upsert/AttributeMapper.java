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
package com.tigergraph.spark.write.upsert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Function;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.util.ArrayData;
import org.apache.spark.sql.catalyst.util.MapData;
import org.apache.spark.sql.types.ArrayType;
import org.apache.spark.sql.types.BooleanType;
import org.apache.spark.sql.types.ByteType;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DateType;
import org.apache.spark.sql.types.Decimal;
import org.apache.spark.sql.types.DecimalType;
import org.apache.spark.sql.types.DoubleType;
import org.apache.spark.sql.types.FloatType;
import org.apache.spark.sql.types.IntegerType;
import org.apache.spark.sql.types.LongType;
import org.apache.spark.sql.types.MapType;
import org.apache.spark.sql.types.ShortType;
import org.apache.spark.sql.types.StringType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.types.TimestampNTZType;
import org.apache.spark.sql.types.TimestampType;

/**
 * The {@code AttributeMapper} class is responsible for mapping a single field from a Spark {@link
 * InternalRow} to a TigerGraph-compatible JSON attribute format. It handles the conversion of
 * various Spark SQL data types into the corresponding JSON representation that TigerGraph's upsert
 * endpoints expect.
 *
 * <p>This class supports primitive types, as well as complex types like Array, Map, and Struct (for
 * UDTs), ensuring they are correctly formatted for TigerGraph.
 *
 * <p>Key responsibilities include:
 *
 * <ul>
 *   <li>Creating a type-specific converter function for a given Spark data type.
 *   <li>Validating that complex types (Map, Struct) adhere to TigerGraph's constraints.
 *   <li>Converting the Spark internal representation into a {@link JsonNode}.
 *   <li>Wrapping the final JSON value in a TigerGraph attribute object, which includes the value
 *       and an optional operation (e.g., for attribute accumulation).
 * </ul>
 */
public class AttributeMapper {
  private final int fieldIndex;
  private final String fieldName;
  private final DataType fieldType;
  private final String attributeName;
  private final String operation;
  private final Function<InternalRow, JsonNode> converter;
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final long MILLIS_IN_A_DAY = 86400000L;

  /**
   * Constructs an {@code AttributeMapper} for a specific field in a Spark schema.
   *
   * @param fieldIndex The index of the field in the Spark {@link InternalRow}.
   * @param field The {@link StructField} containing schema information like name and data type.
   * @param attributeName The name of the target attribute in the TigerGraph schema.
   * @param operation The operation to be performed on the attribute (e.g., "sum", "max"). Can be
   *     null if no operation is specified.
   * @throws RuntimeException if a converter cannot be created for the specified field type.
   */
  public AttributeMapper(
      int fieldIndex, StructField field, String attributeName, String operation) {
    this.fieldIndex = fieldIndex;
    this.fieldName = field.name();
    this.fieldType = field.dataType();
    this.attributeName = attributeName;
    this.operation = operation;
    try {
      this.converter = createConverter(fieldIndex, fieldType);
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to create converter for field '"
              + fieldName
              + "', type: "
              + fieldType.simpleString(),
          e);
    }
  }

  /**
   * Processes a Spark {@link InternalRow} to generate the JSON attribute object for the configured
   * field.
   *
   * <p>If the field's value is null in the row, this method returns null. Otherwise, it converts
   * the value to a {@link JsonNode} and wraps it in an {@link ObjectNode} with the following
   * structure:
   *
   * <pre>{@code
   * {
   *   "value": <converted_value>,
   *   "op": "<operation>" // Optional
   * }
   * }</pre>
   *
   * @param row The Spark {@link InternalRow} to process.
   * @return An {@link ObjectNode} representing the TigerGraph attribute, or null if the field value
   *     is null.
   * @throws RuntimeException if value conversion fails for the given row.
   */
  ObjectNode getAttributeValue(InternalRow row) {
    if (row.isNullAt(fieldIndex)) {
      return null;
    }
    JsonNode valueNode;
    try {
      valueNode = converter.apply(row);
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to convert value for field '"
              + fieldName
              + "', type: "
              + fieldType.simpleString(),
          e);
    }
    if (valueNode == null) {
      return null;
    }
    ObjectNode attrNode = mapper.createObjectNode();
    attrNode.set("value", valueNode);
    if (operation != null) {
      attrNode.put("op", operation);
    }
    return attrNode;
  }

  // Getters

  /**
   * Gets the field index in the Spark row.
   *
   * @return the field index
   */
  public int getFieldIndex() {
    return fieldIndex;
  }

  /**
   * Gets the name of the field from the Spark schema.
   *
   * @return the field name
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * Gets the Spark data type of the field.
   *
   * @return the field type
   */
  public DataType getFieldType() {
    return fieldType;
  }

  /**
   * Gets the name of the TigerGraph attribute this field maps to.
   *
   * @return the attribute name
   */
  public String getAttributeName() {
    return attributeName;
  }

  /**
   * Gets the operation to be performed on the attribute, if any.
   *
   * @return the operation, or null if no operation is specified
   */
  public String getOperation() {
    return operation;
  }

  // Core Converter Logic

  /**
   * Creates a converter function that transforms a value from a Spark {@link InternalRow} into a
   * TigerGraph-compatible {@link JsonNode}.
   *
   * <p>This is the central factory method that dispatches to various helper methods based on the
   * field's {@link DataType}. It handles primitive types, arrays, maps, and structs, ensuring they
   * are correctly validated and converted.
   *
   * @param fieldIndex The index of the field to convert.
   * @param fieldType The {@link DataType} of the field.
   * @return A {@link Function} that takes an {@link InternalRow} and returns a {@link JsonNode}.
   */
  public static Function<InternalRow, JsonNode> createConverter(
      int fieldIndex, DataType fieldType) {
    // Validate complex types upfront to fail early.
    if (fieldType instanceof MapType) {
      validateMapType((MapType) fieldType);
    } else if (fieldType instanceof StructType) {
      validateStructTypeForPrimitiveFields((StructType) fieldType);
    } else if (fieldType instanceof ArrayType) {
      ArrayType arrayType = (ArrayType) fieldType;
      DataType elementType = arrayType.elementType();
      if (elementType instanceof MapType) {
        validateMapType((MapType) elementType);
      } else if (elementType instanceof StructType) {
        validateStructTypeForPrimitiveFields((StructType) elementType);
      } else if (!isPrimitiveType(elementType)) {
        throwUnsupportedArrayElementTypeError(elementType.simpleString());
      }
    } else if (!isPrimitiveType(fieldType)) {
      throwUnsupportedTypeError(fieldType.simpleString());
    }

    // Create functional extractors and converters for this specific field type.
    InternalRowValueExtractor extractor = createRowExtractor(fieldType);
    ValueConverter converter = createValueConverter(fieldType);

    return row -> {
      if (row.isNullAt(fieldIndex)) {
        return null;
      }

      Object rawValue = extractor.extract(row, fieldIndex);
      Object convertedValue = converter.convert(rawValue);
      return mapper.valueToTree(convertedValue);
    };
  }

  // Type Validation Helpers

  /**
   * Checks if a Spark {@link DataType} is a primitive type supported by TigerGraph.
   *
   * <p>Primitive types include standard numeric types, string, boolean, date, and timestamp.
   *
   * @param dataType The data type to check.
   * @return True if the type is a supported primitive, false otherwise.
   */
  private static boolean isPrimitiveType(DataType dataType) {
    return dataType instanceof StringType
        || dataType instanceof IntegerType
        || dataType instanceof LongType
        || dataType instanceof BooleanType
        || dataType instanceof FloatType
        || dataType instanceof DoubleType
        || dataType instanceof ByteType
        || dataType instanceof ShortType
        || dataType instanceof DecimalType
        || dataType instanceof DateType
        || dataType instanceof TimestampType
        || dataType instanceof TimestampNTZType;
  }

  /**
   * Validates that a {@link MapType} is compatible with TigerGraph's MAP attribute type.
   *
   * <p>TigerGraph requires map keys to be primitive types. Map values can be either primitive types
   * or a UDT (represented as a {@link StructType} with only primitive fields).
   *
   * @param mapType The {@link MapType} to validate.
   * @throws IllegalArgumentException if the key or value types are not supported.
   */
  private static void validateMapType(MapType mapType) {
    if (!isPrimitiveType(mapType.keyType())) {
      throwUnsupportedMapKeyTypeError(mapType.keyType().simpleString());
    }

    DataType valueType = mapType.valueType();
    if (!isPrimitiveType(valueType) && !(valueType instanceof StructType)) {
      throwUnsupportedMapValueTypeError(valueType.simpleString());
    }

    if (valueType instanceof StructType) {
      validateStructTypeForPrimitiveFields((StructType) valueType);
    }
  }

  /**
   * Validates that a {@link StructType} is compatible with a TigerGraph User-Defined Tuple (UDT).
   *
   * <p>TigerGraph UDTs can only contain fields of primitive types.
   *
   * @param structType The {@link StructType} to validate.
   * @throws IllegalArgumentException if any field in the struct is not a primitive type.
   */
  private static void validateStructTypeForPrimitiveFields(StructType structType) {
    for (StructField field : structType.fields()) {
      DataType fieldType = field.dataType();
      if (!isPrimitiveType(fieldType)) {
        throwUnsupportedStructFieldTypeError(fieldType.simpleString());
      }
    }
  }

  // Data Conversion Helpers

  /**
   * Converts a Spark date value (days since epoch) to a UTC-formatted date string.
   *
   * @param dateValue The number of days since the epoch (1970-01-01).
   * @return A formatted date string in the format "yyyy-MM-dd".
   */
  static String convertSparkDateToDatetime(int dateValue) {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    return dateFormat.format(new Date((long) dateValue * MILLIS_IN_A_DAY));
  }

  /**
   * Converts a Spark timestamp value (microseconds since epoch) to a UTC-formatted timestamp
   * string.
   *
   * @param timestampValue The number of microseconds since the epoch (1970-01-01T00:00:00Z).
   * @return A formatted timestamp string in the format "yyyy-MM-dd HH:mm:ss.SSS".
   */
  static String convertSparkTimestampToDatetime(long timestampValue) {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    return dateFormat.format(new Timestamp(timestampValue / 1000));
  }

  /**
   * Converts a Spark {@link MapData} object into a TigerGraph-compatible JSON representation.
   *
   * <p>The resulting JSON object has two keys: "keylist" and "valuelist", which are arrays
   * containing the map's keys and values, respectively.
   *
   * <p>Example Output:
   *
   * <pre>{@code
   * {
   *   "keylist": ["key1", "key2"],
   *   "valuelist": [10, 20]
   * }
   * }</pre>
   *
   * @param mapData The {@link MapData} to convert.
   * @param keyExtractor A function to extract a key from the key array.
   * @param keyConverter A function to convert the extracted key.
   * @param valueExtractor A function to extract a value from the value array.
   * @param valueConverter A function to convert the extracted value.
   * @return An {@link ObjectNode} representing the map.
   */
  private static ObjectNode convertMapToJson(
      MapData mapData,
      ArrayDataValueExtractor keyExtractor,
      ValueConverter keyConverter,
      ArrayDataValueExtractor valueExtractor,
      ValueConverter valueConverter) {
    ObjectNode result = mapper.createObjectNode();
    List<Object> keyList = new ArrayList<>();
    List<Object> valueList = new ArrayList<>();

    ArrayData keys = mapData.keyArray();
    ArrayData values = mapData.valueArray();

    for (int i = 0; i < mapData.numElements(); i++) {
      if (!keys.isNullAt(i)) {
        Object keyValue = keyExtractor.extract(keys, i);
        Object convertedKey = keyConverter.convert(keyValue);
        keyList.add(convertedKey);

        Object convertedValue = null;
        if (!values.isNullAt(i)) {
          Object valueValue = valueExtractor.extract(values, i);
          convertedValue = valueConverter.convert(valueValue);
        }
        valueList.add(convertedValue);
      }
    }

    result.set("keylist", mapper.valueToTree(keyList));
    result.set("valuelist", mapper.valueToTree(valueList));
    return result;
  }

  /**
   * Converts a Spark {@link InternalRow} representing a struct into a TigerGraph-compatible JSON
   * representation for a UDT.
   *
   * <p>The resulting JSON object has two keys: "keylist" (the struct field names) and "valuelist"
   * (the struct field values).
   *
   * <p>Example Output (for a struct with fields "name" and "age"):
   *
   * <pre>{@code
   * {
   *   "keylist": ["name", "age"],
   *   "valuelist": ["Alice", 30]
   * }
   * }</pre>
   *
   * @param structRow The {@link InternalRow} representing the struct data.
   * @param structType The {@link StructType} defining the struct's schema.
   * @param fieldExtractors An array of functions to extract each field's value.
   * @param fieldConverters An array of functions to convert each field's value.
   * @return An {@link ObjectNode} representing the UDT.
   */
  private static ObjectNode convertStructToJson(
      InternalRow structRow,
      StructType structType,
      InternalRowValueExtractor[] fieldExtractors,
      ValueConverter[] fieldConverters) {
    ObjectNode result = mapper.createObjectNode();
    List<String> keyList = new ArrayList<>();
    List<Object> valueList = new ArrayList<>();

    StructField[] fields = structType.fields();
    for (int i = 0; i < fields.length; i++) {
      StructField field = fields[i];
      keyList.add(field.name());

      Object value = null;
      if (!structRow.isNullAt(i)) {
        Object fieldValue = fieldExtractors[i].extract(structRow, i);
        value = fieldConverters[i].convert(fieldValue);
      }
      valueList.add(value);
    }

    result.set("keylist", mapper.valueToTree(keyList));
    result.set("valuelist", mapper.valueToTree(valueList));
    return result;
  }

  /**
   * Converts a Spark {@link ArrayData} object into a standard Java {@link List} that can be
   * serialized to a JSON array.
   *
   * @param arrayData The {@link ArrayData} to convert.
   * @param extractor A function to extract an element from the array.
   * @param converter A function to convert the extracted element.
   * @return A {@link List} containing the converted array elements.
   */
  private static List<Object> convertArrayToJsonCompatible(
      ArrayData arrayData, ArrayDataValueExtractor extractor, ValueConverter converter) {
    List<Object> convertedList = new ArrayList<>();

    for (int i = 0; i < arrayData.numElements(); i++) {
      if (!arrayData.isNullAt(i)) {
        Object rawValue = extractor.extract(arrayData, i);
        Object convertedValue = converter.convert(rawValue);
        if (convertedValue != null) {
          convertedList.add(convertedValue);
        }
      }
    }

    return convertedList;
  }

  // Extractor and Converter Factories

  /**
   * Creates a {@link ValueConverter} function for a given Spark {@link DataType}.
   *
   * <p>This function is responsible for converting an extracted raw value (e.g., from an {@link
   * InternalRow} or {@link ArrayData}) into a format suitable for JSON serialization and loading
   * into TigerGraph.
   *
   * @param dataType The data type for which to create the converter.
   * @return A {@link ValueConverter} function.
   */
  private static ValueConverter createValueConverter(DataType dataType) {
    if (dataType instanceof StringType) {
      return value -> value == null ? null : value.toString();
    } else if (dataType instanceof IntegerType
        || dataType instanceof LongType
        || dataType instanceof BooleanType
        || dataType instanceof FloatType
        || dataType instanceof DoubleType
        || dataType instanceof ByteType
        || dataType instanceof ShortType) {
      return value -> value;
    } else if (dataType instanceof DecimalType) {
      return value -> {
        if (value == null) return null;
        if (value instanceof Decimal) {
          return ((Decimal) value).toJavaBigDecimal();
        }
        return value;
      };
    } else if (dataType instanceof DateType) {
      return value -> {
        if (value == null) return null;
        return convertSparkDateToDatetime((Integer) value);
      };
    } else if (dataType instanceof TimestampType || dataType instanceof TimestampNTZType) {
      return value -> {
        if (value == null) return null;
        return convertSparkTimestampToDatetime((Long) value);
      };
    } else if (dataType instanceof ArrayType) {
      ArrayType arrayType = (ArrayType) dataType;
      DataType elementType = arrayType.elementType();
      ArrayDataValueExtractor elementExtractor = createArrayExtractor(elementType);
      ValueConverter elementConverter = createValueConverter(elementType);
      return value -> {
        if (value == null) return null;
        return convertArrayToJsonCompatible((ArrayData) value, elementExtractor, elementConverter);
      };
    } else if (dataType instanceof MapType) {
      MapType mapType = (MapType) dataType;
      ArrayDataValueExtractor keyExtractor = createArrayExtractor(mapType.keyType());
      ValueConverter keyConverter = createValueConverter(mapType.keyType());
      ArrayDataValueExtractor valueExtractor = createArrayExtractor(mapType.valueType());
      ValueConverter valueConverter = createValueConverter(mapType.valueType());
      return value -> {
        if (value == null) return null;
        return convertMapToJson(
            (MapData) value, keyExtractor, keyConverter, valueExtractor, valueConverter);
      };
    } else if (dataType instanceof StructType) {
      StructType structType = (StructType) dataType;
      StructField[] fields = structType.fields();
      InternalRowValueExtractor[] fieldExtractors = new InternalRowValueExtractor[fields.length];
      ValueConverter[] fieldConverters = new ValueConverter[fields.length];
      for (int i = 0; i < fields.length; i++) {
        fieldExtractors[i] = createRowExtractor(fields[i].dataType());
        fieldConverters[i] = createValueConverter(fields[i].dataType());
      }
      return value -> {
        if (value == null) return null;
        return convertStructToJson(
            (InternalRow) value, structType, fieldExtractors, fieldConverters);
      };
    } else {
      throwUnsupportedTypeError(dataType.simpleString());
      return null; // Unreachable code
    }
  }

  /**
   * Creates an {@link InternalRowValueExtractor} function for a given Spark {@link DataType}.
   *
   * <p>This function is responsible for extracting a raw value for a specific field from an {@link
   * InternalRow}. It uses the appropriate getter method on the row (e.g., {@code getInt}, {@code
   * getUTF8String}) based on the data type.
   *
   * @param dataType The data type of the field to extract.
   * @return An {@link InternalRowValueExtractor} function.
   */
  private static InternalRowValueExtractor createRowExtractor(DataType dataType) {
    if (dataType instanceof StringType) {
      return (row, index) -> row.getUTF8String(index).toString();
    } else if (dataType instanceof IntegerType) {
      return (row, index) -> row.getInt(index);
    } else if (dataType instanceof LongType) {
      return (row, index) -> row.getLong(index);
    } else if (dataType instanceof BooleanType) {
      return (row, index) -> row.getBoolean(index);
    } else if (dataType instanceof FloatType) {
      return (row, index) -> row.getFloat(index);
    } else if (dataType instanceof DoubleType) {
      return (row, index) -> row.getDouble(index);
    } else if (dataType instanceof ByteType) {
      return (row, index) -> row.getByte(index);
    } else if (dataType instanceof ShortType) {
      return (row, index) -> row.getShort(index);
    } else if (dataType instanceof DecimalType) {
      DecimalType dt = (DecimalType) dataType;
      return (row, index) -> row.getDecimal(index, dt.precision(), dt.scale());
    } else if (dataType instanceof DateType) {
      return (row, index) -> row.getInt(index);
    } else if (dataType instanceof TimestampType || dataType instanceof TimestampNTZType) {
      return (row, index) -> row.getLong(index);
    } else if (dataType instanceof ArrayType) {
      return (row, index) -> row.getArray(index);
    } else if (dataType instanceof MapType) {
      return (row, index) -> row.getMap(index);
    } else if (dataType instanceof StructType) {
      return (row, index) -> row.getStruct(index, ((StructType) dataType).size());
    } else {
      throwUnsupportedTypeError(dataType.simpleString());
      return null; // Unreachable code
    }
  }

  /**
   * Creates an {@link ArrayDataValueExtractor} function for a given Spark {@link DataType}.
   *
   * <p>This function is responsible for extracting a raw value from a specific element within an
   * {@link ArrayData} object. It uses the appropriate getter method based on the element's data
   * type.
   *
   * @param dataType The data type of the elements in the array.
   * @return An {@link ArrayDataValueExtractor} function.
   */
  private static ArrayDataValueExtractor createArrayExtractor(DataType dataType) {
    if (dataType instanceof StringType) {
      return (array, index) -> array.getUTF8String(index).toString();
    } else if (dataType instanceof IntegerType) {
      return (array, index) -> array.getInt(index);
    } else if (dataType instanceof LongType) {
      return (array, index) -> array.getLong(index);
    } else if (dataType instanceof BooleanType) {
      return (array, index) -> array.getBoolean(index);
    } else if (dataType instanceof FloatType) {
      return (array, index) -> array.getFloat(index);
    } else if (dataType instanceof DoubleType) {
      return (array, index) -> array.getDouble(index);
    } else if (dataType instanceof ByteType) {
      return (array, index) -> array.getByte(index);
    } else if (dataType instanceof ShortType) {
      return (array, index) -> array.getShort(index);
    } else if (dataType instanceof DecimalType) {
      DecimalType dt = (DecimalType) dataType;
      return (array, index) -> array.getDecimal(index, dt.precision(), dt.scale());
    } else if (dataType instanceof DateType) {
      return (array, index) -> array.getInt(index);
    } else if (dataType instanceof TimestampType || dataType instanceof TimestampNTZType) {
      return (array, index) -> array.getLong(index);
    } else if (dataType instanceof ArrayType) {
      return (array, index) -> array.getArray(index);
    } else if (dataType instanceof MapType) {
      return (array, index) -> array.getMap(index);
    } else if (dataType instanceof StructType) {
      return (array, index) -> array.getStruct(index, ((StructType) dataType).size());
    } else {
      throwUnsupportedTypeError(dataType.simpleString());
      return null; // Unreachable code
    }
  }

  // Exception Helpers

  private static void throwUnsupportedTypeError(String typeName) {
    String errMsg =
        String.format(
            "Unsupported data type '%s', only primitive types, Array, Map, and Struct are"
                + " supported.",
            typeName);
    throw new IllegalArgumentException(errMsg);
  }

  private static void throwUnsupportedArrayElementTypeError(String typeName) {
    String errMsg =
        String.format(
            "Unsupported array element type '%s', only arrays of primitive types, Map, and Struct"
                + " are supported.",
            typeName);
    throw new IllegalArgumentException(errMsg);
  }

  private static void throwUnsupportedStructFieldTypeError(String typeName) {
    String errMsg =
        String.format(
            "Unsupported struct field type '%s', struct fields can only be primitive types.",
            typeName);
    throw new IllegalArgumentException(errMsg);
  }

  private static void throwUnsupportedMapKeyTypeError(String typeName) {
    String errMsg =
        String.format(
            "Unsupported map key type '%s', map keys can only be primitive types.", typeName);
    throw new IllegalArgumentException(errMsg);
  }

  private static void throwUnsupportedMapValueTypeError(String typeName) {
    String errMsg =
        String.format(
            "Unsupported map value type '%s', map values can only be primitive types or struct with"
                + " primitive fields.",
            typeName);
    throw new IllegalArgumentException(errMsg);
  }

  // Functional Interfaces

  /** A functional interface for extracting a value from a specific index of an InternalRow. */
  @FunctionalInterface
  private interface InternalRowValueExtractor {
    Object extract(InternalRow row, int index);
  }

  /** A functional interface for extracting a value from a specific index of an ArrayData. */
  @FunctionalInterface
  private interface ArrayDataValueExtractor {
    Object extract(ArrayData array, int index);
  }

  /** A functional interface for converting a raw extracted value into its final form. */
  @FunctionalInterface
  private interface ValueConverter {
    Object convert(Object value);
  }
}
