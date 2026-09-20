package com.crewpocket.fortune;

import com.magic76.crew.agent.ToolCall;
import com.magic76.crew.agent.ToolExecutor;
import com.magic76.crew.agent.ToolRegistry;
import com.magic76.crew.agent.ToolResult;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FortuneToolRegistry {
    private FortuneToolRegistry() {}

    public static ToolRegistry create(final FortuneEngine engine) {
        return new ToolRegistry().register("calculate_fortune", new ToolExecutor() {
            @Override public void execute(ToolCall call, Completion completion) {
                try {
                    Map<String, Object> args = call.arguments();
                    FortuneMode mode = FortuneMode.valueOf(text(args.get("mode")).toUpperCase());
                    BirthPlace birthPlace = null;
                    if (mode == FortuneMode.VEDIC_ASTROLOGY) {
                        birthPlace = new BirthPlace(
                                text(args.get("birthPlace")),
                                number(args.get("latitude"), "latitude"),
                                number(args.get("longitude"), "longitude"),
                                text(args.get("timeZoneId")));
                    }
                    FortuneProfile profile = new FortuneProfile(
                            text(args.get("name")),
                            text(args.get("birthDate")),
                            text(args.get("birthTime")),
                            text(args.get("gender")),
                            birthPlace);
                    FortuneFacts facts = engine.calculateFacts(mode, profile, new Date());

                    Map<String, Object> payload = new LinkedHashMap<String, Object>();
                    payload.put("mode", facts.mode.name());
                    payload.put("basis", facts.basis);
                    payload.put("details", facts.details);
                    payload.put("interpretationRule",
                            "All returned calculations are immutable. Never invent or alter pillars, gods, luck cycles, numerology numbers, tarot cards, planets, houses, nakshatras or dasha periods.");
                    completion.complete(ToolResult.success(call.id(), payload));
                } catch (Exception error) {
                    completion.complete(ToolResult.failure(call.id(), "FORTUNE_INPUT_ERROR",
                            error.getMessage() == null ? "Invalid fortune input" : error.getMessage()));
                }
            }
        });
    }

    private static double number(Object value, String label) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(text(value)); }
        catch (Exception error) {
            throw new IllegalArgumentException("VEDIC_ASTROLOGY requires numeric " + label);
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
