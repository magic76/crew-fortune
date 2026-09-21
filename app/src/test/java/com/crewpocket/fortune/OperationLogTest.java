package com.crewpocket.fortune;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class OperationLogTest {
    @Test public void redactsBirthAndContactDetails() {
        String sanitized = OperationLog.sanitizeDetail(
                "CALCULATE_FAILED",
                "1985-07-06 04:10 mail=a@example.com lat=25.012345 token=abcdefghijklmnopqrstuvwx1234");
        assertFalse(sanitized.contains("1985-07-06"));
        assertFalse(sanitized.contains("04:10"));
        assertFalse(sanitized.contains("a@example.com"));
        assertFalse(sanitized.contains("25.012345"));
        assertFalse(sanitized.contains("abcdefghijklmnopqrstuvwx1234"));
    }

    @Test public void hidesPresetAndBirthPlaceDetailsEntirely() {
        assertEquals("[preset]", OperationLog.sanitizeDetail(
                "PRESET_SAVED", "八字 · 1985-07-06 04:10"));
        assertEquals("[location]", OperationLog.sanitizeDetail(
                "BIRTH_PLACE_SELECTED", "Taipei 25.0, 121.5"));
        assertEquals("[location]", OperationLog.sanitizeDetail(
                "VEDIC_CITY_SEARCH_START", "Bangkok"));
        assertEquals("[location]", OperationLog.sanitizeDetail(
                "VEDIC_CITY_SELECTED", "Bangkok 13.7563, 100.5018"));
        assertEquals("[location]", OperationLog.sanitizeDetail(
                "VEDIC_TIMEZONE_SELECTED", "Asia/Bangkok"));
    }
}
