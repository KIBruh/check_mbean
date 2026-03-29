package io.github.kibruh;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StringThresholdTest {

    @Test
    void testMatch_Positive() {
        Threshold threshold = Threshold.parse("OK", NaemonStatus.CRITICAL, true);
        assertTrue(threshold.isViolated("OK"));
        assertFalse(threshold.isViolated("ERROR"));
    }

    @Test
    void testMatch_Negated() {
        Threshold threshold = Threshold.parse("!OK", NaemonStatus.CRITICAL, true);
        assertFalse(threshold.isViolated("OK"));
        assertTrue(threshold.isViolated("ERROR"));
        
        Threshold threshold2 = Threshold.parse("~OK", NaemonStatus.CRITICAL, true);
        assertFalse(threshold2.isViolated("OK"));
        assertTrue(threshold2.isViolated("ERROR"));
    }

    @Test
    void testMatch_Regex() {
        Threshold threshold = Threshold.parse("^[0-9]+$", NaemonStatus.WARNING, true);
        assertTrue(threshold.isViolated("123"));
        assertFalse(threshold.isViolated("123a"));
    }

    @Test
    void testMatch_CaseSensitiveByDefault() {
        Threshold threshold = Threshold.parse("ok", NaemonStatus.WARNING, true);
        assertTrue(threshold.isViolated("ok"));
        assertFalse(threshold.isViolated("OK"));
    }

    @Test
    void testParse_InvalidRegex() {
        assertThrows(IllegalArgumentException.class, () -> Threshold.parse("[", NaemonStatus.WARNING, true));
    }
}
