package com.crewpocket.fortune;

import com.magic76.crew.agent.AgentHarness;
import com.magic76.crew.agent.ModelSession;

public final class FortuneAgentRuntime {
    private FortuneAgentRuntime() {}

    public static AgentHarness create(ModelSession session, AgentHarness.Listener listener) {
        FortuneEngine engine = new FortuneEngine();
        return new AgentHarness(
                new FortuneAgentSpec(),
                session,
                FortuneToolRegistry.create(engine),
                listener);
    }
}
