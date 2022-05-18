package com.tigergraph.jdbc.log;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.*;

public class UtilTest {

    @Test
    public void shouldConvertToJULFormat0() {
        assertEquals("Hello {0}, this is {1}", Util.formatConverter("Hello {}, this is {}"));
    }

    @Test
    public void shouldConvertToJULFormat1() {
        assertEquals("Hello {{0}}}, this is {1}", Util.formatConverter("Hello {{}}}, this is {}"));
    }

    @Test
    public void shouldConvertToJULFormat2() {
        assertEquals("Hello }, this is {0}", Util.formatConverter("Hello }, this is {}"));
    }

    @Test
    public void shouldConvertToJULFormat4() {
        assertEquals("Hello {, this is {0}", Util.formatConverter("Hello {, this is {}"));
    }

    @Test
    public void shouldConvertToJULFormat5() {
        assertEquals("Hello {0}, this is {1}", Util.formatConverter("Hello {0}, this is {1}"));
    }

    @Test
    public void shouldConvertToJULFormat7() {
        assertEquals("Hello {0}, this is {", Util.formatConverter("Hello {}, this is {"));
    }

    @Test
    public void shouldConvertToJULFormat6() {
        assertEquals("Hello {0}, this is {", Util.formatConverter("Hello {0}, this is {"));
    }
}
