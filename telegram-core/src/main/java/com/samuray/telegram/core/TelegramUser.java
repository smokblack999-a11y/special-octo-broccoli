package com.samuray.telegram.core;

public final class TelegramUser {
    public final long id;
    public final String firstName;
    public final String lastName;
    public final String username;
    public final String phoneNumber;

    public TelegramUser(long id, String firstName, String lastName, String username, String phoneNumber) {
        this.id = id;
        this.firstName = firstName == null ? "" : firstName;
        this.lastName = lastName == null ? "" : lastName;
        this.username = username == null ? "" : username;
        this.phoneNumber = phoneNumber == null ? "" : phoneNumber;
    }

    public String displayName() {
        String full = (firstName + " " + lastName).trim();
        return full.isEmpty() ? username : full;
    }
}
