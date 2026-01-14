package com.tigergraph.spark.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class UtilsTest {

  @Test
  public void testExtractVersion() {
    String input1 =
        "TigerGraph RESTPP:\n"
            + "        --- Version ---\n"
            + "       TigerGraph version: 3.9.3\n"
            + "       product          tg_3.9.3_dev                    "
            + " 559b6532eaf39c8e8074d2fa4960bb5158424aaa  2023-09-28 19:58:18 -0700\n"
            + "       cqrs             tg_3.9.3_dev                    "
            + " 4fcd48bd393e9c63016e01bc2783da0311ed9b38  2023-10-04 15:20:34 -0700\n";

    String input2 =
        "TigerGraph RESTPP:\n"
            + "        --- Version ---\n"
            + "       TigerGraph version: tg_3.9.3_dev\n"
            + "       product          tg_3.9.3_dev                    "
            + " 559b6532eaf39c8e8074d2fa4960bb5158424aaa  2023-09-28 19:58:18 -0700\n"
            + "       cqrs             tg_3.9.3_dev                    "
            + " 4fcd48bd393e9c63016e01bc2783da0311ed9b38  2023-10-04 15:20:34 -0700\n";

    String input3 =
        "TigerGraph RESTPP:\n"
            + "        --- Version ---\n"
            + "       TigerGraph version: \n"
            + "       product          tg_3.9.3_dev                    "
            + " 559b6532eaf39c8e8074d2fa4960bb5158424aaa  2023-09-28 19:58:18 -0700\n"
            + "       cqrs             tg_3.6.4_dev                    "
            + " 4fcd48bd393e9c63016e01bc2783da0311ed9b38  2023-10-04 15:20:34 -0700\n";

    String input4 =
        "TigerGraph RESTPP:\n"
            + "        --- Version ---\n"
            + "       TigerGraph version: \n"
            + "       product          release-3.9.3-2023                    "
            + " 559b6532eaf39c8e8074d2fa4960bb5158424aaa  2023-09-28 19:58:18 -0700\n"
            + "       cqrs             release-3.9.3-2023                    "
            + " 4fcd48bd393e9c63016e01bc2783da0311ed9b38  2023-10-04 15:20:34 -0700\n";

    String input5 = "";
    String input6 = "3.9_5.1_4.2.2";

    assertEquals("3.9.3", Utils.extractVersion(input1));
    assertEquals("3.9.3", Utils.extractVersion(input2));
    assertEquals("3.9.3", Utils.extractVersion(input3));
    assertEquals("3.9.3", Utils.extractVersion(input4));
    assertEquals(Utils.DEFAULT_VERSION, Utils.extractVersion(input5));
    assertEquals("4.2.2", Utils.extractVersion(input6));
  }

  @Test
  public void testVersionCmp() {
    assertTrue(Utils.versionCmp("3.9.3", "3.9.2") > 0);
    assertTrue(Utils.versionCmp("3.09.3", "3.9.2") > 0);
    assertTrue(Utils.versionCmp("3.9.3", "3.10.2") < 0);
    assertTrue(Utils.versionCmp("8.8.8", "008.008.008") == 0);
    assertTrue(Utils.versionCmp("3.9", "3.8.3") > 0);
    assertTrue(Utils.versionCmp("3.9", "3.9.2") < 0);
    assertTrue(Utils.versionCmp("3.9", "3.9.0") == 0);
  }

  @Test
  public void testIsEmpty() {
    assertTrue(Utils.isEmpty(null));
    assertTrue(Utils.isEmpty(""));
    assertTrue(!Utils.isEmpty(" "));
  }

  @Test
  public void testMaskString() {
    assertEquals("abcde", Utils.maskString("abcde", 3));
    assertEquals("ab*de", Utils.maskString("abcde", 2));
    assertEquals("a***e", Utils.maskString("abcde", 1));
    assertEquals("*****", Utils.maskString("abcde", 0));
    assertEquals("a********b", Utils.maskString("a12345678987654321b", 1));
  }

  @Test
  public void testRemoveUserData() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    ArrayList<String> originals = new ArrayList<>();
    originals.add(
        "{\"key1\": {\"lineData\": {\"key2\": \"value2\"}}, \"key3\":"
            + " {\"invalidAttributeLinesData\": [{\"attr1\": \"value1\"}, {\"attr2\":"
            + " \"value2\"}]}, \"lineData\": {\"key4\": \"value4\"}}");
    originals.add(
        "{\"key1\": 1, \"lineData\": [1, 2, 4], \"nested\":"
            + " {\"lineData\":{\"key2\":\"value2\"}},\"key3\":{\"invalidAttributeLinesData\":[{\"attr1\":\"value1\"},{\"attr2\":\"value2\"}]},\"key5\":{\"key4\":\"value4\"}}");
    originals.add(
        "{\"vertex\": [{\"count\": 1, \"lineData\": [1,2,3]}, {\"count\": 1, \"lineData\":"
            + " [1,2,3]}]}");
    ArrayList<String> transformed = new ArrayList<>();
    transformed.add("{\"key1\":{}, \"key3\": {}}");
    transformed.add("{\"key1\": 1, \"nested\": {},\"key3\":{},\"key5\":{\"key4\":\"value4\"}}");
    transformed.add("{\"vertex\": [{\"count\": 1}, {\"count\": 1}]}");
    for (int i = 0; i < originals.size(); i++) {
      JsonNode original = mapper.readTree(originals.get(i));
      Utils.removeUserData(original);
      // System.out.println(original.toPrettyString());
      assertTrue(original.equals(mapper.readTree(transformed.get(i))));
    }
  }

  @Test
  public void testExtractQueryFields() {
    // Test some special characters
    List<String> inputs =
        Arrays.asList(
            "A. 234]", "B/89#", "  x[hello", "1 ->2 ->3 ->4 ", "Comment|a.b|c)d| e$g", "abc", "");
    List<String> seps = Arrays.asList(".", "/", "[", "->", "|", ":", ")");
    List<String> expected =
        Arrays.asList(
            "[A,  234]]",
            "[B, 89#]",
            "[  x, hello]",
            "[1 , 2 , 3 , 4 ]",
            "[Comment, a.b, c)d,  e$g]",
            "[abc]",
            "[]");
    for (int i = 0; i < inputs.size(); i++) {
      assertEquals(
          expected.get(i), Utils.extractQueryFields(inputs.get(i), seps.get(i)).toString());
    }
  }

  private static Stream<Arguments> keyValueParserTestCases() {
    return Stream.of(
        Arguments.of(
            "Should handle quoted keys and values",
            "`key, with comma`: `value: with colon`, id: 123",
            Map.of("key, with comma", "value: with colon", "id", "123"),
            null,
            null),
        Arguments.of(
            "Should handle whitespace and mixed quotes",
            "  ` a ` : ` b `  , c:d ",
            Map.of("a", "b", "c", "d"),
            null,
            null),
        Arguments.of(
            "Should parse a single pair",
            "singleKey:`singleValue with spaces`",
            Map.of("singleKey", "singleValue with spaces"),
            null,
            null),
        Arguments.of(
            "Should parse complex mixed input",
            "path: /api/v1/data, `query:param`: `filter=id,name`",
            Map.of("path", "/api/v1/data", "query:param", "filter=id,name"),
            null,
            null),
        Arguments.of(
            "Should handle escaped backticks in key and value",
            "`key``1`: `value``1`, key2: value2",
            Map.of("key`1", "value`1", "key2", "value2"),
            null,
            null),
        Arguments.of("Should parse pair with empty value", "key:", Map.of("key", ""), null, null),
        Arguments.of(
            "Should parse pair with explicit empty quoted value",
            "key:``",
            Map.of("key", ""),
            null,
            null),
        Arguments.of("Should handle null input", null, Map.of(), null, null),
        Arguments.of("Should handle empty input", "", Map.of(), null, null),
        Arguments.of("Should handle whitespace-only input", "   ", Map.of(), null, null),

        // Invalid Cases
        Arguments.of(
            "Should throw for unclosed quote",
            "key: `unclosed quote",
            null,
            IllegalArgumentException.class,
            "Malformed input: Unclosed quote at end of string. Input: \"key: `unclosed quote\""),
        Arguments.of(
            "Should throw for trailing comma",
            "key: value,",
            null,
            IllegalArgumentException.class,
            "Malformed input: Trailing comma at end of string. Input: \"key: value,\""),
        Arguments.of(
            "Should throw for dangling key",
            "key: value, dangling",
            null,
            IllegalArgumentException.class,
            "Malformed input: Dangling key without a value. Input: \"key: value, dangling\""));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("keyValueParserTestCases")
  void testParseKeyValueString(
      String testCase,
      String input,
      Map<String, String> expected,
      Class<Throwable> exception,
      String error) {
    if (exception != null) {
      Throwable e = assertThrows(exception, () -> Utils.parseKeyValueString(input));
      assertEquals(error, e.getMessage());
    } else {
      assertEquals(expected, Utils.parseKeyValueString(input));
    }
  }

  private static Stream<Arguments> commaDelimitedKeysTestCases() {
    return Stream.of(
        Arguments.of(
            "Should parse simple field names",
            "id,name,value",
            List.of("id", "name", "value"),
            null,
            null),
        Arguments.of(
            "Should parse backquoted field names",
            "`field one`,field_two,`field three`",
            List.of("field one", "field_two", "field three"),
            null,
            null),
        Arguments.of(
            "Should parse mixed quoted and unquoted fields",
            "normal_field,`quoted field`,field3",
            List.of("normal_field", "quoted field", "field3"),
            null,
            null),
        Arguments.of(
            "Should handle quoted field with comma",
            "`field,with,commas`,normal_field",
            List.of("field,with,commas", "normal_field"),
            null,
            null),
        Arguments.of(
            "Should handle escaped backticks",
            "`a``b`,c,`d``e``f`",
            List.of("a`b", "c", "d`e`f"),
            null,
            null),
        Arguments.of("Should handle null input", null, List.of(), null, null),
        Arguments.of("Should handle empty input", "", List.of(), null, null),
        Arguments.of("Should handle whitespace-only input", "   ", List.of(), null, null),

        // Invalid Cases
        Arguments.of(
            "Should throw for empty field name",
            "id1,,id2",
            null,
            IllegalArgumentException.class,
            "Malformed input: Empty field name at position 1. Input: \"id1,,id2\""),
        Arguments.of(
            "Should throw for unclosed quote",
            "field1,`unclosed quote",
            null,
            IllegalArgumentException.class,
            "Malformed input: Unclosed quote at end of string. Input: \"field1,`unclosed quote\""));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("commaDelimitedKeysTestCases")
  void testParseCommaDelimitedKeys(
      String testCase,
      String input,
      List<String> expected,
      Class<Throwable> exception,
      String error) {
    if (exception != null) {
      Throwable e = assertThrows(exception, () -> Utils.parseCommaDelimitedKeys(input));
      assertEquals(error, e.getMessage());
    } else {
      assertEquals(expected, Utils.parseCommaDelimitedKeys(input));
    }
  }
}
