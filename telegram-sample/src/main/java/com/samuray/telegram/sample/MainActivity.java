package com.samuray.telegram.sample;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.samuray.telegram.core.TelegramAuthState;
import com.samuray.telegram.core.TelegramChat;
import com.samuray.telegram.core.TelegramClient;
import com.samuray.telegram.core.TelegramConfig;
import com.samuray.telegram.core.TelegramMessage;
import com.samuray.telegram.core.TelegramResult;
import com.samuray.telegram.core.TelegramUser;
import com.samuray.telegram.tdlib.TelegramClients;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int PICK_PHOTO = 1001;

    private EditText apiIdInput;
    private EditText apiHashInput;
    private EditText phoneInput;
    private EditText codeInput;
    private EditText passwordInput;
    private EditText emailInput;
    private EditText emailCodeInput;
    private EditText messageInput;
    private TextView statusView;
    private TextView chatTitleView;
    private TextView messagesView;
    private ListView chatsList;
    private ArrayAdapter<String> chatsAdapter;

    private final List<TelegramChat> chatModels = new ArrayList<>();
    private TelegramClient client;
    private long selectedChatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        statusView = new TextView(this);
        statusView.setText("STOPPED");
        root.addView(statusView, new LinearLayout.LayoutParams(-1, -2));

        apiIdInput = field("Telegram API ID", InputType.TYPE_CLASS_NUMBER);
        apiHashInput = field("Telegram API hash", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        phoneInput = field("Phone, international format", InputType.TYPE_CLASS_PHONE);
        codeInput = field("Login code", InputType.TYPE_CLASS_NUMBER);
        passwordInput = field("2FA password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        emailInput = field("Login email (when requested)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailCodeInput = field("Email code", InputType.TYPE_CLASS_NUMBER);
        messageInput = field("Message", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        root.addView(apiIdInput);
        root.addView(apiHashInput);
        root.addView(phoneInput);
        root.addView(codeInput);
        root.addView(passwordInput);
        root.addView(emailInput);
        root.addView(emailCodeInput);

        LinearLayout authButtons = new LinearLayout(this);
        authButtons.setOrientation(LinearLayout.HORIZONTAL);

        Button start = button("START");
        Button sendPhone = button("PHONE");
        Button sendCode = button("CODE");
        Button sendPassword = button("2FA");
        Button sendEmail = button("EMAIL");
        Button sendEmailCode = button("EMAIL CODE");
        Button qr = button("QR");

        authButtons.addView(start);
        authButtons.addView(sendPhone);
        authButtons.addView(sendCode);
        authButtons.addView(sendPassword);
        authButtons.addView(sendEmail);
        authButtons.addView(sendEmailCode);
        authButtons.addView(qr);
        root.addView(authButtons);

        start.setOnClickListener(v -> startTelegram());
        sendPhone.setOnClickListener(v -> {
            if (client == null) return;
            client.setPhoneNumber(phoneInput.getText().toString().trim(), simpleResult("Phone request"));
        });
        sendCode.setOnClickListener(v -> {
            if (client == null) return;
            client.setAuthenticationCode(codeInput.getText().toString().trim(), simpleResult("Code check"));
        });
        sendPassword.setOnClickListener(v -> {
            if (client == null) return;
            client.setAuthenticationPassword(passwordInput.getText().toString(), simpleResult("Password check"));
        });
        sendEmail.setOnClickListener(v -> {
            if (client == null) return;
            client.setEmailAddress(emailInput.getText().toString().trim(), simpleResult("Email request"));
        });
        sendEmailCode.setOnClickListener(v -> {
            if (client == null) return;
            client.setEmailCode(emailCodeInput.getText().toString().trim(), simpleResult("Email code check"));
        });
        qr.setOnClickListener(v -> {
            if (client == null) return;
            client.requestQrCodeAuthentication(simpleResult("QR request"));
        });

        Button reloadChats = button("LOAD CHATS");
        reloadChats.setOnClickListener(v -> loadChats());
        root.addView(reloadChats);

        chatTitleView = new TextView(this);
        chatTitleView.setText("No chat selected");
        root.addView(chatTitleView);

        chatsList = new ListView(this);
        chatsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        chatsList.setAdapter(chatsAdapter);
        chatsList.setOnItemClickListener((parent, view, position, id) -> {
            if (position < chatModels.size()) openChat(chatModels.get(position));
        });
        root.addView(chatsList, new LinearLayout.LayoutParams(-1, 0, 1f));

        messagesView = new TextView(this);
        messagesView.setTextIsSelectable(true);
        root.addView(messagesView, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout sendRow = new LinearLayout(this);
        sendRow.setOrientation(LinearLayout.HORIZONTAL);
        sendRow.addView(messageInput, new LinearLayout.LayoutParams(0, -2, 1f));

        Button sendText = button("SEND");
        Button pickPhoto = button("PHOTO");
        sendRow.addView(sendText);
        sendRow.addView(pickPhoto);
        root.addView(sendRow);

        sendText.setOnClickListener(v -> sendTextMessage());
        pickPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, PICK_PHOTO);
        });

        setContentView(root);
    }

    private EditText field(String hint, int type) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setInputType(type);
        return field;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        return button;
    }

    private void startTelegram() {
        final int apiId;
        try {
            apiId = Integer.parseInt(apiIdInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            status("API ID is invalid");
            return;
        }

        String apiHash = apiHashInput.getText().toString().trim();
        if (apiHash.isEmpty()) {
            status("API hash is required");
            return;
        }

        try {
            byte[] key = SampleKeyStore.getOrCreateDatabaseKey(this);
            File root = new File(getFilesDir(), "telegram");
            File db = new File(root, "db");
            File files = new File(root, "files");

            TelegramConfig config = new TelegramConfig(
                    apiId,
                    apiHash,
                    db,
                    files,
                    key,
                    "en",
                    android.os.Build.MODEL,
                    android.os.Build.VERSION.RELEASE,
                    "0.1.0");

            client = TelegramClients.create(config);
            client.setListener(new TelegramClient.Listener() {
                @Override public void onAuthStateChanged(TelegramAuthState state) {
                    runOnUiThread(() -> status(state.name()));
                    if (state == TelegramAuthState.READY) {
                        runOnUiThread(MainActivity.this::loadAccount);
                    }
                }

                @Override public void onOtherDeviceConfirmation(String link) {
                    runOnUiThread(() -> status("QR LINK: " + link));
                }

                @Override public void onChatChanged(TelegramChat chat) {
                    // The list is reloaded explicitly to keep the sample deterministic.
                }

                @Override public void onMessageChanged(TelegramMessage message) {
                    if (message != null && message.chatId == selectedChatId) {
                        runOnUiThread(() -> appendLiveMessage(message));
                    }
                }

                @Override public void onError(String code, String message) {
                    runOnUiThread(() -> status("ERROR " + code + ": " + message));
                }
            });
            client.start();
        } catch (Exception e) {
            status("START ERROR: " + e.getMessage());
        }
    }

    private void loadAccount() {
        if (client == null) return;
        client.getMe(new TelegramResult<TelegramUser>() {
            @Override public void onSuccess(TelegramUser user) {
                runOnUiThread(() -> status("READY: " + user.displayName()));
            }
            @Override public void onError(String code, String message) {
                runOnUiThread(() -> status("ME ERROR " + code + ": " + message));
            }
        });
        loadChats();
    }

    private void loadChats() {
        if (client == null || client.getAuthState() != TelegramAuthState.READY) return;
        client.loadChats(100, new TelegramResult<Void>() {
            @Override public void onSuccess(Void value) {
                client.getChats(100, new TelegramResult<List<TelegramChat>>() {
                    @Override public void onSuccess(List<TelegramChat> chats) {
                        runOnUiThread(() -> renderChats(chats));
                    }
                    @Override public void onError(String code, String message) {
                        runOnUiThread(() -> status("CHATS ERROR " + code + ": " + message));
                    }
                });
            }

            @Override public void onError(String code, String message) {
                runOnUiThread(() -> status("LOAD CHATS ERROR " + code + ": " + message));
            }
        });
    }

    private void renderChats(List<TelegramChat> chats) {
        chatModels.clear();
        chatModels.addAll(chats);
        chatsAdapter.clear();
        for (TelegramChat chat : chats) {
            chatsAdapter.add(chat.title + (chat.unreadCount > 0 ? " [" + chat.unreadCount + "]" : ""));
        }
        chatsAdapter.notifyDataSetChanged();
        status("CHATS: " + chats.size());
    }

    private void openChat(TelegramChat chat) {
        selectedChatId = chat.id;
        chatTitleView.setText(chat.title);
        messagesView.setText("");
        if (client == null) return;
        client.openChat(chat.id);
        client.getChatHistory(chat.id, 0L, 0, 50, new TelegramResult<List<TelegramMessage>>() {
            @Override public void onSuccess(List<TelegramMessage> messages) {
                runOnUiThread(() -> renderHistory(messages));
            }
            @Override public void onError(String code, String message) {
                runOnUiThread(() -> status("HISTORY ERROR " + code + ": " + message));
            }
        });
    }

    private void renderHistory(List<TelegramMessage> messages) {
        StringBuilder out = new StringBuilder();
        for (TelegramMessage message : messages) {
            out.append(message.outgoing ? "ME" : "THEM")
                    .append(": ")
                    .append(message.text)
                    .append('\n');
        }
        messagesView.setText(out.toString());
        status("MESSAGES: " + messages.size());
    }

    private void appendLiveMessage(TelegramMessage message) {
        String prefix = message.outgoing ? "ME" : "THEM";
        messagesView.append(prefix + ": " + message.text + "\n");
    }

    private void sendTextMessage() {
        if (client == null || selectedChatId == 0L) {
            status("Select a chat first");
            return;
        }
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;
        client.sendText(selectedChatId, text, new TelegramResult<TelegramMessage>() {
            @Override public void onSuccess(TelegramMessage message) {
                runOnUiThread(() -> messageInput.setText(""));
            }
            @Override public void onError(String code, String message) {
                runOnUiThread(() -> status("SEND ERROR " + code + ": " + message));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PHOTO || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            final String localPath = copyToCache(uri);
            client.sendPhoto(selectedChatId, localPath, "", new TelegramResult<TelegramMessage>() {
                @Override public void onSuccess(TelegramMessage message) {
                    runOnUiThread(() -> status("PHOTO SENT"));
                }
                @Override public void onError(String code, String message) {
                    runOnUiThread(() -> status("PHOTO ERROR " + code + ": " + message));
                }
            });
        } catch (Exception e) {
            status("PHOTO COPY ERROR: " + e.getMessage());
        }
    }

    private String copyToCache(Uri uri) throws Exception {
        File file = new File(getCacheDir(), "telegram-" + System.currentTimeMillis() + ".jpg");
        try (InputStream input = getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(file)) {
            if (input == null) throw new IllegalStateException("Cannot open selected file");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        }
        return file.getAbsolutePath();
    }

    private TelegramResult<Void> simpleResult(final String operation) {
        return new TelegramResult<Void>() {
            @Override public void onSuccess(Void value) {
                runOnUiThread(() -> status(operation + ": OK"));
            }
            @Override public void onError(String code, String message) {
                runOnUiThread(() -> status(operation + ": ERROR " + code + ": " + message));
            }
        };
    }

    private void status(String text) {
        statusView.setText(text);
    }

    @Override
    protected void onDestroy() {
        if (client != null) {
            client.close();
            client = null;
        }
        super.onDestroy();
    }
}
