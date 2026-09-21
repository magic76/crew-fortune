package com.crewpocket.fortune;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Stable, non-PII key for a birth profile + fortune system. */
final class FortuneReadingId {
    private FortuneReadingId() {}

    static String from(FortunePreset preset) {
        if (preset == null) return "";
        return sha256(preset.key());
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(
                    (value == null ? "" : value)
                            .getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) {
                out.append(String.format(java.util.Locale.US, "%02x", b & 0xff));
            }
            return out.toString();
        } catch (Exception error) {
            throw new IllegalStateException("Unable to create reading id", error);
        }
    }
}
