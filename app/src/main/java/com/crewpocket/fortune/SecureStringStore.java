package com.crewpocket.fortune;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Stores small secrets encrypted with an AES key held by Android Keystore.
 */
final class SecureStringStore {
    private static final String PREFS = "crew_fortune_secure_store";
    private static final String KEY_ALIAS = "crew_fortune_secret_key_v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private SecureStringStore() {}

    static String get(Context context, String key) {
        if (context == null || key == null || key.trim().isEmpty()) return "";
        SharedPreferences prefs = prefs(context);
        String cipherText = prefs.getString(key + "_cipher", "");
        String ivText = prefs.getString(key + "_iv", "");
        if (cipherText == null || cipherText.isEmpty() || ivText == null || ivText.isEmpty()) {
            return "";
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateKey(),
                    new GCMParameterSpec(128, Base64.decode(ivText, Base64.NO_WRAP)));
            byte[] plain = cipher.doFinal(Base64.decode(cipherText, Base64.NO_WRAP));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception error) {
            throw new IllegalStateException("Unable to decrypt local secret", error);
        }
    }

    static void put(Context context, String key, String value) {
        if (context == null || key == null || key.trim().isEmpty()) return;
        String clean = value == null ? "" : value.trim();
        SharedPreferences prefs = prefs(context);
        if (clean.isEmpty()) {
            prefs.edit()
                    .remove(key + "_cipher")
                    .remove(key + "_iv")
                    .apply();
            return;
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] encrypted = cipher.doFinal(clean.getBytes(StandardCharsets.UTF_8));
            prefs.edit()
                    .putString(key + "_cipher", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .putString(key + "_iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .apply();
        } catch (Exception error) {
            throw new IllegalStateException("Unable to encrypt local secret", error);
        }
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        }

        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
