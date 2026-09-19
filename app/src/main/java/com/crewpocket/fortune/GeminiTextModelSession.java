package com.crewpocket.fortune;

import com.magic76.crew.agent.ModelEvent;
import com.magic76.crew.agent.ModelSession;
import com.magic76.crew.agent.SessionConfig;
import com.magic76.crew.agent.ToolCall;
import com.magic76.crew.agent.ToolResult;
import com.magic76.crew.agent.ToolSpec;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Text-only Gemini adapter for Crew Agent Harness.
 *
 * Fortune uses a normal generateContent loop instead of Gemini Live because the product only needs
 * short text generation. The Harness still owns orchestration and tool execution.
 */
public final class GeminiTextModelSession implements ModelSession {
    private static final String HOST = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String[] MODELS = {
            "gemini-3.8-flash",
            "gemini-3.6-flash",
            "gemini-3.5-flash"
    };

    private final String apiKey;
    private final double temperature;
    private final OkHttpClient client;
    private final JSONArray history = new JSONArray();
    private final Map<String, String> pendingCallNames = new LinkedHashMap<String, String>();

    private SessionConfig config;
    private ModelSession.Listener listener;
    private volatile Call activeCall;
    private volatile boolean started;
    private volatile boolean closed;
    private volatile boolean interrupted;
    private volatile int preferredModelIndex;

    public GeminiTextModelSession(String apiKey) {
        this(apiKey, 0.82);
    }

