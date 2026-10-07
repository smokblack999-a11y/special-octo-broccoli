package com.samuray.telegram.tdlib;

import com.samuray.telegram.core.TelegramAuthState;
import com.samuray.telegram.core.TelegramChat;
import com.samuray.telegram.core.TelegramClient;
import com.samuray.telegram.core.TelegramConfig;
import com.samuray.telegram.core.TelegramMessage;
import com.samuray.telegram.core.TelegramResult;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        } else if (object instanceof TdApi.UpdateNewMessage) {
            notifyMessage(((TdApi.UpdateNewMessage) object).message);
        } else if (object instanceof TdApi.UpdateMessageSendSucceeded) {
            notifyMessage(((TdApi.UpdateMessageSendSucceeded) object).message);
        } else if (object instanceof TdApi.UpdateMessageSendFailed) {
            TdApi.UpdateMessageSendFailed update = (TdApi.UpdateMessageSendFailed) object;
            notifyError(String.valueOf(update.error.code), update.error.message);
        } else if (object instanceof TdApi.UpdateChat) {
            notifyChat(((TdApi.UpdateChat) object).chat);
        }
    }

    private void handleAuthorizationState(TdApi.AuthorizationState state) {
        if (state instanceof TdApi.AuthorizationStateWaitTdlibParameters) {
            setAuthState(TelegramAuthState.WAIT_PARAMETERS);
            TdApi.SetTdlibParameters request = new TdApi.SetTdlibParameters();
            request.databaseDirectory = config.databaseDirectory.getAbsolutePath();
            request.filesDirectory = config.filesDirectory.getAbsolutePath();
            request.useMessageDatabase = true;
            request.useChatInfoDatabase = true;
            request.useFileDatabase = true;
            request.useChatDatabase = true;
            request.useSecretChats = true;
            request.apiId = config.apiId;
            request.apiHash = config.apiHash;
            request.systemLanguageCode = config.systemLanguageCode;
            request.deviceModel = config.deviceModel;
            request.systemVersion = config.systemVersion;
            request.applicationVersion = config.applicationVersion;
            request.enableStorageOptimizer = true;
            sendRaw(request, null);
        } else if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
            setAuthState(TelegramAuthState.WAIT_PHONE_NUMBER);
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
        sendRaw(new TdApi.CheckAuthenticationEmailCode(
                new TdApi.EmailAddressAuthenticationCode(code)), wrapVoid(result));
    }

    @Override
    public void getChats(long offsetOrder, long offsetChatId, int limit,
                         final TelegramResult<List<TelegramChat>> result) {
        requireClient();
        sendRaw(new TdApi.GetChats(offsetOrder, offsetChatId, limit), new ResultAdapter() {
            @Override
            public void onSuccess(TdApi.Object object) {
                TdApi.Chats chats = (TdApi.Chats) object;
                ArrayList<TelegramChat> out = new ArrayList<>(chats.chatIds.length);
                for (long id : chats.chatIds) {
                    requestChat(id, out);
                }
                result.onSuccess(Collections.unmodifiableList(out));
            }

            @Override
            public void onFailure(TdApi.Error error) {
                if (result != null) result.onError(String.valueOf(error.code), error.message);
            }
        });
    }

    private void requestChat(final long id, final List<TelegramChat> target) {
        sendRaw(new TdApi.GetChat(id), new ResultAdapter() {
            @Override
            public void onSuccess(TdApi.Object object) {
                TdApi.Chat chat = (TdApi.Chat) object;
                target.add(mapChat(chat));
                notifyChat(chat);
            }
        });
    }

    @Override
    public void searchChats(String query, int limit, final TelegramResult<List<TelegramChat>> result) {
        requireClient();
        sendRaw(new TdApi.SearchChats(query == null ? "" : query, limit), new ResultAdapter() {
            @Override
            public void onSuccess(TdApi.Object object) {
                TdApi.Chats chats = (TdApi.Chats) object;
                ArrayList<TelegramChat> out = new ArrayList<>(chats.chatIds.length);
                for (long id : chats.chatIds) requestChat(id, out);
                result.onSuccess(Collections.unmodifiableList(out));
            }

            @Override
            public void onFailure(TdApi.Error error) {
                if (result != null) result.onError(String.valueOf(error.code), error.message);
            }
        });
    }

    @Override
    public void getChatHistory(long chatId, long fromMessageId, int offset, int limit,
                               final TelegramResult<List<TelegramMessage>> result) {
        requireClient();
        sendRaw(new TdApi.GetChatHistory(chatId, fromMessageId, offset, limit), new ResultAdapter() {
            @Override
            public void onSuccess(TdApi.Object object) {
                TdApi.Messages messages = (TdApi.Messages) object;
                ArrayList<TelegramMessage> out = new ArrayList<>(messages.messages.length);
                for (TdApi.Message message : messages.messages) {
                    TelegramMessage mapped = mapMessage(message);
                    if (mapped != null) out.add(mapped);
                }
                result.onSuccess(Collections.unmodifiableList(out));
            }

            @Override
            public void onFailure(TdApi.Error error) {
                if (result != null) result.onError(String.valueOf(error.code), error.message);
            }
        });
    }

    @Override
    public void sendText(long chatId, String text, TelegramResult<TelegramMessage> result) {
        requireClient();
        TdApi.InputMessageText content = new TdApi.InputMessageText();
        content.text = new TdApi.FormattedText(text == null ? "" : text, null);
        content.disableWebPagePreview = false;
        content.clearDraft = true;

        TdApi.SendMessage request = new TdApi.SendMessage();
        request.chatId = chatId;
        request.replyToMessageId = 0;
        request.disableNotification = false;
        request.fromBackground = false;
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
    public void sendPhoto(long chatId, String localPath, String caption, TelegramResult<TelegramMessage> result) {
        requireClient();
        TdApi.InputMessagePhoto content = new TdApi.InputMessagePhoto();
        content.photo = new TdApi.InputFileLocal(localPath);
        content.caption = caption == null ? "" : caption;

        TdApi.SendMessage request = new TdApi.SendMessage();
        request.chatId = chatId;
        request.replyToMessageId = 0;
        request.disableNotification = false;
        request.fromBackground = false;
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
        sendRaw(new TdApi.DownloadFile(fileId, priority, 0, 0, false), new ResultAdapter() {
            @Override
            public void onSuccess(TdApi.Object object) {
                TdApi.File file = (TdApi.File) object;
                result.onSuccess(file.local.path);
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
    public void viewMessages(long chatId, int[] messageIds) {
        requireClient();
        sendRaw(new TdApi.ViewMessages(chatId, messageIds, new TdApi.MessageSourceUnknown(), true), null);
    }

    @Override
    public void logout(TelegramResult<Void> result) {
        requireClient();
        sendRaw(new TdApi.LogOut(), wrapVoid(result));
    }

    @Override
    public synchronized void close() {
        if (client == null) return;
        sendRaw(new TdApi.Close(), null);
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
        if (chat == null) return;
        Listener l = listener;
        if (l != null) {
            int topId = chat.topMessage == null ? 0 : chat.topMessage.id;
            l.onChatChanged(new TelegramChat(chat.id, chat.title, chat.unreadCount, topId));
        }
    }

    private void notifyMessage(TdApi.Message message) {
        if (message == null) return;
        TelegramMessage mapped = mapMessage(message);
        if (mapped != null) {
            Listener l = listener;
            if (l != null) l.onMessageChanged(mapped);
        }
    }

    private TelegramChat mapChat(TdApi.Chat chat) {
        int topId = chat.topMessage == null ? 0 : chat.topMessage.id;
        return new TelegramChat(chat.id, chat.title, chat.unreadCount, topId);
    }

    private TelegramMessage mapMessage(TdApi.Message message) {
        long senderUserId = 0;
        if (message.senderId instanceof TdApi.MessageSenderUser) {
            senderUserId = ((TdApi.MessageSenderUser) message.senderId).userId;
        }

        String text = "";
        if (message.content instanceof TdApi.MessageText) {
            text = ((TdApi.MessageText) message.content).text.text;
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

    private void sendRaw(TdApi.TLFunction request, final ResultAdapter adapter) {
        client.send(request, object -> {
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

        public abstract void onSuccess(TdApi.Object object);
        public void onFailure(TdApi.Error error) {}
    }
}
