package com.crewpocket.fortune;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * First-release local entitlement store.
 *
 * A purchased report is written before its Play purchase is consumed. If AI generation fails,
 * the consumable stays owned and can be retried instead of silently losing the purchase.
 */
final class FortunePaidReadingStore {
    private static final String PREFS = "crew_fortune_paid_readings";
    private static final String REPORT_PREFIX = "report_";
    private static final String KEY_PENDING_READING = "pending_reading";
    private static final String KEY_PENDING_PURCHASE_TOKEN = "pending_purchase_token";

    private FortunePaidReadingStore() {}

    static void saveReport(
            Context context,
            String readingId,
            AiFortuneCopy copy) {
        if (context == null || clean(readingId).isEmpty() || copy == null) return;
        prefs(context).edit()
                .putString(REPORT_PREFIX + readingId, copy.toJson())
                .apply();
    }

    static AiFortuneCopy loadReport(Context context, String readingId) {
        if (context == null || clean(readingId).isEmpty()) return null;
        String raw = prefs(context).getString(
                REPORT_PREFIX + readingId,
                "");
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return AiFortuneCopy.parse(raw);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static boolean hasReport(Context context, String readingId) {
        return loadReport(context, readingId) != null;
    }

    static void beginPurchase(Context context, String readingId) {
        prefs(context).edit()
                .putString(KEY_PENDING_READING, clean(readingId))
                .remove(KEY_PENDING_PURCHASE_TOKEN)
                .apply();
    }

    static void attachPurchaseToken(Context context, String token) {
        prefs(context).edit()
                .putString(KEY_PENDING_PURCHASE_TOKEN, clean(token))
                .apply();
    }

    static String pendingReadingId(Context context) {
        return clean(prefs(context).getString(KEY_PENDING_READING, ""));
    }

    static String pendingPurchaseToken(Context context) {
        return clean(prefs(context).getString(
                KEY_PENDING_PURCHASE_TOKEN,
                ""));
    }

    static boolean isPendingFor(Context context, String readingId) {
        return !clean(readingId).isEmpty()
                && clean(readingId).equals(pendingReadingId(context));
    }

    static void clearPending(Context context) {
        prefs(context).edit()
                .remove(KEY_PENDING_READING)
                .remove(KEY_PENDING_PURCHASE_TOKEN)
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
