package com.crewpocket.fortune;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;

final class FortuneShareController {
    private final MainActivity host;

    FortuneShareController(MainActivity host) {
        this.host = host;
    }

    void share() {
        FortuneResult result = host.shareResultForController();
        FortuneFacts facts = host.shareFactsForController();
        if (result == null || facts == null) return;

        if (host.isAiInterpretationRunning()
                && host.shareCopyForController() == null) {
            Toast.makeText(
                    host,
                    "AI 還在整理，完成後再產生分享圖片",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final FortuneMode mode = host.aiModeForController();
        final AiFortuneCopy copy = host.shareCopyForController();

        OperationLog.add(host, "SHARE_IMAGE_START", mode.name());
        Toast.makeText(
                host,
                "正在產生分享卡…",
                Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            Bitmap bitmap = null;
            try {
                FortuneShareCardData data = FortuneShareCardData.from(
                        mode,
                        facts,
                        result,
                        copy,
                        "");
                bitmap = FortuneShareCardRenderer.render(data);

                File directory = new File(host.getCacheDir(), "share");
                if (!directory.exists() && !directory.mkdirs()) {
                    throw new IllegalStateException("Cannot create share cache");
                }

                File image = new File(
                        directory,
                        "crew_fortune_share.png");
                FileOutputStream stream =
                        new FileOutputStream(image, false);
                try {
                    if (!bitmap.compress(
                            Bitmap.CompressFormat.PNG,
                            100,
                            stream)) {
                        throw new IllegalStateException("PNG encode failed");
                    }
                    stream.flush();
                } finally {
                    try { stream.close(); } catch (Exception ignored) {}
                }

                Uri uri = FileProvider.getUriForFile(
                        host,
                        host.getPackageName() + ".fileprovider",
                        image);

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/png");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.setClipData(
                        ClipData.newRawUri("Crew Fortune", uri));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                OperationLog.add(
                        host,
                        "SHARE_IMAGE_READY",
                        mode.name()
                                + " · 1080x1350 · privacy_safe");

                host.runOnUiThread(() -> host.startActivity(
                        Intent.createChooser(
                                intent,
                                "分享你的命運")));
            } catch (Exception error) {
                OperationLog.add(
                        host,
                        "SHARE_IMAGE_FAILED",
                        host.safeErrorMessage(error));
                host.runOnUiThread(() -> Toast.makeText(
                        host,
                        "分享圖片產生失敗，請再試一次",
                        Toast.LENGTH_SHORT).show());
            } finally {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        }, "fortune-share-card").start();
    }
}
