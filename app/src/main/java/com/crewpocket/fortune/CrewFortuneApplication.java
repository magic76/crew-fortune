package com.crewpocket.fortune;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

/**
 * Initializes Firebase only when google-services.json is present.
 * CI and local BYOK development can keep running without Firebase configuration.
 */
public final class CrewFortuneApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();

        FirebaseApp app;
        try {
            app = FirebaseApp.initializeApp(this);
        } catch (RuntimeException error) {
            app = null;
        }
        if (app == null) return;

        try {
            FirebaseAppCheck appCheck = FirebaseAppCheck.getInstance();
            boolean debuggable =
                    (getApplicationInfo().flags
                            & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)
                            != 0;
            if (debuggable) {
                installDebugProvider(appCheck);
            } else {
                appCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance());
            }
        } catch (RuntimeException ignored) {
            // AI Logic will surface an actionable error if App Check is misconfigured.
        }
    }

    private void installDebugProvider(FirebaseAppCheck appCheck) {
        try {
            Class<?> type = Class.forName(
                    "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory");
            Object value = type.getMethod("getInstance").invoke(null);
            if (value instanceof com.google.firebase.appcheck.AppCheckProviderFactory) {
                appCheck.installAppCheckProviderFactory(
                        (com.google.firebase.appcheck.AppCheckProviderFactory) value);
            }
        } catch (Exception ignored) {
            // Debug builds without firebase-appcheck-debug simply leave App Check uninstalled.
        }
    }
}
