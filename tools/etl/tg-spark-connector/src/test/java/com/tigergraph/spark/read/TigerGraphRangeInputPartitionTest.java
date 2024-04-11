package com.tigergraph.spark.read;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TigerGraphRangeInputPartitionTest {

  @Test
  void testCalculatePartitions() {
    List<BigInteger> actual;
    // Only 1 partition, no split point
    actual =
        TigerGraphRangeInputPartition.calculatePartitions(
            1, BigInteger.valueOf(100), BigInteger.valueOf(200));
    assertArrayEquals(new BigInteger[] {}, actual.toArray());
    // 2 partitions, split point = (upper + lower)/2
    actual =
        TigerGraphRangeInputPartition.calculatePartitions(
            2, BigInteger.valueOf(100), BigInteger.valueOf(199));
    assertArrayEquals(new BigInteger[] {BigInteger.valueOf(149)}, actual.toArray());
    // more than 2 partitions
    actual =
        TigerGraphRangeInputPartition.calculatePartitions(
            10, BigInteger.valueOf(10), BigInteger.valueOf(300));
    assertArrayEquals(
        new BigInteger[] {
          BigInteger.valueOf(10),
          BigInteger.valueOf(46),
          BigInteger.valueOf(82),
          BigInteger.valueOf(118),
          BigInteger.valueOf(154),
          BigInteger.valueOf(190),
          BigInteger.valueOf(226),
          BigInteger.valueOf(262),
          BigInteger.valueOf(300)
        },
        actual.toArray());
    // partition num > upper - lower
    actual =
        TigerGraphRangeInputPartition.calculatePartitions(
            10, BigInteger.valueOf(5), BigInteger.valueOf(10));
    assertArrayEquals(
        new BigInteger[] {
          BigInteger.valueOf(5),
          BigInteger.valueOf(6),
          BigInteger.valueOf(7),
          BigInteger.valueOf(8),
          BigInteger.valueOf(9),
          BigInteger.valueOf(10)
        },
        actual.toArray());
  }

  @Test
  void testPartitionRangeInfo() {
    TigerGraphRangeInputPartition actual;
    // Non-partitioned query
    actual = new TigerGraphRangeInputPartition();
    assertEquals("single partition query", actual.toString());
    // Partitioned query
    actual =
        new TigerGraphRangeInputPartition(
            3, "length", BigInteger.valueOf(100), BigInteger.valueOf(300));
    assertEquals("partitionId: 3, partitionRange: length>=100,length<300", actual.toString());

    actual = new TigerGraphRangeInputPartition(3, "length", BigInteger.valueOf(100), null);
    assertEquals("partitionId: 3, partitionRange: length>=100", actual.toString());

    actual = new TigerGraphRangeInputPartition(3, "length", null, BigInteger.valueOf(300));
    assertEquals("partitionId: 3, partitionRange: length<300", actual.toString());
  }
}
