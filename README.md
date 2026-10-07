# Telegram Core

Standalone Android modules for embedding a **real Telegram user-account client** into other applications.

This is intentionally not a Telegram Bot API wrapper. The host application owns the UI and product logic; this project exposes a stable Telegram-facing API and keeps the TDLib implementation behind an adapter boundary.

## Architecture

```
Host Android app
      |
      v
:telegram-core          <- stable public API
      |
      v
:telegram-tdlib         <- TDLib adapter / transport
      |
      v
Telegram network + local TDLib storage
```

A future camera/GPS/gallery application can depend on `:telegram-core` and `:telegram-tdlib` without coupling its camera or gallery code to TDLib classes.

## Current API

The reusable client contract covers:

- normal user-account authorization: phone, code, 2FA password, email verification, QR flow, logout;
- current-account lookup;
- chat loading/search;
- chat history;
- incoming message updates;
- text messages;
- photo upload;
- file download;
- opening/closing chats and marking messages viewed;
- lifecycle and error callbacks.

## Dependency

For fast iteration this repository uses the third-party precompiled Android TDLib distribution:

```
implementation 'io.github.tdlib-android:core:0.1.1'
```

It avoids a local C++/NDK build and provides Android native binaries. The dependency is third-party, so the final production release should pin the exact artifact/checksum or build and verify TDLib in our own CI.

## Credentials and session security

Create the Telegram application credentials (`api_id` and `api_hash`) through Telegram's developer portal.

Never commit `api_hash`, a phone number, authentication code, 2FA password, or a live TDLib database to GitHub.

The reusable API requires a 32-byte TDLib database encryption key. The consuming app should generate it once and persist it in platform-secure storage.

## Example

```java
TelegramConfig config = new TelegramConfig(
        TELEGRAM_API_ID,
        TELEGRAM_API_HASH,
        new File(context.getFilesDir(), "tdlib-db"),
        new File(context.getFilesDir(), "tdlib-files"),
        persisted32ByteKey,
        "en",
        android.os.Build.MODEL,
        android.os.Build.VERSION.RELEASE,
        "1.0.0");

TelegramClient client = TelegramClients.create(config);

client.setListener(new TelegramClient.Listener() {
    @Override public void onAuthStateChanged(TelegramAuthState state) { }
    @Override public void onOtherDeviceConfirmation(String link) { }
    @Override public void onChatChanged(TelegramChat chat) { }
    @Override public void onMessageChanged(TelegramMessage message) { }
    @Override public void onError(String code, String message) { }
});

client.start();
```

When the client reports `WAIT_PHONE_NUMBER`, supply the phone number. Then supply the authentication code and, when required, the 2FA password. On `READY`, the normal user account is available through the chat/message APIs.

## Repository modules

- `:telegram-core` — stable public contracts and data models.
- `:telegram-tdlib` — TDLib-backed implementation.
- `:telegram-sample` — minimal Android app used to verify packaging and integration.

## Roadmap

1. Make the TDLib adapter compile cleanly against the current generated TDLib API.
2. Build a real authentication screen and session restore.
3. Build chats + message screen.
4. Add robust media upload/download progress and retries.
5. Add the camera/GPS/gallery integration as a separate host application layer.
6. Publish a versioned reusable AAR/SDK.

## Compliance

This is a third-party Telegram client. Use it in accordance with Telegram's API Terms of Service and applicable platform rules.
