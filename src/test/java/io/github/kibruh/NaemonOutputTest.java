package io.github.kibruh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NaemonOutputTest {

    @Test
    void testFormatNumber_Long() {
        assertEquals("1234567890", NaemonOutput.formatNumber(1234567890L));
        assertEquals("0", NaemonOutput.formatNumber(0L));
        assertEquals("-100", NaemonOutput.formatNumber(-100L));
    }

    @Test
    void testFormatNumber_Double() {
        assertEquals("123.456", NaemonOutput.formatNumber(123.456));
        assertEquals("0", NaemonOutput.formatNumber(0.0));
        assertEquals("-50.5", NaemonOutput.formatNumber(-50.5));
    }

    @Test
    void testFormatNumber_Integer() {
        assertEquals("42", NaemonOutput.formatNumber(42));
        assertEquals("-10", NaemonOutput.formatNumber(-10));
    }

    @Test
    void testFormatNumber_Null() {
        assertEquals("0", NaemonOutput.formatNumber(null));
    }

    @Test
    void testBuild_WithoutPerfData() {
        NaemonOutput output = new NaemonOutput(NaemonStatus.OK, "All good");
        assertEquals("OK - All good", output.build());
    }

    @Test
    void testBuild_WithPerfData() {
        NaemonOutput output = new NaemonOutput(NaemonStatus.WARNING, "Value is 50");
        output.addPerfData("myvalue", 50, "");
        
        String result = output.build();
        assertTrue(result.startsWith("WARNING - Value is 50 |"));
        assertTrue(result.contains("'myvalue'=50"));
    }

    @Test
    void testStaticFactories() {
        assertEquals(NaemonStatus.OK, NaemonOutput.ok("test").getStatus());
        assertEquals(NaemonStatus.WARNING, NaemonOutput.warning("test").getStatus());
        assertEquals(NaemonStatus.CRITICAL, NaemonOutput.critical("test").getStatus());
        assertEquals(NaemonStatus.UNKNOWN, NaemonOutput.unknown("test").getStatus());
    }

    @Test
    void testEscapePerfDataLabel() {
        NaemonOutput output = NaemonOutput.ok("test");
        // The escape is internal, just verify output works
        NaemonOutput output2 = new NaemonOutput(NaemonStatus.OK, "test");
        output2.addPerfData("my.label", 100, "");
        assertTrue(output2.build().contains("'my.label'=100"));
    }

    @Test
    void testPerfDataWithThresholds() {
        Threshold warn = Threshold.parse("10:50", NaemonStatus.WARNING);
        Threshold crit = Threshold.parse("50:100", NaemonStatus.CRITICAL);
        
        NaemonOutput output = new NaemonOutput(NaemonStatus.OK, "Value is 25");
        output.addPerfData("myvalue", 25, "", warn, crit);
        
        String result = output.build();
        assertTrue(result.contains(";10:50;50:100;;"));
    }

    @Test
    void testPerfDataWithUnit() {
        NaemonOutput output = new NaemonOutput(NaemonStatus.OK, "Memory is 1024");
        output.addPerfData("memory", 1024, "B");
        
        assertTrue(output.build().contains("'memory'=1024B"));
    }

    @Test
    void testPerfDataWithCounter() {
        NaemonOutput output = new NaemonOutput(NaemonStatus.OK, "Counter is 100");
        output.addPerfData("counter", 100, "c");
        
        assertTrue(output.build().contains("'counter'=100c"));
    }
}
