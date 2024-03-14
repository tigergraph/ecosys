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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

/**
 * The RESTPP response decoder used for handling very large response. Parse JSON response with
 * Jackson Streaming API in lazy evaluation: it only parse response for {@link
 * RestppStreamResponse}, when parsing to the 'results' field, stop parsing and hand the parser to
 * {@link RestppStreamResponse}, then Spark can call 'readRow()' when needed.
 */
public class RestppStreamDecoder implements Decoder {
  // Expected fields, others will be discarded.
  private static final String KEY_CODE = "code";
  private static final String KEY_ERROR = "error";
  private static final String KEY_MESSAGE = "message";
  private static final String KEY_RESULTS = "results";

  @Override
  public Object decode(Response response, Type type)
      throws IOException, DecodeException, FeignException {
    if (response.status() == 404
        || response.status() == 204
        || !type.equals(RestppStreamResponse.class)) return Util.emptyValueOf(type);
    // Only works for RestppStreamResponse
    if (response.body() == null) return null;
    Reader reader = response.body().asReader(response.charset());
    if (!reader.markSupported()) {
      reader = new BufferedReader(reader, 1);
    }
    try {
      // Read the first byte to see if we have any data
      reader.mark(1);
      // Eagerly returning null avoids "No content to map due to end-of-input"
      if (reader.read() == -1) {
        return null;
      }
      reader.reset();
      return parse(reader);
    } catch (Exception e) {
      if (e.getCause() != null && e.getCause() instanceof IOException) {
        throw IOException.class.cast(e.getCause());
      }
      throw e;
    }
  }

  /**
   * Parse the first level of the response reader and build {@link RestppStreamResponse}
   *
   * @param reader
   * @return {@link RestppStreamResponse}
   * @throws IOException
   */
  private RestppStreamResponse parse(Reader reader) throws IOException {
    JsonFactory factory = new JsonFactory();
    RestppStreamResponse resp = new RestppStreamResponse();
    JsonParser parser = factory.createParser(reader);
    parser.setCodec(RestppDecoder.MAPPER);
    // Move the parser to the first token of the JSON response
    if (!JsonToken.START_OBJECT.equals(parser.nextToken())) {
      return null;
    }
    // Process the JSON object til end ('}')
    while (!JsonToken.END_OBJECT.equals(parser.nextToken())) {
      // Check the current token(fieldName) and retain the expected ones
      if (JsonToken.FIELD_NAME.equals(parser.currentToken())) {
        String fieldName = parser.getCurrentName();
        // Move to maybe the value token
        parser.nextToken();
        if (KEY_CODE.equals(fieldName)) {
          resp.code = parser.getValueAsString();
        } else if (KEY_ERROR.equals(fieldName)) {
          resp.error = Boolean.parseBoolean(parser.getValueAsString());
        } else if (KEY_MESSAGE.equals(fieldName)) {
          resp.message = parser.getValueAsString();
        } else if (KEY_RESULTS.equals(fieldName)) {
          // The results should be a JSON array, otherwise discard it
          // If already got "error"=true before "results", discard "results" and proceed to "code"
          if (!resp.error && JsonToken.START_ARRAY.equals(parser.currentToken())) {
            resp.results = parser;
            break;
          }
        }
      }
      // For unexpected nest fields, skip the entire children
      parser.skipChildren();
    }
    return resp;
  }
}
