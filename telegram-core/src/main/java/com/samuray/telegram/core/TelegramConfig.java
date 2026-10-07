package com.samuray.telegram.core;

import java.io.File;
import java.util.Arrays;

public final class TelegramConfig {
    public final int apiId;
    public final String apiHash;
    public final File databaseDirectory;
    public final File filesDirectory;
    public final byte[] databaseEncryptionKey;
    public final String systemLanguageCode;
    public final String deviceModel;
    public final String systemVersion;
    public final String applicationVersion;

    public TelegramConfig(int apiId, String apiHash, File databaseDirectory, File filesDirectory,
                          byte[] databaseEncryptionKey, String systemLanguageCode,
                          String deviceModel, String systemVersion, String applicationVersion) {
        if (apiId <= 0) throw new IllegalArgumentException("apiId must be positive");
        if (apiHash == null || apiHash.trim().isEmpty()) throw new IllegalArgumentException("apiHash is required");
        if (databaseDirectory == null) throw new NullPointerException("databaseDirectory");
        if (filesDirectory == null) throw new NullPointerException("filesDirectory");
        if (databaseEncryptionKey == null || databaseEncryptionKey.length != 32) {
            throw new IllegalArgumentException("databaseEncryptionKey must be exactly 32 bytes");
        }

        this.apiId = apiId;
        this.apiHash = apiHash;
        this.databaseDirectory = databaseDirectory;
        this.filesDirectory = filesDirectory;
        this.databaseEncryptionKey = Arrays.copyOf(databaseEncryptionKey, databaseEncryptionKey.length);
        this.systemLanguageCode = systemLanguageCode == null ? "en" : systemLanguageCode;
        this.deviceModel = deviceModel == null ? "Android" : deviceModel;
        this.systemVersion = systemVersion == null ? "unknown" : systemVersion;
        this.applicationVersion = applicationVersion == null ? "0.1.0" : applicationVersion;
    }
}
