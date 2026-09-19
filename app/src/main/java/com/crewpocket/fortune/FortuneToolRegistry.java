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
                            text(args.get("secondaryName")));
                    FortuneFacts facts = engine.calculateFacts(mode, profile, new Date());

                    Map<String, Object> payload = new LinkedHashMap<String, Object>();
                    payload.put("mode", facts.mode.name());
                    payload.put("score", facts.score);
                    payload.put("basis", facts.basis);
                    payload.put("lifeNumber", facts.lifeNumber);
                    payload.put("zodiac", facts.zodiac);
                    payload.put("nameNumber", facts.nameNumber);
                    if (facts.secondaryNameNumber > 0) {
                        payload.put("secondaryNameNumber", facts.secondaryNameNumber);
                    }
                    payload.put("momentum", facts.momentum);
                    payload.put("stability", facts.stability);
                    payload.put("social", facts.social);
                    payload.put("impulse", facts.impulse);
                    payload.put("dayKey", facts.dayKey);
                    payload.put("interpretationRule",
                            "These values are deterministic facts. Create fresh wording from them; do not change numeric values.");
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
