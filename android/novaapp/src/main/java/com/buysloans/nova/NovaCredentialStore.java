package com.buysloans.nova;

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

final class NovaCredentialStore {
    private static final String PREFS = "nova_secure_session";
    private static final String KEY_ALIAS = "nova_remembered_session_v1";
    private static final String TOKEN = "refresh_token";
    private static final String IV = "refresh_iv";
    private static final String EMAIL = "email";

    private final SharedPreferences prefs;

    NovaCredentialStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    boolean hasRememberedSession() {
        return prefs.contains(TOKEN) && prefs.contains(IV);
    }

    String email() {
        return prefs.getString(EMAIL, "");
    }

    void save(NovaApiClient.Session session) throws Exception {
        if (session == null || session.refreshToken == null || session.refreshToken.isBlank()) {
            throw new IllegalArgumentException("Nova session does not contain a refresh token.");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = cipher.doFinal(session.refreshToken.getBytes(StandardCharsets.UTF_8));
        prefs.edit()
                .putString(TOKEN, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .putString(EMAIL, session.email == null ? "" : session.email)
                .apply();
    }

    String refreshToken() throws Exception {
        String encrypted = prefs.getString(TOKEN, "");
        String iv = prefs.getString(IV, "");
        if (encrypted.isBlank() || iv.isBlank()) return "";
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(),
                new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
        byte[] plain = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP));
        return new String(plain, StandardCharsets.UTF_8);
    }

    void clear() {
        prefs.edit().clear().apply();
    }

    private SecretKey key() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        java.security.Key existing = keyStore.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;

        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }
}
