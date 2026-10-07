package com.samuray.telegram.core;

public final class TelegramChat {
    public final long id;
    public final String title;
    public final int unreadCount;
    public final long lastMessageId;

    public TelegramChat(long id, String title, int unreadCount, long lastMessageId) {
        this.id = id;
        this.title = title == null ? "" : title;
        this.unreadCount = Math.max(0, unreadCount);
        this.lastMessageId = Math.max(0L, lastMessageId);
    }
}