    public GeminiTextModelSession(String apiKey, double temperature) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.temperature = Math.max(0.1, Math.min(1.4, temperature));
        this.client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .build();
    }

    @Override public synchronized void start(SessionConfig config, ModelSession.Listener listener) {
        if (closed) throw new IllegalStateException("session is closed");
        if (started) return;
        if (apiKey.isEmpty()) throw new IllegalStateException("Gemini API key is empty");
        if (config == null) throw new IllegalArgumentException("session config is null");
        if (listener == null) throw new IllegalArgumentException("listener is null");
        this.config = config;
        this.listener = listener;
        this.started = true;
    }

    @Override public void sendUserText(String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) return;
        requireRunning();
        interrupted = false;
        try {
            synchronized (this) {
                history.put(content("user", new JSONObject().put("text", value)));
            }
            requestModel(config.tools() != null && !config.tools().isEmpty());
        } catch (Exception error) {
            emit(ModelEvent.error(error));
        }
    }

    @Override public void sendUserAudio(byte[] audio) {
        emit(ModelEvent.error(new UnsupportedOperationException("Crew Fortune is text-only")));
    }

    @Override public void sendToolResult(ToolResult result) {
        requireRunning();
        if (result == null) return;
        interrupted = false;
        try {
            String name;
            synchronized (this) {
                name = pendingCallNames.remove(result.callId());
            }
            if (name == null || name.isEmpty()) name = "calculate_fortune";

            JSONObject payload = new JSONObject();
            payload.put("ok", result.success());
            if (result.success()) {
                for (Map.Entry<String, Object> entry : result.payload().entrySet()) {
                    payload.put(entry.getKey(), JSONObject.wrap(entry.getValue()));
                }
            } else {
                payload.put("error_code", result.errorCode());
                payload.put("error", result.errorMessage());
            }

            JSONObject functionResponse = new JSONObject()
                    .put("id", result.callId())
                    .put("name", name)
                    .put("response", payload);
            synchronized (this) {
                history.put(content("user", new JSONObject().put("functionResponse", functionResponse)));
            }
            requestModel(false);
        } catch (Exception error) {
            emit(ModelEvent.error(error));
        }
    }

    @Override public void interrupt() {
        interrupted = true;
        Call call = activeCall;
        if (call != null) call.cancel();
        emit(ModelEvent.interrupted());
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        started = false;
        Call call = activeCall;
        if (call != null) call.cancel();
        activeCall = null;
        pendingCallNames.clear();
        listener = null;
    }

    private void requestModel(boolean forceFortuneTool) {
        final JSONObject body;
        try {
            body = buildRequest(forceFortuneTool);
        } catch (Exception error) {
            emit(ModelEvent.error(error));
            return;
        }
        execute(body, Math.max(0, Math.min(preferredModelIndex, MODELS.length - 1)));
    }

    private void execute(final JSONObject body, final int modelIndex) {
        if (closed) return;
        String model = MODELS[modelIndex];
        Request request = new Request.Builder()
                .url(HOST + model + ":generateContent")
                .header("x-goog-api-key", apiKey)
                .post(RequestBody.create(JSON, body.toString()))
                .build();
        Call call = client.newCall(request);
        activeCall = call;
        call.enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException error) {
                if (closed) return;
                if (interrupted || call.isCanceled()) {
                    interrupted = false;
                    return;
                }
                emit(ModelEvent.error(error));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String raw = response.body() == null ? "" : response.body().string();
                try {
                    if (!response.isSuccessful()) {
                        if (modelIndex + 1 < MODELS.length) {
                            execute(body, modelIndex + 1);
                            return;
                        }
                        throw new IOException("Gemini HTTP " + response.code() + ": " + abbreviate(raw));
                    }
                    preferredModelIndex = modelIndex;
                    handleResponse(raw);
                } catch (Exception error) {
                    emit(ModelEvent.error(error));
                } finally {
                    response.close();
                }
            }
        });
    }

    private JSONObject buildRequest(boolean forceFortuneTool) throws Exception {
        JSONArray contents;
        synchronized (this) {
            contents = new JSONArray(history.toString());
        }

        JSONObject request = new JSONObject()
                .put("systemInstruction", new JSONObject().put("parts",
                        new JSONArray().put(new JSONObject().put("text", config.systemPrompt()))))
                .put("contents", contents)
                .put("generationConfig", new JSONObject()
                        .put("temperature", temperature)
                        .put("maxOutputTokens", 6000)
                        .put("responseMimeType", "application/json"));

        JSONArray declarations = functionDeclarations();
        if (declarations.length() > 0) {
            request.put("tools", new JSONArray().put(new JSONObject()
                    .put("functionDeclarations", declarations)));
        }

        if (forceFortuneTool && declarations.length() > 0) {
            request.put("toolConfig", new JSONObject().put("functionCallingConfig",
                    new JSONObject()
                            .put("mode", "ANY")
                            .put("allowedFunctionNames", new JSONArray().put("calculate_fortune"))));
        }
        return request;
    }

    private JSONArray functionDeclarations() throws Exception {
        JSONArray declarations = new JSONArray();
        for (ToolSpec tool : config.tools()) {
            declarations.put(new JSONObject()
                    .put("name", tool.name())
                    .put("description", tool.description())
                    .put("parameters", new JSONObject(tool.inputSchemaJson())));
        }
        return declarations;
    }

    private void handleResponse(String raw) throws Exception {
        JSONObject root = new JSONObject(raw);
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            throw new IllegalStateException("Gemini returned no candidates");
        }
        JSONObject candidate = candidates.getJSONObject(0);
        JSONObject modelContent = candidate.optJSONObject("content");
        if (modelContent == null) throw new IllegalStateException("Gemini returned no content");

        synchronized (this) {
            history.put(new JSONObject(modelContent.toString()));
        }

        JSONArray parts = modelContent.optJSONArray("parts");
        boolean requestedTool = false;
        if (parts != null) {
            for (int i = 0; i < parts.length(); i++) {
                JSONObject part = parts.optJSONObject(i);
                if (part == null) continue;

                String text = part.optString("text", "");
                if (!text.isEmpty()) emit(ModelEvent.text(text));

                JSONObject functionCall = part.optJSONObject("functionCall");
                if (functionCall != null) {
                    String name = functionCall.optString("name", "");
                    String id = functionCall.optString("id", "");
                    if (id.isEmpty()) id = "fortune_" + System.nanoTime();
                    JSONObject args = functionCall.optJSONObject("args");
                    Map<String, Object> arguments = args == null
                            ? new LinkedHashMap<String, Object>()
                            : jsonToMap(args);
                    synchronized (this) {
                        pendingCallNames.put(id, name);
                    }
                    requestedTool = true;
                    emit(ModelEvent.toolCall(new ToolCall(id, name, arguments)));
                }
            }
        }
        if (!requestedTool) emit(ModelEvent.turnCompleted());
    }

    private static JSONObject content(String role, JSONObject singlePart) throws Exception {
        return new JSONObject()
                .put("role", role)
                .put("parts", new JSONArray().put(singlePart));
    }

    private void requireRunning() {
        if (!started || closed) throw new IllegalStateException("session is not running");
    }

    private void emit(ModelEvent event) {
        ModelSession.Listener target = listener;
        if (target != null && event != null) target.onModelEvent(event);
    }

    private static Map<String, Object> jsonToMap(JSONObject object) throws Exception {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            result.put(key, unwrap(object.opt(key)));
        }
        return result;
    }

    private static Object unwrap(Object value) throws Exception {
        if (value instanceof JSONObject) return jsonToMap((JSONObject) value);
        if (value instanceof JSONArray) {
            JSONArray source = (JSONArray) value;
            java.util.List<Object> values = new java.util.ArrayList<Object>();
            for (int i = 0; i < source.length(); i++) values.add(unwrap(source.opt(i)));
            return values;
        }
        return value == JSONObject.NULL ? null : value;
    }

    private static String abbreviate(String value) {
        if (value == null) return "";
        String clean = value.replace('\n', ' ').trim();
        return clean.length() <= 240 ? clean : clean.substring(0, 240) + "…";
    }
}
