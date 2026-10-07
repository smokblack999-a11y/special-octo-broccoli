package com.samuray.telegram.core;

public final class TelegramChat {
    public final long id;
    public final String title;
    public final int unreadCount;
    public final int lastMessageId;

    public TelegramChat(long id, String title, int unreadCount, int lastMessageId) {
        this.id = id;
        this.title = title == null ? "" : title;
        this.unreadCount = unreadCount;
        this.lastMessageId = lastMessageId;
    }
}
