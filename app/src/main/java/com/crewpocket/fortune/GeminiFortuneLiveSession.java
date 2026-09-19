package com.crewpocket.fortune;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.SystemClock;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public final class GeminiFortuneLiveSession {
    private static final String HOST = "generativelanguage.googleapis.com";
    private static final String WS_PATH =
            "/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent";
    private static final String MODEL = "gemini-3.8-live";
    private static final int INPUT_RATE = 16000;
    private static final int OUTPUT_RATE = 24000;
    private static final long ECHO_GUARD_MS = 320L;

    public interface Listener {
        void onStatus(String status);
        void onReady();
        void onInputTranscript(String text);
        void onOutputTranscript(String text);
        void onSpeakingChanged(boolean speaking);
        void onError(String message);
    }

    private final Context context;
    private final String apiKey;
    private final String voiceName;
    private final String systemPrompt;
    private final String openingPrompt;
    private final Listener listener;
    private final OkHttpClient client;
    private final ExecutorService audioWriter = Executors.newSingleThreadExecutor();

    private WebSocket webSocket;
    private volatile boolean running;
    private volatile boolean ready;
    private volatile boolean recording;
    private volatile boolean speaking;
    private volatile long micSuppressedUntilMs;
    private AudioRecord recorder;
    private AudioTrack player;
    private AcousticEchoCanceler echoCanceler;
    private NoiseSuppressor noiseSuppressor;
    private Thread micThread;

    public GeminiFortuneLiveSession(
            Context context,
            String apiKey,
            String voiceName,
            String systemPrompt,
            String openingPrompt,
            Listener listener) {
        this.context = context.getApplicationContext();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.voiceName = voiceName == null || voiceName.trim().isEmpty() ? "Kore" : voiceName.trim();
        this.systemPrompt = systemPrompt == null ? "" : systemPrompt;
        this.openingPrompt = openingPrompt == null ? "" : openingPrompt;
        this.listener = listener;
        this.client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(10, TimeUnit.SECONDS)
                .build();
    }

    public synchronized void start() {
        if (running) return;
        if (apiKey.isEmpty()) throw new IllegalStateException("Gemini API key is empty");
        running = true;
        ready = false;
        status("正在連接命理老師…");

        Request request = new Request.Builder()
                .url("wss://" + HOST + WS_PATH + "?key=" + apiKey)
                .build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override public void onOpen(WebSocket socket, Response response) {
                try {
                    socket.send(buildSetup().toString());
                } catch (Exception error) {
                    fail("老師連線初始化失敗：" + error.getMessage());
                }
            }

            @Override public void onMessage(WebSocket socket, String text) {
                handleMessage(text);
            }

            @Override public void onMessage(WebSocket socket, ByteString bytes) {
                handleMessage(bytes.utf8());
            }

            @Override public void onClosed(WebSocket socket, int code, String reason) {
                if (running) fail("老師已斷線：" + code);
            }

            @Override public void onFailure(WebSocket socket, Throwable error, Response response) {
                fail("老師連線失敗：" + (error == null ? "未知錯誤" : error.getMessage()));
            }
        });
    }

    public synchronized void close() {
        if (!running) return;
        running = false;
        ready = false;
        sendAudioStreamEnd();
        stopAudio();
        if (webSocket != null) {
            try { webSocket.close(1000, "Session closed"); } catch (Exception ignored) {}
            webSocket = null;
        }
        status("已結束");
    }

    public void interrupt() {
        flushPlayback();
        setSpeaking(false);
        status("請直接說，我在聽");
    }

    public boolean isRunning() {
        return running;
    }

    private JSONObject buildSetup() throws Exception {
        JSONObject setup = new JSONObject()
                .put("model", "models/" + MODEL)
                .put("generationConfig", new JSONObject()
                        .put("responseModalities", new JSONArray().put("AUDIO"))
                        .put("speechConfig", new JSONObject()
                                .put("voiceConfig", new JSONObject()
                                        .put("prebuiltVoiceConfig", new JSONObject()
                                                .put("voiceName", voiceName)))))
                .put("inputAudioTranscription", new JSONObject())
                .put("outputAudioTranscription", new JSONObject())
                .put("contextWindowCompression", new JSONObject()
                        .put("slidingWindow", new JSONObject()))
                .put("systemInstruction", new JSONObject()
                        .put("parts", new JSONArray()
                                .put(new JSONObject().put("text", systemPrompt))));
        return new JSONObject().put("setup", setup);
    }

    private void handleMessage(String text) {
        if (!running || text == null) return;
        try {
            JSONObject response = new JSONObject(text);
            JSONObject apiError = response.optJSONObject("error");
            if (apiError != null) {
                fail(apiError.optString("message", "Gemini Live error"));
                return;
            }

            if (response.has("setupComplete") || response.has("setup_complete")) {
                ready = true;
                ensurePlayer();
                startInputAudio();
                sendOpeningTurn();
                status("老師上線了，你可以直接跟他說話");
                if (listener != null) listener.onReady();
                return;
            }

            JSONObject server = response.optJSONObject("serverContent");
            if (server == null) server = response.optJSONObject("server_content");
            if (server != null) handleServerContent(server);
        } catch (Exception error) {
            fail("老師回應解析失敗：" + error.getMessage());
        }
    }

    private void handleServerContent(JSONObject server) throws Exception {
        if (server.optBoolean("interrupted", false)) {
            flushPlayback();
            setSpeaking(false);
        }

        JSONObject input = server.optJSONObject("inputTranscription");
        if (input == null) input = server.optJSONObject("input_transcription");
        if (input != null && listener != null) {
            String value = input.optString("text", "").trim();
            if (!value.isEmpty()) listener.onInputTranscript(value);
        }

        JSONObject output = server.optJSONObject("outputTranscription");
        if (output == null) output = server.optJSONObject("output_transcription");
        if (output != null && listener != null) {
            String value = output.optString("text", "").trim();
            if (!value.isEmpty()) listener.onOutputTranscript(value);
        }

        JSONObject turn = server.optJSONObject("modelTurn");
        if (turn == null) turn = server.optJSONObject("model_turn");
        if (turn != null) {
            JSONArray parts = turn.optJSONArray("parts");
            if (parts != null) {
                for (int i = 0; i < parts.length(); i++) {
                    JSONObject part = parts.optJSONObject(i);
                    if (part == null) continue;
                    JSONObject inline = part.optJSONObject("inlineData");
                    if (inline == null) inline = part.optJSONObject("inline_data");
                    if (inline == null) continue;
                    String mime = inline.optString("mimeType", "");
                    if (!mime.startsWith("audio/pcm")) continue;
                    byte[] pcm = Base64.decode(inline.optString("data", ""), Base64.DEFAULT);
                    if (pcm.length > 0) {
                        ensurePlayer();
                        setSpeaking(true);
                        final byte[] frame = pcm;
                        audioWriter.execute(new Runnable() {
                            @Override public void run() {
                                writeAudio(frame);
                            }
                        });
                    }
                }
            }
        }

        if (server.optBoolean("turnComplete", server.optBoolean("turn_complete", false))) {
            setSpeaking(false);
            status("你可以繼續追問");
        }
    }

    private void sendOpeningTurn() {
        if (!running || !ready || webSocket == null || openingPrompt.trim().isEmpty()) return;
        try {
            JSONObject turn = new JSONObject()
                    .put("role", "user")
                    .put("parts", new JSONArray()
                            .put(new JSONObject().put("text", openingPrompt)));
            webSocket.send(new JSONObject()
                    .put("clientContent", new JSONObject()
                            .put("turns", new JSONArray().put(turn))
                            .put("turnComplete", true))
                    .toString());
        } catch (Exception error) {
            fail("無法開始講解：" + error.getMessage());
        }
    }

    private synchronized void startInputAudio() {
        if (!running || !ready || recording) return;
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            fail("需要麥克風權限才能跟老師對話");
            return;
        }

        int min = AudioRecord.getMinBufferSize(
                INPUT_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int size = Math.max(min * 4, 8192);
        try {
            recorder = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    INPUT_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    size);
        } catch (Exception ignored) {
            recorder = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    INPUT_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    size);
        }

        enableAudioEffects();
        recording = true;
        recorder.startRecording();
        micThread = new Thread(new Runnable() {
            @Override public void run() {
                captureLoop();
            }
        }, "FortuneTeacherMic");
        micThread.start();
    }

    private synchronized void ensurePlayer() {
        if (!running || !ready || player != null) return;
        try {
            AudioManager manager =
                    (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (manager != null) manager.setMode(AudioManager.MODE_NORMAL);
        } catch (Exception ignored) {}

        int min = AudioTrack.getMinBufferSize(
                OUTPUT_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        player = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(OUTPUT_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(Math.max(min * 4, 24000))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
        player.play();
    }

    private void captureLoop() {
        byte[] buffer = new byte[3200];
        while (running && recording) {
            AudioRecord target = recorder;
            if (target == null) break;
            int read;
            try {
                read = target.read(buffer, 0, buffer.length);
            } catch (Exception ignored) {
                break;
            }
            if (read <= 0) continue;
            if (speaking || SystemClock.elapsedRealtime() < micSuppressedUntilMs) continue;
            byte[] frame = new byte[read];
            System.arraycopy(buffer, 0, frame, 0, read);
            sendAudio(frame);
        }
    }

    private void sendAudio(byte[] bytes) {
        if (!running || !ready || webSocket == null || bytes == null || bytes.length == 0) return;
        try {
            JSONObject audio = new JSONObject()
                    .put("mimeType", "audio/pcm;rate=16000")
                    .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP));
            webSocket.send(new JSONObject()
                    .put("realtimeInput", new JSONObject().put("audio", audio))
                    .toString());
        } catch (Exception error) {
            fail("麥克風串流失敗：" + error.getMessage());
        }
    }

    private void writeAudio(byte[] pcm) {
        try {
            AudioTrack target = player;
            if (!running || target == null) return;
            long durationMs = Math.max(1L, (pcm.length * 1000L) / (OUTPUT_RATE * 2L));
            micSuppressedUntilMs = Math.max(
                    micSuppressedUntilMs,
                    SystemClock.elapsedRealtime() + durationMs + ECHO_GUARD_MS);
            target.write(pcm, 0, pcm.length, AudioTrack.WRITE_BLOCKING);
        } catch (Exception ignored) {}
    }

    private void sendAudioStreamEnd() {
        if (webSocket == null || !ready) return;
        try {
            webSocket.send(new JSONObject()
                    .put("realtimeInput", new JSONObject().put("audioStreamEnd", true))
                    .toString());
        } catch (Exception ignored) {}
    }

    private synchronized void enableAudioEffects() {
        releaseAudioEffects();
        if (recorder == null) return;
        int sessionId = recorder.getAudioSessionId();
        try {
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(sessionId);
                if (echoCanceler != null) echoCanceler.setEnabled(true);
            }
        } catch (Exception ignored) {
            echoCanceler = null;
        }
        try {
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId);
                if (noiseSuppressor != null) noiseSuppressor.setEnabled(true);
            }
        } catch (Exception ignored) {
            noiseSuppressor = null;
        }
    }

    private synchronized void releaseAudioEffects() {
        if (echoCanceler != null) {
            try { echoCanceler.setEnabled(false); } catch (Exception ignored) {}
            try { echoCanceler.release(); } catch (Exception ignored) {}
            echoCanceler = null;
        }
        if (noiseSuppressor != null) {
            try { noiseSuppressor.setEnabled(false); } catch (Exception ignored) {}
            try { noiseSuppressor.release(); } catch (Exception ignored) {}
            noiseSuppressor = null;
        }
    }

    private synchronized void stopAudio() {
        recording = false;
        AudioRecord input = recorder;
        recorder = null;
        releaseAudioEffects();
        if (input != null) {
            try { input.stop(); } catch (Exception ignored) {}
            try { input.release(); } catch (Exception ignored) {}
        }

        AudioTrack output = player;
        player = null;
        if (output != null) {
            try { output.pause(); output.flush(); output.stop(); } catch (Exception ignored) {}
            try { output.release(); } catch (Exception ignored) {}
        }

        try {
            AudioManager manager =
                    (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (manager != null) manager.setMode(AudioManager.MODE_NORMAL);
        } catch (Exception ignored) {}
        setSpeaking(false);
    }

    private synchronized void flushPlayback() {
        if (player == null) return;
        try {
            player.pause();
            player.flush();
            player.play();
        } catch (Exception ignored) {}
    }

    private void setSpeaking(boolean value) {
        if (speaking == value) return;
        speaking = value;
        if (!value) {
            micSuppressedUntilMs = Math.max(
                    micSuppressedUntilMs,
                    SystemClock.elapsedRealtime() + ECHO_GUARD_MS);
        }
        if (listener != null) listener.onSpeakingChanged(value);
    }

    private void status(String value) {
        if (listener != null) listener.onStatus(value);
    }

    private void fail(String message) {
        running = false;
        ready = false;
        stopAudio();
        if (listener != null) listener.onError(message == null ? "未知錯誤" : message);
    }
}
