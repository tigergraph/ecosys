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
package com.tigergraph.spark.read;

import com.fasterxml.jackson.databind.JsonNode;
import com.tigergraph.spark.util.Options.QueryType;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.expressions.GenericInternalRow;
import org.apache.spark.sql.types.BinaryType;
import org.apache.spark.sql.types.BooleanType;
import org.apache.spark.sql.types.ByteType;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DateType;
import org.apache.spark.sql.types.DayTimeIntervalType;
import org.apache.spark.sql.types.Decimal;
import org.apache.spark.sql.types.DecimalType;
import org.apache.spark.sql.types.DoubleType;
import org.apache.spark.sql.types.FloatType;
import org.apache.spark.sql.types.IntegerType;
import org.apache.spark.sql.types.LongType;
import org.apache.spark.sql.types.NullType;
import org.apache.spark.sql.types.ShortType;
import org.apache.spark.sql.types.StringType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.types.TimestampNTZType;
import org.apache.spark.sql.types.TimestampType;
import org.apache.spark.sql.types.YearMonthIntervalType;
import org.apache.spark.unsafe.types.UTF8String;

/**
 * The converter that is responsible for converting RESTPP query response(in JSON) to {@link
 * org.apache.spark.sql.catalyst.InternalRow}. The order of the fields should match the given
 * schema.
 */
public class TigerGraphJsonConverter {
  private final StructField[] fields;
  private final Object[] values;
  // the converters for each field according to the data types
  private final Function<JsonNode, Object>[] fieldConverter;
  // the searcher that find Spark field name in a nested JSON
  private final BiFunction<JsonNode, String, JsonNode> nodeSearcher;

  @SuppressWarnings("unchecked")
  public TigerGraphJsonConverter(StructType schema, QueryType queryType) {
    fields = schema.fields();
    values = new Object[fields.length];
    fieldConverter = new Function[fields.length];
    // Predefine the converter of each field so that no
    // need to do type checks for every row
    for (int i = 0; i < fields.length; i++) {
      fieldConverter[i] = getConverter(fields[i].dataType());
    }
    nodeSearcher = getNodeSearcher(queryType);
  }

  /**
   * Convert the JSON query response to a Spark row with the pre-registered converter and mapper.
   *
   * @param obj the JSON query response
   */
  public InternalRow convert(JsonNode obj) {
    for (int i = 0; i < values.length; i++) {
      DataType dt = fields[i].dataType();
      JsonNode rawVal = nodeSearcher.apply(obj, fields[i].name());
      if (rawVal != null && !rawVal.isMissingNode() && !(dt instanceof NullType)) {
        values[i] = fieldConverter[i].apply(rawVal);
      } else {
        values[i] = null;
      }
    }
    return new GenericInternalRow(values);
  }

  /**
   * Get the converter of a specific data type which can convert a JSON value to the Spark type.
   *
   * <p>Check {@link org.apache.spark.sql.catalyst.InternalRow#getAccessor(DataType, boolean)} to
   * know how Spark SQL types are mapped to those primitive types
   *
   * @param dt the data type of the field
   */
  private static Function<JsonNode, Object> getConverter(DataType dt) {
    if (dt instanceof BooleanType) {
      return (obj) -> obj.asBoolean();
    } else if (dt instanceof ByteType) {
      return (obj) -> Byte.parseByte(obj.asText());
    } else if (dt instanceof ShortType) {
      return (obj) -> (short) obj.asInt();
    } else if (dt instanceof IntegerType
        || dt instanceof DateType
        || dt instanceof YearMonthIntervalType) {
      return (obj) -> obj.asInt();
    } else if (dt instanceof LongType
        || dt instanceof TimestampType
        || dt instanceof TimestampNTZType
        || dt instanceof DayTimeIntervalType) {
      return (obj) -> obj.asLong();
    } else if (dt instanceof FloatType) {
      return (obj) -> (float) obj.asDouble();
    } else if (dt instanceof DoubleType) {
      return (obj) -> obj.asDouble();
    } else if (dt instanceof DecimalType) {
      return (obj) -> Decimal.apply(obj.asText());
    } else if (dt instanceof StringType) {
      return (obj) -> {
        String val = obj.isValueNode() ? obj.asText() : obj.toString();
        return UTF8String.fromString(val);
      };
    } else if (dt instanceof BinaryType) {
      // TG returns the fixed_binary(n) attribute in this way:
      // "Bytes byte1 byte2 ... byte_n"
      return (obj) -> {
        String[] splitted = obj.asText().split(" ");
        byte[] res = new byte[splitted.length - 1];
        for (int i = 0; i < res.length; i++) {
          res[i] = Byte.parseByte(splitted[i + 1]);
        }
        return res;
      };
    } else {
      // Unsupported types
      // * CalendarIntervalType
      // * ArrayType (TODO in the future, currently parse it as String)
      // * MapType
      // * StructType
      throw new UnsupportedOperationException("Unsupported data type " + dt.simpleString());
    }
  }

  /**
   * Get the node searcher that can find a Spark field in nested JSON: E.g., When query type is
   * "GET_VERTICES", "name" -> "attributes.name"
   *
   * @param qt the query type
   */
  private static BiFunction<JsonNode, String, JsonNode> getNodeSearcher(QueryType qt) {
    switch (qt) {
      case GET_VERTEX:
      case GET_VERTICES:
        return (obj, key) -> "v_id".equals(key) ? obj.get(key) : obj.path("attributes").get(key);
        // TODO: edge type and query type
      default:
        return (obj, key) -> obj.get(key);
    }
  }
}
