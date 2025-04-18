package com.tigergraph.spark.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
}
