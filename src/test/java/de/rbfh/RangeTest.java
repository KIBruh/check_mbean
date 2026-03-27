package de.rbfh;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class RangeTest {

    @ParameterizedTest
    @CsvSource({
        "10, false, 0, 10",
        "20, false, 10, 20",
        "10, true, 0, 10",
        "20, true, 10, 20"
    })
    void testSimpleRange(double value, boolean invert, double start, double end) {
        Range range = new Range(start, end, invert, true, true);
        boolean inside = value >= start && value <= end;
        boolean expected = invert ? !inside : inside;
        assertEquals(expected, range.isInRange(value));
    }

    @Test
    void testParseSimpleNumber() {
        Range range = Range.parse("10");
        assertEquals(0, range.start(), 0.001);
        assertEquals(10, range.end(), 0.001);
        assertFalse(range.invert());

        assertTrue(range.isInRange(5));
        assertTrue(range.isInRange(0));
        assertTrue(range.isInRange(10));
        assertFalse(range.isInRange(-1));
        assertFalse(range.isInRange(15));
    }

    @Test
    void testParseRangeWithColon() {
        Range range = Range.parse("10:20");
        assertEquals(10, range.start(), 0.001);
        assertEquals(20, range.end(), 0.001);

        assertTrue(range.isInRange(10));
        assertTrue(range.isInRange(15));
        assertTrue(range.isInRange(20));
        assertFalse(range.isInRange(5));
        assertFalse(range.isInRange(25));
    }

    @Test
    void testParseInvertRange() {
        Range range = Range.parse("@10:20");

        assertTrue(range.invert());
        assertFalse(range.isInRange(10));
        assertFalse(range.isInRange(15));
        assertFalse(range.isInRange(20));
        assertTrue(range.isInRange(5));
        assertTrue(range.isInRange(25));
    }

    @Test
    void testParseNegativeInfinity() {
        Range range = Range.parse("~:10");

        assertEquals(Double.NEGATIVE_INFINITY, range.start(), 0.001);
        assertEquals(10, range.end(), 0.001);

        assertTrue(range.isInRange(0));
        assertTrue(range.isInRange(10));
        assertFalse(range.isInRange(15));
    }

    @Test
    void testParsePositiveInfinity() {
        Range range = Range.parse("10:~");

        assertEquals(10, range.start(), 0.001);
        assertEquals(Double.POSITIVE_INFINITY, range.end(), 0.001);

        assertFalse(range.isInRange(5));
        assertTrue(range.isInRange(10));
        assertTrue(range.isInRange(100));
    }

    @Test
    void testParseFullInfinity() {
        Range range = Range.parse("~");

        assertEquals(Double.NEGATIVE_INFINITY, range.start(), 0.001);
        assertEquals(Double.POSITIVE_INFINITY, range.end(), 0.001);

        assertTrue(range.isInRange(0));
        assertTrue(range.isInRange(-1000000));
        assertTrue(range.isInRange(1000000));
    }

    @Test
    void testParseInvalidRange() {
        assertThrows(java.lang.IllegalArgumentException.class, () -> Range.parse("20:10"));
        assertThrows(java.lang.IllegalArgumentException.class, () -> Range.parse(""));
    }

    @Test
    void testToString() {
        assertEquals("10:20", Range.parse("10:20").toString());
        assertEquals("@10:20", Range.parse("@10:20").toString());
        assertEquals("~:10", Range.parse("~:10").toString());
        assertEquals("10:~", Range.parse("10:~").toString());
        assertEquals("~:~", Range.parse("~").toString());
    }
}
