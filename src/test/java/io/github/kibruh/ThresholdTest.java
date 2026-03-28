package io.github.kibruh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThresholdTest {

    @Test
    void testViolation_DefaultRange_InsideNotViolated() {
        Threshold threshold = Threshold.parse("100:200", NaemonStatus.WARNING);
        
        assertFalse(threshold.isViolated(100));
        assertFalse(threshold.isViolated(150));
        assertFalse(threshold.isViolated(200));
    }

    @Test
    void testViolation_DefaultRange_OutsideViolates() {
        Threshold threshold = Threshold.parse("100:200", NaemonStatus.WARNING);
        
        assertTrue(threshold.isViolated(50));
        assertTrue(threshold.isViolated(250));
    }

    @Test
    void testViolation_InvertedRange_InsideViolates() {
        Threshold threshold = Threshold.parse("@100:200", NaemonStatus.CRITICAL);
        
        assertTrue(threshold.isViolated(100));
        assertTrue(threshold.isViolated(150));
        assertTrue(threshold.isViolated(200));
    }

    @Test
    void testViolation_InvertedRange_OutsideNotViolated() {
        Threshold threshold = Threshold.parse("@100:200", NaemonStatus.CRITICAL);
        
        assertFalse(threshold.isViolated(50));
        assertFalse(threshold.isViolated(250));
    }

    @Test
    void testViolation_SimpleNumber_InsideNotViolated() {
        Threshold threshold = Threshold.parse("100", NaemonStatus.WARNING);
        
        assertFalse(threshold.isViolated(0));
        assertFalse(threshold.isViolated(50));
        assertFalse(threshold.isViolated(100));
    }

    @Test
    void testViolation_SimpleNumber_OutsideViolates() {
        Threshold threshold = Threshold.parse("100", NaemonStatus.WARNING);
        
        assertTrue(threshold.isViolated(-1));
        assertTrue(threshold.isViolated(101));
    }

    @Test
    void testViolation_NegativeInfinity_InsideNotViolated() {
        Threshold threshold = Threshold.parse("~:100", NaemonStatus.WARNING);
        
        assertFalse(threshold.isViolated(50));
        assertFalse(threshold.isViolated(100));
        assertTrue(threshold.isViolated(150));
    }

    @Test
    void testViolation_PositiveInfinity_InsideNotViolated() {
        Threshold threshold = Threshold.parse("100:~", NaemonStatus.WARNING);
        
        assertTrue(threshold.isViolated(50));
        assertFalse(threshold.isViolated(100));
        assertFalse(threshold.isViolated(150));
    }

    @Test
    void testWarningAndCriticalThresholds() {
        Threshold warning = Threshold.parse("100:200", NaemonStatus.WARNING);
        Threshold critical = Threshold.parse("200:300", NaemonStatus.CRITICAL);
        
        // Value 50 is outside both ranges
        assertTrue(warning.isViolated(50));
        assertTrue(critical.isViolated(50));
        
        // Value 150 is inside warning but outside critical
        assertFalse(warning.isViolated(150));
        assertTrue(critical.isViolated(150));
        
        // Value 250 is outside warning but inside critical
        assertTrue(warning.isViolated(250));
        assertFalse(critical.isViolated(250));
    }
}
