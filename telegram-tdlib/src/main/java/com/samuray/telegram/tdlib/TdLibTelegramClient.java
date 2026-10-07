package com.samuray.telegram.tdlib;

import com.samuray.telegram.core.TelegramAuthState;
import com.samuray.telegram.core.TelegramChat;
import com.samuray.telegram.core.TelegramClient;
import com.samuray.telegram.core.TelegramConfig;
import com.samuray.telegram.core.TelegramMessage;
import com.samuray.telegram.core.TelegramResult;
import com.samuray.telegram.core.TelegramUser;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class TdLibTelegramClient implements TelegramClient {
    private final TelegramConfig config;
    private volatile Listener listener;
    private volatile TelegramAuthState authState = TelegramAuthState.WAIT_PARAMETERS;
    private volatile Client client;

    public TdLibTelegramClient(TelegramConfig config) {
        if (config == null) throw new NullPointerException("config");
        this.config = config;
    }

    @Override
    public synchronized void start() {
        if (client != null) return;
        config.databaseDirectory.mkdirs();
        config.filesDirectory.mkdirs();
        client = Client.create(this::handleUpdate, null, null);
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public TelegramAuthState getAuthState() {
        return authState;
    }

    private void handleUpdate(TdApi.Object object) {
        if (object instanceof TdApi.UpdateAuthorizationState) {
            handleAuthorizationState(((TdApi.UpdateAuthorizationState) object).authorizationState);
        } else if (object instanceof TdApi.UpdateNewChat) {
            notifyChat(((TdApi.UpdateNewChat) object).chat);
        } else if (object instanceof TdApi.UpdateChatTitle
                || object instanceof TdApi.UpdateChatLastMessage
                || object instanceof TdApi.UpdateChatReadInbox
                || object instanceof TdApi.UpdateChatUnreadMentionCount) {
            long chatId = extractChatId(object);
            if (chatId != 0) refreshChat(chatId);
        } else if (object instanceof TdApi.UpdateNewMessage) {
            notifyMessage(((TdApi.UpdateNewMessage) object).message);
        } else if (object instanceof TdApi.UpdateMessageSendSucceeded) {
            notifyMessage(((TdApi.UpdateMessageSendSucceeded) object).message);
        } else if (object instanceof TdApi.UpdateMessageSendFailed) {
            TdApi.UpdateMessageSendFailed update = (TdApi.UpdateMessageSendFailed) object;
            notifyError(String.valueOf(update.error.code), update.error.message);
        }
    }

    private long extractChatId(TdApi.Object object) {
        if (object instanceof TdApi.UpdateChatTitle) return ((TdApi.UpdateChatTitle) object).chatId;
        if (object instanceof TdApi.UpdateChatLastMessage) return ((TdApi.UpdateChatLastMessage) object).chatId;
        if (object instanceof TdApi.UpdateChatReadInbox) return ((TdApi.UpdateChatReadInbox) object).chatId;
        if (object instanceof TdApi.UpdateChatUnreadMentionCount) return ((TdApi.UpdateChatUnreadMentionCount) object).chatId;
        return 0L;
    }

    private void handleAuthorizationState(TdApi.AuthorizationState state) {
        if (state instanceof TdApi.AuthorizationStateWaitTdlibParameters) {
            setAuthState(TelegramAuthState.WAIT_PARAMETERS);
            TdApi.SetTdlibParameters request = new TdApi.SetTdlibParameters();
            request.useTestDc = false;
            request.databaseDirectory = config.databaseDirectory.getAbsolutePath();
            request.filesDirectory = config.filesDirectory.getAbsolutePath();
            request.databaseEncryptionKey = config.databaseEncryptionKey.clone();
            request.useFileDatabase = true;
            request.useChatInfoDatabase = true;
            request.useMessageDatabase = true;
            request.useSecretChats = true;
            request.apiId = config.apiId;
            request.apiHash = config.apiHash;
            request.systemLanguageCode = config.systemLanguageCode;
            request.deviceModel = config.deviceModel;
            request.systemVersion = config.systemVersion;
            request.applicationVersion = config.applicationVersion;
            sendRaw(request, null);
        } else if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
            setAuthState(TelegramAuthState.WAIT_PHONE_NUMBER);
        } else if (state instanceof TdApi.AuthorizationStateWaitPremiumPurchase) {
            setAuthState(TelegramAuthState.WAIT_PREMIUM_PURCHASE);
        } else if (state instanceof TdApi.AuthorizationStateWaitCode) {
            setAuthState(TelegramAuthState.WAIT_CODE);
        } else if (state instanceof TdApi.AuthorizationStateWaitPassword) {
            setAuthState(TelegramAuthState.WAIT_PASSWORD);
        } else if (state instanceof TdApi.AuthorizationStateWaitEmailAddress) {
            setAuthState(TelegramAuthState.WAIT_EMAIL_ADDRESS);
        } else if (state instanceof TdApi.AuthorizationStateWaitEmailCode) {
            setAuthState(TelegramAuthState.WAIT_EMAIL_CODE);
        } else if (state instanceof TdApi.AuthorizationStateWaitRegistration) {
            setAuthState(TelegramAuthState.WAIT_REGISTRATION);
        } else if (state instanceof TdApi.AuthorizationStateWaitOtherDeviceConfirmation) {
            setAuthState(TelegramAuthState.WAIT_OTHER_DEVICE_CONFIRMATION);
            notifyOtherDeviceConfirmation(((TdApi.AuthorizationStateWaitOtherDeviceConfirmation) state).link);
        } else if (state instanceof TdApi.AuthorizationStateReady) {
            setAuthState(TelegramAuthState.READY);
        } else if (state instanceof TdApi.AuthorizationStateLoggingOut) {
            setAuthState(TelegramAuthState.LOGGING_OUT);
        } else if (state instanceof TdApi.AuthorizationStateClosing) {
            setAuthState(TelegramAuthState.CLOSING);
        } else if (state instanceof TdApi.AuthorizationStateClosed) {
            setAuthState(TelegramAuthState.CLOSED);
            client = null;
        }
    }

    @Override
    public void setPhoneNumber(String phoneNumber, TelegramResult<Void> result) {
        requireClient();
        sendRaw(new TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), wrapVoid(result));
    }

    @Override
    public void setAuthenticationCode(String code, TelegramResult<Void> result) {
        requireClient();
        sendRaw(new TdApi.CheckAuthenticationCode(code), wrapVoid(result));
    }

    @Override
    public void setAuthenticationPassword(String password, TelegramResult<Void> result) {
        requireClient();
        sendRaw(new TdApi.CheckAuthenticationPassword(password), wrapVoid(result));
    }

    @Override
    public void setRegistrationName(String firstName, String lastName, TelegramResult<Void> result) {
        requireClient();
        sendRaw(new TdApi.RegisterUser(firstName, lastName == null ? "" : lastName, false), wrapVoid(result));
    }

    @Override
    public void setEmailAddress(String emailAddress, TelegramResult<Void> result) {
        requireClient();
        sendRaw(new TdApi.SetAuthenticationEmailAddress(emailAddress), wrapVoid(result));
    }

    @Override
    public void setEmailCode(String code, TelegramResult<Void> result) {
        requireClient();
        sendRaw(new TdApi.CheckAuthenticationEmailCode(new TdApi.EmailAddressAuthenticationCode(code)),
                wrapVoid(result));
    }

    @Override
    public void resendAuthenticationCode(TelegramResult<Void> result) {
        requireClient();
        sendRaw(new TdApi.ResendAuthenticationCode(null), wrapVoid(result));
    }

    @Override
    public void requestQrCodeAuthentication(TelegramResult<Void> result) {
        requireClient();
        sendRaw(new TdApi.RequestQrCodeAuthentication(new long[0]), wrapVoid(result));
    }

    @Override
    public void getMe(final TelegramResult<TelegramUser> result) {
        requireClient();
        sendRaw(new TdApi.GetMe(), new ResultAdapter() {
            @Override
            public void onSuccess(TdApi.Object object) {
                if (result != null) result.onSuccess(mapUser((TdApi.User) object));
            }

            @Override
            public void onFailure(TdApi.Error error) {
                if (result != null) result.onError(String.valueOf(error.code), error.message);
            }
        });
    }

    @Override
    public void loadChats(int limit, TelegramResult<Void> result) {
        requireClient();
        sendRaw(new TdApi.LoadChats(null, limit), wrapVoid(result));
    }

    @Override
    public void getChats(int limit, TelegramResult<List<TelegramChat>> result) {
        requireClient();
        sendRaw(new TdApi.GetChats(null, limit), new ResultAdapter() {
            @Override
            public void onSuccess(TdApi.Object object) {
                collectChats(((TdApi.Chats) object).chatIds, result);
            }

            @Override
            public void onFailure(TdApi.Error error) {
                if (result != null) result.onError(String.valueOf(error.code), error.message);
            }
        });
    }

    @Override
    public void searchChats(String query, int limit, TelegramResult<List<TelegramChat>> result) {
        requireClient();
        sendRaw(new TdApi.SearchChats(query == null ? "" : query, limit), new ResultAdapter() {
            @Override
            public void onSuccess(TdApi.Object object) {
                collectChats(((TdApi.Chats) object).chatIds, result);
            }

            @Override
            public void onFailure(TdApi.Error error) {
                if (result != null) result.onError(String.valueOf(error.code), error.message);
            }
        });
    }

    private void collectChats(final long[] chatIds, final TelegramResult<List<TelegramChat>> result) {
        if (result == null) return;
        if (chatIds == null || chatIds.length == 0) {
            result.onSuccess(Collections.emptyList());
            return;
        }

        final ArrayList<TelegramChat> output =
                new ArrayList<>(Collections.nCopies(chatIds.length, (TelegramChat) null));
        final AtomicInteger remaining = new AtomicInteger(chatIds.length);
        final AtomicBoolean finished = new AtomicBoolean(false);

        for (int i = 0; i < chatIds.length; i++) {
            final int index = i;
            final long chatId = chatIds[i];
            sendRaw(new TdApi.GetChat(chatId), new ResultAdapter() {
                @Override
                public void onSuccess(TdApi.Object object) {
                    if (finished.get()) return;
                    output.set(index, mapChat((TdApi.Chat) object));
                    if (remaining.decrementAndGet() == 0
                            && finished.compareAndSet(false, true)) {
                        result.onSuccess(Collections.unmodifiableList(new ArrayList<>(output)));
                    }
                }

                @Override
                public void onFailure(TdApi.Error error) {
                    if (finished.compareAndSet(false, true)) {
                        result.onError(String.valueOf(error.code), error.message);
                    }
                }
            });
        }
    }

    @Override
    public void getChatHistory(long chatId, long fromMessageId, int offset, int limit,
                               final TelegramResult<List<TelegramMessage>> result) {
        requireClient();
        sendRaw(new TdApi.GetChatHistory(chatId, fromMessageId, offset, limit), new ResultAdapter() {
            @Override
            public void onSuccess(TdApi.Object object) {
                TdApi.Messages messages = (TdApi.Messages) object;
                ArrayList<TelegramMessage> output = new ArrayList<>(messages.messages.length);
                for (TdApi.Message message : messages.messages) {
                    TelegramMessage mapped = mapMessage(message);
                    if (mapped != null) output.add(mapped);
                }
                if (result != null) result.onSuccess(Collections.unmodifiableList(output));
            }

            @Override
            public void onFailure(TdApi.Error error) {
                if (result != null) result.onError(String.valueOf(error.code), error.message);
            }
        });
    }

    @Override
    public void sendText(long chatId, String text, final TelegramResult<TelegramMessage> result) {
        requireClient();

        TdApi.InputMessageText content = new TdApi.InputMessageText();
        content.text = new TdApi.FormattedText(text == null ? "" : text, null);
        content.linkPreviewOptions = null;
        content.clearDraft = true;

        TdApi.SendMessage request = new TdApi.SendMessage();
        request.chatId = chatId;
        request.topicId = null;
        request.replyTo = null;
        request.options = null;
        request.replyMarkup = null;
        request.inputMessageContent = content;

        sendRaw(request, new ResultAdapter() {
            @Override
            public void onSuccess(TdApi.Object object) {
                if (result != null) result.onSuccess(mapMessage((TdApi.Message) object));
            }

            @Override
            public void onFailure(TdApi.Error error) {
                if (result != null) result.onError(String.valueOf(error.code), error.message);
            }
        });
    }

    @Override
    public void sendPhoto(long chatId, String localPath, String caption,
                          final TelegramResult<TelegramMessage> result) {
        requireClient();

        TdApi.InputPhoto inputPhoto = new TdApi.InputPhoto();
        inputPhoto.photo = new TdApi.InputFileLocal(localPath);
        inputPhoto.thumbnail = null;
        inputPhoto.video = null;
        inputPhoto.addedStickerFileIds = new int[0];
        inputPhoto.width = 0;
        inputPhoto.height = 0;

        TdApi.InputMessagePhoto content = new TdApi.InputMessagePhoto();
        content.photo = inputPhoto;
        content.caption = new TdApi.FormattedText(caption == null ? "" : caption, null);
        content.showCaptionAboveMedia = false;
        content.selfDestructType = null;
        content.hasSpoiler = false;

        TdApi.SendMessage request = new TdApi.SendMessage();
        request.chatId = chatId;
        request.topicId = null;
        request.replyTo = null;
        request.options = null;
        request.replyMarkup = null;
        request.inputMessageContent = content;

        sendRaw(request, new ResultAdapter() {
            @Override
            public void onSuccess(TdApi.Object object) {
                if (result != null) result.onSuccess(mapMessage((TdApi.Message) object));
            }

            @Override
            public void onFailure(TdApi.Error error) {
                if (result != null) result.onError(String.valueOf(error.code), error.message);
            }
        });
    }

    @Override
    public void downloadFile(int fileId, int priority, final TelegramResult<String> result) {
        requireClient();
        sendRaw(new TdApi.DownloadFile(fileId, priority, 0, 0, true), new ResultAdapter() {
            @Override
            public void onSuccess(TdApi.Object object) {
                TdApi.File file = (TdApi.File) object;
                if (file.local != null && file.local.path != null && !file.local.path.isEmpty()) {
                    if (result != null) result.onSuccess(file.local.path);
                } else if (result != null) {
                    result.onError("FILE_PATH_EMPTY", "TDLib returned an empty local file path");
                }
            }

            @Override
            public void onFailure(TdApi.Error error) {
                if (result != null) result.onError(String.valueOf(error.code), error.message);
            }
        });
    }

    @Override
    public void openChat(long chatId) {
        requireClient();
        sendRaw(new TdApi.OpenChat(chatId), null);
    }

    @Override
    public void closeChat(long chatId) {
        requireClient();
        sendRaw(new TdApi.CloseChat(chatId), null);
    }

    @Override
    public void viewMessages(long chatId, long[] messageIds) {
        requireClient();
        sendRaw(new TdApi.ViewMessages(chatId, messageIds, null, true), null);
    }

    @Override
    public void logout(TelegramResult<Void> result) {
        requireClient();
        sendRaw(new TdApi.LogOut(), wrapVoid(result));
    }

    @Override
    public synchronized void close() {
        Client current = client;
        if (current == null) return;
        if (authState == TelegramAuthState.CLOSING || authState == TelegramAuthState.CLOSED) return;
        current.send(new TdApi.Close(), object -> {});
    }

    private void refreshChat(final long chatId) {
        if (client == null) return;
        sendRaw(new TdApi.GetChat(chatId), new ResultAdapter() {
            @Override
            public void onSuccess(TdApi.Object object) {
                notifyChat((TdApi.Chat) object);
            }
        });
    }

    private void requireClient() {
        if (client == null) throw new IllegalStateException("Telegram client is not started");
    }

    private void setAuthState(TelegramAuthState state) {
        authState = state;
        Listener l = listener;
        if (l != null) l.onAuthStateChanged(state);
    }

    private void notifyOtherDeviceConfirmation(String link) {
        Listener l = listener;
        if (l != null) l.onOtherDeviceConfirmation(link);
    }

    private void notifyChat(TdApi.Chat chat) {
        Listener l = listener;
        if (l != null && chat != null) l.onChatChanged(mapChat(chat));
    }

    private void notifyMessage(TdApi.Message message) {
        TelegramMessage mapped = mapMessage(message);
        Listener l = listener;
        if (mapped != null && l != null) l.onMessageChanged(mapped);
    }

    private TelegramUser mapUser(TdApi.User user) {
        String username = "";
        if (user.usernames != null
                && user.usernames.activeUsernames != null
                && user.usernames.activeUsernames.length > 0) {
            username = user.usernames.activeUsernames[0];
        }
        return new TelegramUser(user.id, user.firstName, user.lastName, username, user.phoneNumber);
    }

    private TelegramChat mapChat(TdApi.Chat chat) {
        long lastMessageId = chat.lastMessage == null ? 0L : chat.lastMessage.id;
        return new TelegramChat(chat.id, chat.title, chat.unreadCount, lastMessageId);
    }

    private TelegramMessage mapMessage(TdApi.Message message) {
        if (message == null) return null;
        long senderUserId = 0L;
        if (message.senderId instanceof TdApi.MessageSenderUser) {
            senderUserId = ((TdApi.MessageSenderUser) message.senderId).userId;
        }

        String text = "";
        if (message.content instanceof TdApi.MessageText) {
            TdApi.MessageText messageText = (TdApi.MessageText) message.content;
            text = messageText.text == null ? "" : messageText.text.text;
        }

        return new TelegramMessage(
                message.chatId,
                message.id,
                senderUserId,
                message.isOutgoing,
                message.date,
                text);
    }

    private void notifyError(String code, String message) {
        Listener l = listener;
        if (l != null) l.onError(code, message == null ? "" : message);
    }

    private <T extends TdApi.Object> void sendRaw(
            TdApi.Function<T> request, final ResultAdapter adapter) {
        Client current = client;
        if (current == null) throw new IllegalStateException("Telegram client is not started");

        current.send(request, object -> {
            if (adapter == null) {
                if (object instanceof TdApi.Error) {
                    TdApi.Error error = (TdApi.Error) object;
                    notifyError(String.valueOf(error.code), error.message);
                }
                return;
            }
            adapter.handle(object);
        });
    }

    private ResultAdapter wrapVoid(final TelegramResult<Void> result) {
        return new ResultAdapter() {
            @Override
            public void onSuccess(TdApi.Object object) {
                if (result != null) result.onSuccess(null);
            }

            @Override
            public void onFailure(TdApi.Error error) {
                if (result != null) result.onError(String.valueOf(error.code), error.message);
            }
        };
    }

    private abstract static class ResultAdapter {
        final void handle(TdApi.Object object) {
            if (object instanceof TdApi.Error) {
                onFailure((TdApi.Error) object);
            } else {
                onSuccess(object);
            }
        }

        abstract void onSuccess(TdApi.Object object);
        void onFailure(TdApi.Error error) {}
    }
}
