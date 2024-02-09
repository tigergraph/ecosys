package com.tigergraph.spark;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class TigerGraphConnectionTest {
  @Test
  public void testGenerateJobId() {
    assertTrue(
        TigerGraphConnection.generateJobId("graph", "load_social", 1234567)
            .equals("graph.load_social.spark.all.1234567"));
  }
}
