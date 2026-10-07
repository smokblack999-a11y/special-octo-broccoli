package com.samuray.telegram.sample;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SampleKeyStore {
    private static final String ALIAS = "telegram-core-sample-key";
    private static final String PREFS = "telegram-core-sample";
    private static final String KEY = "encrypted-db-key";

    private SampleKeyStore() {
    }

    public static byte[] getOrCreateDatabaseKey(Context context) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);

        if (!store.containsAlias(ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build());
            generator.generateKey();
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String stored = prefs.getString(KEY, null);
        SecretKey secretKey = ((KeyStore.SecretKeyEntry) store.getEntry(ALIAS, null)).getSecretKey();

        if (stored == null) {
            byte[] plain = new byte[32];
            new SecureRandom().nextBytes(plain);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(plain);
            byte[] packed = ByteBuffer.allocate(4 + cipher.getIV().length + encrypted.length)
                    .putInt(cipher.getIV().length)
                    .put(cipher.getIV())
                    .put(encrypted)
                    .array();

            prefs.edit().putString(KEY, Base64.encodeToString(packed, Base64.NO_WRAP)).apply();
            return plain;
        }

        byte[] packed = Base64.decode(stored, Base64.NO_WRAP);
        ByteBuffer buffer = ByteBuffer.wrap(packed);
        int ivLength = buffer.getInt();
        byte[] iv = new byte[ivLength];
        buffer.get(iv);
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
        return cipher.doFinal(encrypted);
    }
}
