# Telegram Core

A standalone Android Telegram client core for embedding into other applications.

## Scope

This project targets a real Telegram user account, not the Bot API. It is designed as an embeddable client layer so camera, gallery, or other application features can depend on a stable Telegram-facing API without depending on Telegram internals.

Telegram transport and local synchronization are provided by TDLib.

## Planned public API

- account authorization and session restore
- chats and chat search
- message history and live updates
- text messages
- photo/video/document upload
- media download
- retry and offline-aware upload state
- clean lifecycle/shutdown
- adapter layer so consuming apps do not depend directly on TDLib

Telegram API credentials (api_id/api_hash) must be supplied by the consuming application or secure build/runtime configuration. Never commit them to the repository.

## Status

Foundation repository initialized. The implementation is being built in vertical slices, starting with the Telegram client engine and authentication state machine.

## Telegram compliance

This is a third-party Telegram client. Use must comply with Telegram's API Terms of Service and applicable platform rules.
