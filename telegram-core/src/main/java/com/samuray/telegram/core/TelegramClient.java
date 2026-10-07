package com.samuray.telegram.core;

import java.util.List;

public interface TelegramClient {
    interface Listener {
        void onAuthStateChanged(TelegramAuthState state);
        void onOtherDeviceConfirmation(String link);
        void onChatChanged(TelegramChat chat);
        void onMessageChanged(TelegramMessage message);
        void onError(String code, String message);
    }

    void start();
    void setListener(Listener listener);
    TelegramAuthState getAuthState();

    void setPhoneNumber(String phoneNumber, TelegramResult<Void> result);
    void setAuthenticationCode(String code, TelegramResult<Void> result);
    void setAuthenticationPassword(String password, TelegramResult<Void> result);
    void setRegistrationName(String firstName, String lastName, TelegramResult<Void> result);
    void setEmailAddress(String emailAddress, TelegramResult<Void> result);
    void setEmailCode(String code, TelegramResult<Void> result);

    void getChats(long offsetOrder, long offsetChatId, int limit, TelegramResult<List<TelegramChat>> result);
    void searchChats(String query, int limit, TelegramResult<List<TelegramChat>> result);
    void getChatHistory(long chatId, long fromMessageId, int offset, int limit,
                        TelegramResult<List<TelegramMessage>> result);

    void sendText(long chatId, String text, TelegramResult<TelegramMessage> result);
    void sendPhoto(long chatId, String localPath, String caption, TelegramResult<TelegramMessage> result);
    void downloadFile(int fileId, int priority, TelegramResult<String> result);

    void openChat(long chatId);
    void closeChat(long chatId);
    void viewMessages(long chatId, int[] messageIds);

    void logout(TelegramResult<Void> result);
    void close();
}
