package com.samuray.telegram.core;

import java.io.File;

public final class TelegramConfig {
    public final int apiId;
    public final String apiHash;
    public final File databaseDirectory;
    public final File filesDirectory;
    public final String databaseEncryptionKey;
    public final String systemLanguageCode;
    public final String deviceModel;
    public final String systemVersion;
    public final String applicationVersion;

    public TelegramConfig(int apiId, String apiHash, File databaseDirectory, File filesDirectory,
                          String databaseEncryptionKey, String systemLanguageCode,
                          String deviceModel, String systemVersion, String applicationVersion) {
        if (apiId <= 0) throw new IllegalArgumentException("apiId must be positive");
        if (apiHash == null || apiHash.trim().isEmpty()) throw new IllegalArgumentException("apiHash is required");
        if (databaseDirectory == null) throw new NullPointerException("databaseDirectory");
        if (filesDirectory == null) throw new NullPointerException("filesDirectory");
        this.apiId = apiId;
        this.apiHash = apiHash;
        this.databaseDirectory = databaseDirectory;
        this.filesDirectory = filesDirectory;
        this.databaseEncryptionKey = databaseEncryptionKey == null ? "" : databaseEncryptionKey;
        this.systemLanguageCode = systemLanguageCode == null ? "en" : systemLanguageCode;
        this.deviceModel = deviceModel == null ? "Android" : deviceModel;
        this.systemVersion = systemVersion == null ? "unknown" : systemVersion;
        this.applicationVersion = applicationVersion == null ? "0.1.0" : applicationVersion;
    }
}
