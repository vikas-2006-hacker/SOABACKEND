package com.klef.soa.sdp.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AttendanceThresholdPolicyTest {
    private final AttendanceThresholdPolicy policy = new AttendanceThresholdPolicy(90, 80, 75);

    @Test
    void classifiesAtCentralizedThresholdBoundaries() {
        assertEquals("EXCELLENT", policy.status(10, 90));
        assertEquals("GOOD", policy.status(10, 80));
        assertEquals("WARNING", policy.status(10, 75));
        assertEquals("CRITICAL", policy.status(10, 74.99));
        assertEquals("INSUFFICIENT_DATA", policy.status(0, 100));
        assertEquals("LOW", policy.risk(10, 80));
        assertEquals("MODERATE", policy.risk(10, 75));
        assertEquals("HIGH", policy.risk(10, 74.99));
        assertEquals("UNKNOWN", policy.risk(0, 0));
    }

    @Test
    void determinesEligibilityFromConfiguredWarningThreshold() {
        assertFalse(policy.examEligible(0, 0));
        assertTrue(policy.examEligible(4, 75));
        assertFalse(policy.examEligible(4, 74.99));
    }

    @Test
    void rejectsOutOfRangeNonFiniteAndUnorderedThresholds() {
        assertThrows(IllegalStateException.class, () -> new AttendanceThresholdPolicy(90, 80, -1).validateThresholds());
        assertThrows(IllegalStateException.class, () -> new AttendanceThresholdPolicy(101, 80, 75).validateThresholds());
        assertThrows(IllegalStateException.class, () -> new AttendanceThresholdPolicy(90, 75, 75).validateThresholds());
        assertThrows(IllegalStateException.class, () -> new AttendanceThresholdPolicy(Double.NaN, 80, 75).validateThresholds());
    }
}
