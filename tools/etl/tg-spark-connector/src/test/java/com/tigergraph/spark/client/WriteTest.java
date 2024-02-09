package com.tigergraph.spark.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WriteTest {
  @Test
  public void testCheckInvalidData() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    // don't have invalid data
    String jsonString =
        "{\"results\": [\n"
            + "{\"sourceFileName\": \"Online_POST\",\n"
            + "\"statistics\": {\n"
            + "\"validLine\": 7927,\n"
            + "\"rejectLine\": 0,\n"
            + "\"failedConditionLine\": 0,\n"
            + "\"notEnoughToken\": 0,\n"
            + "\"invalidJson\": 0,\n"
            + "\"oversizeToken\": 0,\n"
            + "\"vertex\": [\n"
            + "{\"typeName\": \"company\",\n"
            + "\"validObject\": 7,\n"
            + "\"noIdFound\": 0,\n"
            + "\"invalidAttribute\": 0,\n"
            + "\"invalidPrimaryId\": 0,\n"
            + "\"invalidSecondaryId\": 0,\n"
            + "\"incorrectFixedBinaryLength\": 0\n"
            + "}\n"
            + "],\n"
            + "\"edge\": [],\n"
            + "\"deleteVertex\": [],\n"
            + "\"deleteEdge\": []\n"
            + "}\n"
            + "}\n"
            + "]}";
    assertFalse(Write.LoadingResponse.hasInvalidRecord(mapper.readTree(jsonString)));
    // have invalid data
    jsonString =
        "{\"results\": [\n"
            + "{\"sourceFileName\": \"Online_POST\",\n"
            + "\"statistics\": {\n"
            + "\"validLine\": 7927,\n"
            + "\"rejectLine\": 1,\n"
            + "\"failedConditionLine\": 0,\n"
            + "\"notEnoughToken\": 0,\n"
            + "\"invalidJson\": 0,\n"
            + "\"oversizeToken\": 0,\n"
            + "\"vertex\": [\n"
            + "{\"typeName\": \"company\",\n"
            + "\"validObject\": 7,\n"
            + "\"noIdFound\": 0,\n"
            + "\"invalidAttribute\": 3,\n"
            + "\"invalidPrimaryId\": 0,\n"
            + "\"invalidSecondaryId\": 0,\n"
            + "\"incorrectFixedBinaryLength\": 0\n"
            + "}\n"
            + "],\n"
            + "\"edge\": [],\n"
            + "\"deleteVertex\": [],\n"
            + "\"deleteEdge\": []\n"
            + "}\n"
            + "}\n"
            + "]}";
    assertTrue(Write.LoadingResponse.hasInvalidRecord(mapper.readTree(jsonString)));
  }
}
