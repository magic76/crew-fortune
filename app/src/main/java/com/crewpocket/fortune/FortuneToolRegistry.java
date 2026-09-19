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
                    FortuneProfile profile = new FortuneProfile(
                            text(args.get("name")),
                            text(args.get("birthDate")),
                            text(args.get("birthTime")),
                            text(args.get("gender")));
                    FortuneFacts facts = engine.calculateFacts(mode, profile, new Date());

                    Map<String, Object> payload = new LinkedHashMap<String, Object>();
                    payload.put("mode", facts.mode.name());
                    payload.put("basis", facts.basis);
                    payload.put("details", facts.details);
                    payload.put("interpretationRule",
                            "All returned calculations are immutable. Explain them in fresh Traditional Chinese wording; never invent or alter pillars, gods, luck cycles, numerology numbers, or tarot cards.");
                    completion.complete(ToolResult.success(call.id(), payload));
                } catch (Exception error) {
                    completion.complete(ToolResult.failure(call.id(), "FORTUNE_INPUT_ERROR",
                            error.getMessage() == null ? "Invalid fortune input" : error.getMessage()));
                }
            }
        });
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
