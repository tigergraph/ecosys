package com.tigergraph.spark.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tigergraph.spark.util.OptionDef.ValidVersion;
import org.junit.jupiter.api.Test;

public class OptionDefTest {
  @Test
  public void testVersionValidator() {
    ValidVersion validator = ValidVersion.INSTANCE;
    assertDoesNotThrow(() -> validator.ensureValid("version", "3.1.1"));
    assertDoesNotThrow(() -> validator.ensureValid("version", "99.99.99"));
    assertDoesNotThrow(() -> validator.ensureValid("version", "0.0.0"));
    assertThrows(IllegalArgumentException.class, () -> validator.ensureValid("version", "3.1"));
    assertThrows(IllegalArgumentException.class, () -> validator.ensureValid("version", "3.1.1.1"));
    assertThrows(IllegalArgumentException.class, () -> validator.ensureValid("version", ".1.1"));
    assertThrows(IllegalArgumentException.class, () -> validator.ensureValid("version", "3..1"));
  }
}
