package com.github.xioshe.net.channels.core.crypto;

import com.github.xioshe.net.channels.core.exception.CryptoException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AESCipherTest {

    private final AESCipher cipher = new AESCipher("1234567890123456");

    @Test
    void shouldEncryptAndDecryptSuccessfully() {
        String raw = "Hello, World!";
        byte[] originalData = raw.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = cipher.encrypt(originalData);
        byte[] decrypted = cipher.decrypt(encrypted);

        assertNotEquals(originalData, encrypted);
        assertEquals(raw, new String(decrypted, StandardCharsets.UTF_8));
    }

    @Test
    void shouldThrowExceptionOnInvalidKey() {
        assertThrows(CryptoException.class, () -> {
            new AESCipher("short").encrypt("test".getBytes(StandardCharsets.UTF_8));
        });
    }
}