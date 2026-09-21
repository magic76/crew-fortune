package com.crewpocket.fortune;

import java.time.LocalDate;
import java.util.Date;

final class FortuneResultState {
    private FortuneResultState() {}

    static String transitDateText(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    static LocalDate parseTransitDate(String raw) {
        String clean = raw == null ? "" : raw.trim();
        if (clean.isEmpty()) return null;
        try {
            return LocalDate.parse(clean);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static Date referenceDate(long millis) {
        return millis > 0L ? new Date(millis) : new Date();
    }
}
