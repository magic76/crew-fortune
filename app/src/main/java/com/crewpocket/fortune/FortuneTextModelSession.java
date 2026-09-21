package com.crewpocket.fortune;

import android.content.Context;

import com.magic76.crew.agent.ModelSession;
import com.magic76.crew.agent.SessionConfig;
import com.magic76.crew.agent.ToolResult;

/**
 * Chooses the commercial Firebase AI Logic transport when configured.
 * A locally stored Gemini key remains a developer fallback only.
 */
final class FortuneTextModelSession implements ModelSession {
    private final ModelSession delegate;
    private final FirebaseAiTextModelSession firebase;
    private final GeminiTextModelSession byok;

    FortuneTextModelSession(Context context, double temperature) {
        if (FirebaseAiTextModelSession.isConfigured(context)) {
            firebase = new FirebaseAiTextModelSession(context, temperature);
            byok = null;
            delegate = firebase;
        } else {
            firebase = null;
            byok = new GeminiTextModelSession(
                    AppConfig.getGeminiApiKey(context),
                    temperature);
            delegate = byok;
        }
    }

    static boolean hasProductionAi(Context context) {
        return FirebaseAiTextModelSession.isConfigured(context);
    }

    static boolean canGenerate(Context context) {
        return hasProductionAi(context)
                || AppConfig.hasGeminiApiKey(context);
    }

    static boolean usesDeveloperKey(Context context) {
        return !hasProductionAi(context)
                && AppConfig.hasGeminiApiKey(context);
    }

    @Override public void start(SessionConfig config, Listener listener) {
        delegate.start(config, listener);
    }

    @Override public void sendUserText(String text) {
        delegate.sendUserText(text);
    }

    @Override public void sendUserAudio(byte[] audio) {
        delegate.sendUserAudio(audio);
    }

    @Override public void sendToolResult(ToolResult result) {
        delegate.sendToolResult(result);
    }

    @Override public void interrupt() {
        delegate.interrupt();
    }

    @Override public void close() {
        delegate.close();
    }

    String lastFinishReason() {
        return firebase != null
                ? firebase.lastFinishReason()
                : byok.lastFinishReason();
    }

    String lastModel() {
        return firebase != null
                ? firebase.lastModel()
                : byok.lastModel();
    }

    String lastModelAttempts() {
        return firebase != null
                ? firebase.lastModelAttempts()
                : byok.lastModelAttempts();
    }
}
