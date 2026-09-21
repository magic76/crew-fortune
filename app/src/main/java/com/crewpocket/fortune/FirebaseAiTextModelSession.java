package com.crewpocket.fortune;

import android.content.Context;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.RequestOptions;
import com.magic76.crew.agent.ModelEvent;
import com.magic76.crew.agent.ModelSession;
import com.magic76.crew.agent.SessionConfig;
import com.magic76.crew.agent.ToolResult;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Production text adapter using Firebase AI Logic.
 *
 * The Gemini credential stays behind Firebase's proxy and is never embedded in the APK.
 * Fortune interpretation intentionally exposes no model tools, so one request is enough.
 */
final class FirebaseAiTextModelSession implements ModelSession {
    static final String MODEL = "gemini-3.8-flash";

    private final Context context;
    private final float temperature;
    private final Executor callbackExecutor = Executors.newSingleThreadExecutor();

    private SessionConfig config;
    private Listener listener;
    private volatile ListenableFuture<GenerateContentResponse> activeFuture;
    private volatile boolean started;
    private volatile boolean closed;

    FirebaseAiTextModelSession(Context context, double temperature) {
        this.context = context.getApplicationContext();
        this.temperature = (float) Math.max(0.1, Math.min(1.4, temperature));
    }

    static boolean isConfigured(Context context) {
        if (context == null) return false;
        try {
            FirebaseApp app = FirebaseApp.initializeApp(context.getApplicationContext());
            return app != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override public synchronized void start(
            SessionConfig config,
            Listener listener) {
        if (closed) throw new IllegalStateException("session is closed");
        if (started) return;
        if (!isConfigured(context)) {
            throw new IllegalStateException("Firebase AI Logic is not configured");
        }
        if (config == null) throw new IllegalArgumentException("session config is null");
        if (listener == null) throw new IllegalArgumentException("listener is null");
        if (config.tools() != null && !config.tools().isEmpty()) {
            throw new IllegalArgumentException(
                    "Firebase fortune interpretation does not expose tools");
        }
        this.config = config;
        this.listener = listener;
        this.started = true;
    }

    @Override public void sendUserText(String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) return;
        requireRunning();

        try {
            GenerationConfig generationConfig = new GenerationConfig.Builder()
                    .setTemperature(temperature)
                    .setMaxOutputTokens(9000)
                    .setResponseMimeType("application/json")
                    .build();

            Content systemInstruction = new Content.Builder()
                    .addText(config.systemPrompt())
                    .build();

            GenerativeModel model = FirebaseAI
                    .getInstance(GenerativeBackend.googleAI())
                    .generativeModel(
                            MODEL,
                            generationConfig,
                            null,
                            null,
                            null,
                            systemInstruction,
                            new RequestOptions(60000L, 1));

            GenerativeModelFutures futures =
                    GenerativeModelFutures.from(model);
            Content prompt = new Content.Builder()
                    .addText(value)
                    .build();
            ListenableFuture<GenerateContentResponse> future =
                    futures.generateContent(prompt);
            activeFuture = future;

            Futures.addCallback(
                    future,
                    new FutureCallback<GenerateContentResponse>() {
                        @Override public void onSuccess(
                                GenerateContentResponse response) {
                            if (closed) return;
                            String result = response == null
                                    ? ""
                                    : response.getText();
                            if (result == null || result.trim().isEmpty()) {
                                emit(ModelEvent.error(
                                        new IllegalStateException(
                                                "Firebase AI returned empty text")));
                                return;
                            }
                            emit(ModelEvent.text(result));
                            emit(ModelEvent.turnCompleted());
                        }

                        @Override public void onFailure(Throwable error) {
                            if (closed) return;
                            emit(ModelEvent.error(
                                    error instanceof Exception
                                            ? (Exception) error
                                            : new RuntimeException(error)));
                        }
                    },
                    callbackExecutor);
        } catch (Exception error) {
            emit(ModelEvent.error(error));
        }
    }

    @Override public void sendUserAudio(byte[] audio) {
        emit(ModelEvent.error(
                new UnsupportedOperationException(
                        "Crew Fortune interpretation is text-only")));
    }

    @Override public void sendToolResult(ToolResult result) {
        emit(ModelEvent.error(
                new UnsupportedOperationException(
                        "Fortune interpretation does not use model tools")));
    }

    @Override public void interrupt() {
        ListenableFuture<GenerateContentResponse> future = activeFuture;
        if (future != null) future.cancel(true);
        emit(ModelEvent.interrupted());
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        started = false;
        ListenableFuture<GenerateContentResponse> future = activeFuture;
        if (future != null) future.cancel(true);
        activeFuture = null;
        listener = null;
    }

    String lastModel() {
        return "firebase-ai:" + MODEL;
    }

    String lastFinishReason() {
        return "";
    }

    String lastModelAttempts() {
        return lastModel();
    }

    private void requireRunning() {
        if (!started || closed) {
            throw new IllegalStateException("session is not running");
        }
    }

    private void emit(ModelEvent event) {
        Listener target = listener;
        if (target != null && event != null) {
            target.onModelEvent(event);
        }
    }
}
