package com.samuray.telegram.core;

public interface TelegramResult<T> {
    void onSuccess(T value);
    void onError(String code, String message);
}
