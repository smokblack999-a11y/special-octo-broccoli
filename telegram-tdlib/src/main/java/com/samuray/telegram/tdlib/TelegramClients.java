package com.samuray.telegram.tdlib;

import com.samuray.telegram.core.TelegramClient;
import com.samuray.telegram.core.TelegramConfig;

public final class TelegramClients {
    private TelegramClients() {
    }

    public static TelegramClient create(TelegramConfig config) {
        return new TdLibTelegramClient(config);
    }
}
