package com.tigergraph.spark.write;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.expressions.GenericInternalRow;
import org.apache.spark.sql.types.Decimal;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.unsafe.types.UTF8String;
import org.junit.jupiter.api.Test;

public class TigerGraphDataWriterTest {
  @Test
  public void testSupportedDataTypesConversion() {
    StructType schema =
        StructType.fromDDL(
            new StringBuilder()
                .append("c0 BOOLEAN")
                .append(",c1 BYTE")
                .append(",c2 SHORT")
                .append(",c3 INT")
                .append(",c4 DATE")
                .append(",c6 LONG")
                .append(",c7 TIMESTAMP")
                .append(",c8 FLOAT")
                .append(",c9 DOUBLE")
                .append(",c10 DECIMAL(38, 0)")
                .append(",c11 STRING")
                .toString());

    // Create an array of values corresponding to the schema
    Object[] values =
        new Object[] {
          true,
          (byte) 1,
          (short) 123,
          456,
          19025,
          7898765L,
          1711701745123462L,
          3.14f,
          2.718,
          Decimal.apply("12345678901234567890123456789012345678.123"),
          UTF8String.fromString("Hello World")
        };

    // Create a GenericInternalRow(Spark row)
    InternalRow row = new GenericInternalRow(values);

    // Convert the GenericInternalRow to delimited Strings by the converter
    List<BiFunction<InternalRow, Integer, String>> converters =
        TigerGraphDataWriter.getConverters(schema);

    String actual =
        IntStream.range(0, row.numFields())
            .mapToObj(i -> converters.get(i).apply(row, i))
            .collect(Collectors.joining("|"));
    String expected =
        "true|1|123|456|2022-02-02|7898765|2024-03-29"
            + " 08:42:25.123|3.14|2.718|12345678901234567890123456789012345678.123|Hello World";

    assertEquals(expected, actual);
  }
}
