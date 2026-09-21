package com.crewpocket.fortune;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
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
            if (BuildConfig.DEBUG) {
                appCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance());
            } else {
                appCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance());
            }
        } catch (RuntimeException ignored) {
            // AI Logic will surface an actionable error if App Check is misconfigured.
        }
    }
}
