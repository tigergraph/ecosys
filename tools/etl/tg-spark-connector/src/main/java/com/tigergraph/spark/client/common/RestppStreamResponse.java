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
import java.io.IOException;

/**
 * TG RESTPP response with streaming reader. Support reading from large 'results' array row by row
 */
public class RestppStreamResponse {
  public String code;
  public boolean error;
  public String message;
  public JsonParser results;

  // store the current row which can be read multiple times
  private JsonNode currentRow;

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
    if (!JsonToken.END_ARRAY.equals(results.nextToken())) {
      try {
        currentRow = results.readValueAsTree();
      } catch (JsonParseException e) {
        // It's likely the response is too large and RESTPP truncate it
        // then the JSON structure is incomplete and invalid.
        // We stop here and tell it reaches the end.
        results.close();
        return false;
      }
      return true;
    } else {
      results.close();
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
    if (results == null
        || results.isClosed()
        || JsonToken.START_ARRAY.equals(results.currentToken())) {
      return null;
    }
    return currentRow;
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
