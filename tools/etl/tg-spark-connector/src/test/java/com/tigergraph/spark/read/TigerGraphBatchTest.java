package com.tigergraph.spark.read;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.spark.sql.connector.read.InputPartition;
import org.junit.jupiter.api.Test;

public class TigerGraphBatchTest {
  @Test
  public void testPlanInputPartitions() {
    // Partitioned query 1
    InputPartition[] partitions = TigerGraphBatch.planInputPartitions(5, "age", "20", "50");

    String expected =
        String.join(
            "|",
            "partitionId: 0, partitionRange: age<20",
            "partitionId: 1, partitionRange: age>=20,age<30",
            "partitionId: 2, partitionRange: age>=30,age<40",
            "partitionId: 3, partitionRange: age>=40,age<50",
            "partitionId: 4, partitionRange: age>=50");
    String actual =
        Arrays.stream(partitions).map((p) -> p.toString()).collect(Collectors.joining("|"));
    assertEquals(expected, actual);

    // Partitioned query 2 (1 partition)
    partitions = TigerGraphBatch.planInputPartitions(1, "age", "20", "50");

    expected = String.join("|", "partitionId: 0, partitionRange: ");
    actual = Arrays.stream(partitions).map((p) -> p.toString()).collect(Collectors.joining("|"));
    assertEquals(expected, actual);

    // Partitioned query 3 (2 partition)
    partitions = TigerGraphBatch.planInputPartitions(2, "age", "20", "49");

    expected =
        String.join(
            "|",
            "partitionId: 0, partitionRange: age<34",
            "partitionId: 1, partitionRange: age>=34");
    actual = Arrays.stream(partitions).map((p) -> p.toString()).collect(Collectors.joining("|"));
    assertEquals(expected, actual);

    // Partitioned query 4 (Partitioned num > upper - lower)
    partitions = TigerGraphBatch.planInputPartitions(15, "age", "10", "13");

    expected =
        String.join(
            "|",
            "partitionId: 0, partitionRange: age<10",
            "partitionId: 1, partitionRange: age>=10,age<11",
            "partitionId: 2, partitionRange: age>=11,age<12",
            "partitionId: 3, partitionRange: age>=12,age<13",
            "partitionId: 4, partitionRange: age>=13");
    actual = Arrays.stream(partitions).map((p) -> p.toString()).collect(Collectors.joining("|"));
    assertEquals(expected, actual);

    // Non-partitioned query
    partitions = TigerGraphBatch.planInputPartitions(15, null, "10", "13");
    assertEquals(partitions.length, 1);
    assertEquals("single partition query", partitions[0].toString());
  }
}
