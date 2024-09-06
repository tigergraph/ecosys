/**
 * Copyright (c) 2023 TigerGraph Inc.
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
package com.tigergraph.spark.client.common;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tigergraph.spark.log.LoggerFactory;
import com.tigergraph.spark.util.Utils;
import java.io.IOException;
import org.slf4j.Logger;

/**
 * TG RESTPP response with streaming reader. Support reading from large 'results' array row by row
 */
public class RestppStreamResponse {

  private static final Logger logger = LoggerFactory.getLogger(RestppStreamResponse.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  public String code;
  public boolean error;
  public String message;
  public JsonParser results;

  // store the current row which can be read multiple times
  private JsonNode currentRow = null;
  private Boolean reinited = false;
  // whether the JSON key is meaningful, e.g., MapAccum
  private Boolean isMap = false;

  /** Throw exception when HTTP status code is 200 but RESTPP error=true */
  public void panicOnFail() {
    if (error) {
      throw new RestppErrorException(code, message);
    }
  }

  /**
   * Move forward the cursor to next row and indicate whether the next one is readable
   *
   * @return true if it moves to next row successfully and doesn't reach the end
   * @throws IOException
   */
  public boolean next() throws IOException {
    if (results == null || results.isClosed()) return false;
    JsonToken nxtToken = results.nextToken();
    if (!JsonToken.END_ARRAY.equals(nxtToken) && !JsonToken.END_OBJECT.equals(nxtToken)) {
      try {
        if (isMap) {
          results.nextToken();
          ObjectNode node = mapper.createObjectNode();
          currentRow =
              node.put("key", results.currentName()).set("value", results.readValueAsTree());
        } else {
          currentRow = results.readValueAsTree();
        }
      } catch (JsonParseException e) {
        // It's likely the response is too large and RESTPP truncate it
        // then the JSON structure is incomplete and invalid.
        // We stop here and tell it reaches the end.
        results.close();
        currentRow = null;
        return false;
      }
      return true;
    } else {
      results.close();
      currentRow = null;
      return false;
    }
  }

  /**
   * Read the current row into {@link JsonNode} format. This method should return same value until
   * `next` is called.
   *
   * @return {@link JsonNode}
   */
  public JsonNode readRow() {
    return currentRow;
  }

  /**
   * Move to the expected JSON object based on row number and JSON key.
   *
   * @throws IOException
   */
  public void reinitCursor(Integer rowNumber, String objKey) throws IOException {
    if (!reinited) {
      // Skip #rowNumber elements of results JSON array
      for (int i = 0; i < rowNumber; i++) {
        results.nextToken();
        results.skipChildren();
        if (results.currentToken() == null || results.currentToken().equals(JsonToken.END_ARRAY)) {
          throw new UnsupportedOperationException(
              "The query results do not contain a row of index "
                  + rowNumber
                  + ". Total row number: "
                  + i);
        }
      }
      // Go into the first JSON object of the current JSON array element;
      results.nextToken();
      // Move forward until meet the expected JSON key.
      while (true) {
        // To FIELD_NAME
        results.nextToken();
        if (results.currentToken() == null || results.currentToken().equals(JsonToken.END_OBJECT)) {
          throw new UnsupportedOperationException(
              "Row " + rowNumber + " of the query results doesn't contain JSON key " + objKey);
        }
        if (Utils.isEmpty(objKey) || objKey.equals(results.getCurrentName())) {
          break;
        } else {
          // To value
          results.nextToken();
          // To END_OBJECT
          results.skipChildren();
        }
      }
      results.nextToken();
      if (JsonToken.START_ARRAY.equals(results.getCurrentToken())) {
        this.isMap = false;
      } else if (JsonToken.START_OBJECT.equals(results.getCurrentToken())) {
        this.isMap = true;
      } else {
        throw new UnsupportedOperationException(
            String.format(
                "The value of %d:%s can't be extracted because it is neither JSON array, nor JSON"
                    + " object: %s.",
                rowNumber, objKey, results.getCurrentToken().toString()));
      }
      this.reinited = true;
    }
  }

  public void close() {
    if (results != null && !results.isClosed())
      try {
        results.close();
      } catch (IOException e) {
        // no-op
      }
  }
}
