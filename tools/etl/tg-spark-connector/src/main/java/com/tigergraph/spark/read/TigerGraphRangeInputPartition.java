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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.spark.sql.connector.read.InputPartition;

/** Partitioned query range information */
public class TigerGraphRangeInputPartition implements InputPartition {

  private final boolean isPartitionedQuery;
  private Integer partitionId;
  private String partitionKey;
  private BigInteger noLessThan;
  private BigInteger lessThan;
  // can be applied to RESTPP query param `filter`
  private String partitionRange;

  public TigerGraphRangeInputPartition(
      Integer partitionId, String partitionKey, BigInteger noLessThan, BigInteger lessThan) {
    isPartitionedQuery = true;
    this.partitionId = partitionId;
    this.partitionKey = partitionKey;
    this.noLessThan = noLessThan;
    this.lessThan = lessThan;
    String filterGe = noLessThan == null ? null : partitionKey + ">=" + noLessThan;
    String filterLt = lessThan == null ? null : partitionKey + "<" + lessThan;
    partitionRange =
        Stream.of(filterGe, filterLt).filter(s -> s != null).collect(Collectors.joining(","));
  }

  public TigerGraphRangeInputPartition() {
    isPartitionedQuery = false;
  }

  /**
   * Calculate the partitions' range based on partitionNum and lower/upper bound E.g., given:
   * partitionNum=5, lower=20, upper=50 return: splitPoints=[20, 30, 40, 50] partition info: (,20),
   * [20,30), [30,40), [40,50), [50,)
   */
  public static List<BigInteger> calculatePartitions(
      Integer partitionNum, BigInteger lowerBound, BigInteger upperBound) {
    List<BigInteger> splitPoints = new ArrayList<BigInteger>();
    // Only one partition, no split point
    if (partitionNum <= 1) return splitPoints;
    // 2 partitions, use (lower + upper)/2 as the split point
    if (partitionNum == 2) {
      splitPoints.add(lowerBound.add(upperBound).divide(BigInteger.valueOf(2)));
      return splitPoints;
    }
    // more than 2 partitions, calculate the stride=(upper - lower)/(partitionNum - 2)
    BigInteger stride =
        upperBound
            .subtract(lowerBound)
            .divide(BigInteger.valueOf(partitionNum - 2)); // exclude head and tail
    stride = stride.max(BigInteger.ONE);
    // Split the partition based on stride
    while (lowerBound.compareTo(upperBound) < 0 && splitPoints.size() < partitionNum - 2) {
      splitPoints.add(lowerBound);
      lowerBound = lowerBound.add(stride);
    }
    splitPoints.add(upperBound);
    return splitPoints;
  }

  public Boolean isPartitionedQuery() {
    return isPartitionedQuery;
  }

  public Integer getPartitionId() {
    return partitionId;
  }

  public String getPartitionKey() {
    return partitionKey;
  }

  /* No less than */
  public BigInteger getLowerBound() {
    return noLessThan;
  }

  /* Less than */
  public BigInteger getUpperBound() {
    return lessThan;
  }

  /**
   * The partition range which can be directly applied to RESTPP query param 'filter' E.g.,
   * length>=50,length<70
   */
  public String getPartitionRange() {
    return partitionRange;
  }

  @Override
  public String toString() {
    if (isPartitionedQuery) {
      return "partitionId: " + partitionId + ", partitionRange: " + partitionRange;
    } else {
      return "single partition query";
    }
  }
}
