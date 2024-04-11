package com.tigergraph.spark.client.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonParseException;
import feign.Request;
import feign.Request.HttpMethod;
import feign.RequestTemplate;
import feign.Response;
import feign.Response.Builder;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class RestppStreamDecoderTest {
  private static RestppStreamDecoder strmDecoder;
  private static Builder respBuilder;

  @BeforeAll
  static void init() {
    strmDecoder = new RestppStreamDecoder();
    respBuilder =
        Response.builder()
            .request(
                Request.create(
                    HttpMethod.GET,
                    "localhost:80",
                    new HashMap<>(),
                    new byte[0],
                    StandardCharsets.UTF_8,
                    new RequestTemplate()))
            .status(200);
  }

  @Test
  @DisplayName("Positive - basic functionalities on small response body")
  public void testPositiveBasic() throws IOException {
    String body =
        "{\"version\":{\"edition\":\"enterprise\",\"api\":\"v2\",\"schema\":0},\"error\":false,\"message\":\"success\",\"results\":[{\"a\":10},{\"b\":20}]}";
    Response resp = respBuilder.body(body, StandardCharsets.UTF_8).build();

    RestppStreamResponse strmResp =
        (RestppStreamResponse) strmDecoder.decode(resp, RestppStreamResponse.class);
    // expected fields
    assertEquals(strmResp.error, false);
    assertEquals(strmResp.message, "success");
    // must call next() when first time retrieving the result
    assertNull(strmResp.readRow());
    // ensure next element exists and move to it
    assertTrue(strmResp.next());
    // parse first element as JsonNode
    assertEquals(strmResp.readRow().toString(), "{\"a\":10}");
    // ensure next element exists and move to it
    assertTrue(strmResp.next());
    // parse second element as JsonNode
    assertEquals(strmResp.readRow().toString(), "{\"b\":20}");
    // no more elements
    assertFalse(strmResp.next());
    // parser and reader are closed, subsequent calls should return null/false
    assertFalse(strmResp.next());
    assertNull(strmResp.readRow());
  }

  @Test
  @DisplayName("Positive - test parsing large response in streaming way")
  public void testPositiveLargeStream() throws IOException {
    // writer keep appending elements to outputStream, meanwhile, parser reads from inputStream
    PipedInputStream inputStream = new PipedInputStream();
    PipedOutputStream outputStream = new PipedOutputStream(inputStream);
    Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
    Response resp = respBuilder.body(inputStream, null).build();
    writer.write("{\"results\":[");
    writer.flush();
    RestppStreamResponse strmResp =
        (RestppStreamResponse) strmDecoder.decode(resp, RestppStreamResponse.class);
    // 1000000 rows in total, write one then parse one to simulate RESTPP and client.
    for (int i = 0; i < 1000000; i++) {
      writer.write("{\"index\":" + i + "},");
      writer.flush();
      assertTrue(strmResp.next());
      assertEquals("{\"index\":" + i + "}", strmResp.readRow().toString());
    }
    // Sending JsonToken.END_ARRAY(']') marks the end of the writing/reading
    writer.write("{\"index\":-1}]}");
    writer.flush();
    assertTrue(strmResp.next());
    assertEquals("{\"index\":-1}", strmResp.readRow().toString());
    assertFalse(strmResp.next());
    writer.close();
  }

  @Test
  @DisplayName("Negative - connection closed during before finishing reading all results")
  public void testNegativeStreamHanging() throws IOException {
    // writer keep appending elements to outputStream, meanwhile, parser reads from inputStream
    PipedInputStream inputStream = new PipedInputStream();
    PipedOutputStream outputStream = new PipedOutputStream(inputStream);
    Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
    Response resp = respBuilder.body(inputStream, null).build();
    writer.write("{\"results\":[");
    writer.flush();
    RestppStreamResponse strmResp =
        (RestppStreamResponse) strmDecoder.decode(resp, RestppStreamResponse.class);
    writer.write("{\"index\":1},");
    writer.flush();
    assertTrue(strmResp.next());
    assertEquals("{\"index\":1}", strmResp.readRow().toString());
    // Connection closed for some reasons
    writer.close();
    assertThrows(JsonParseException.class, () -> strmResp.next());
  }

  @Test
  @DisplayName("Negative - malformed JSON")
  public void testNegativeMalformed() throws IOException {
    // Malformed data detected at 'decode()'
    String body = "{\"error\":false,{\"message\":\"success\",\"results\":[{\"a\":10},{\"b\":20}]}";
    final Response resp1 = respBuilder.body(body, StandardCharsets.UTF_8).build();
    assertThrows(
        JsonParseException.class, () -> strmDecoder.decode(resp1, RestppStreamResponse.class));
  }

  @Test
  @DisplayName("Negative - Wrong response type")
  public void testWrongRespType() throws IOException {
    // RestppResponse instead of RestppStreamResponse
    String body = "{\"error\":false,\"message\":\"success\",\"results\":[{\"a\":10},{\"b\":20}]}";
    Response resp = respBuilder.body(body, StandardCharsets.UTF_8).build();
    assertNull(strmDecoder.decode(resp, new RestppResponse().getClass()));
  }

  @Test
  @DisplayName("Negative - Wrong results type(not JSON array)")
  public void testWrongResultsType() throws IOException {
    String body = "{\"error\":false,\"message\":\"success\",\"results\":{\"a\":10}}";
    Response resp = respBuilder.body(body, StandardCharsets.UTF_8).build();
    RestppStreamResponse strmResp =
        (RestppStreamResponse) strmDecoder.decode(resp, RestppStreamResponse.class);
    assertFalse(strmResp.next());
  }

  @Test
  @DisplayName("Negative - Empty results array")
  public void testEmptyResultsArr() throws IOException {
    String body = "{\"error\":false,\"message\":\"success\",\"results\":[]}";
    Response resp = respBuilder.body(body, StandardCharsets.UTF_8).build();
    RestppStreamResponse strmResp =
        (RestppStreamResponse) strmDecoder.decode(resp, RestppStreamResponse.class);
    assertFalse(strmResp.next());
  }

  @Test
  @DisplayName("Negative - Missing results field")
  public void testMissingResultsField() throws IOException {
    String body = "{\"error\":false,\"message\":\"success\"}";
    Response resp = respBuilder.body(body, StandardCharsets.UTF_8).build();
    RestppStreamResponse strmResp =
        (RestppStreamResponse) strmDecoder.decode(resp, RestppStreamResponse.class);
    assertFalse(strmResp.next());
  }

  @Test
  @DisplayName("Negative - Get error=true")
  public void testErrorResponse() throws IOException {
    String body =
        "{\"error\":true,\"message\":\"failed to query\", \"results\":[], \"code\":"
            + " \"RESTPP-1234\"}";
    Response resp = respBuilder.body(body, StandardCharsets.UTF_8).build();
    RestppStreamResponse strmResp =
        (RestppStreamResponse) strmDecoder.decode(resp, RestppStreamResponse.class);
    RestppErrorException e = assertThrows(RestppErrorException.class, () -> strmResp.panicOnFail());
    assertEquals(
        e.getMessage(), "RESTPP error response, code: RESTPP-1234, message: failed to query");
  }
}
