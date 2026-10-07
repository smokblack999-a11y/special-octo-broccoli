package com.samuray.telegram.core;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TelegramCoreContractTest {
    @Test
    public void configRequires32ByteDatabaseKey() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TelegramConfig(
                        123,
                        "hash",
                        new File("build/test-db"),
                        new File("build/test-files"),
                        new byte[31],
                        "en",
                        "test",
                        "1",
                        "0.1.0"));
    }

    @Test
    public void userDisplayNamePrefersFullName() {
        TelegramUser user = new TelegramUser(1, "First", "Last", "username", "123");
        assertEquals("First Last", user.displayName());
    }

    @Test
    public void userDisplayNameFallsBackToUsername() {
        TelegramUser user = new TelegramUser(1, "", "", "username", "123");
        assertEquals("username", user.displayName());
    }
}
