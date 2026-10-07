package com.samuray.telegram.core;

public final class TelegramMessage {
    public final long chatId;
    public final long id;
    public final long senderUserId;
    public final boolean outgoing;
    public final int date;
    public final String text;

    public TelegramMessage(long chatId, long id, long senderUserId, boolean outgoing, int date, String text) {
        this.chatId = chatId;
        this.id = id;
        this.senderUserId = senderUserId;
        this.outgoing = outgoing;
        this.date = date;
        this.text = text == null ? "" : text;
    }
}
