package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.magic76.crew.agent.AgentEvent;
import com.magic76.crew.agent.AgentHarness;
import com.magic76.crew.agent.ModelEvent;
import com.magic76.crew.agent.ModelSession;
import com.magic76.crew.agent.SessionConfig;
import com.magic76.crew.agent.ToolCall;
import com.magic76.crew.agent.ToolResult;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FortuneAgentRuntimeTest {
    @Test public void harnessExecutesFortuneToolBeforeFinalCopy() {
        FakeSession model = new FakeSession();
        final List<AgentEvent.Type> events = new ArrayList<AgentEvent.Type>();
        AgentHarness harness = FortuneAgentRuntime.create(model, new AgentHarness.Listener() {
            @Override public void onAgentEvent(AgentEvent event) {
                events.add(event.type());
            }
        });

        harness.start();
        harness.submitText("幫我算塔羅生命靈數");

        assertTrue(model.receivedToolResult);
        assertEquals("calculate_fortune", model.requestedToolName);
        assertTrue(events.contains(AgentEvent.Type.TOOL_COMPLETED));
        assertTrue(events.contains(AgentEvent.Type.MODEL_TEXT));
        assertTrue(events.contains(AgentEvent.Type.TURN_COMPLETED));
    }

    private static final class FakeSession implements ModelSession {
        Listener listener;
        boolean receivedToolResult;
        String requestedToolName;

        @Override public void start(SessionConfig config, Listener listener) {
            this.listener = listener;
            assertEquals("crew-fortune", config.agentId());
        }

        @Override public void sendUserText(String text) {
            Map<String, Object> args = new LinkedHashMap<String, Object>();
            args.put("mode", "TAROT_NUMEROLOGY");
            args.put("name", "小明");
            args.put("birthDate", "1950-02-21");
            requestedToolName = "calculate_fortune";
            listener.onModelEvent(ModelEvent.toolCall(
                    new ToolCall("call-1", requestedToolName, args)));
        }

        @Override public void sendUserAudio(byte[] audio) {}

        @Override public void sendToolResult(ToolResult result) {
            receivedToolResult = result.success();
            listener.onModelEvent(ModelEvent.text("{\"title\":\"測試\",\"analysis\":\"A\",\"translation\":\"B\",\"punchline\":\"C\",\"advice\":\"D\",\"shareText\":\"E\"}"));
            listener.onModelEvent(ModelEvent.turnCompleted());
        }

        @Override public void interrupt() {}
        @Override public void close() {}
    }
}
