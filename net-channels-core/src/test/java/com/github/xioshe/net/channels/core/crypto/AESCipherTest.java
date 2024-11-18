package com.github.xioshe.net.channels.core.crypto;

import com.github.xioshe.net.channels.core.exception.CryptoException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AESCipherTest {

    private final AESCipher cipher = new AESCipher("1234567890123456");

    @Test
    void shouldEncryptAndDecryptSuccessfully() {
        String originalData = "Hello, World!";
        String encrypted = cipher.encrypt(originalData);
        String decrypted = cipher.decrypt(encrypted);

        assertNotEquals(originalData, encrypted);
        assertEquals(originalData, decrypted);
    }

    @Test
    void shouldThrowExceptionOnInvalidKey() {
        assertThrows(CryptoException.class, () -> {
            new AESCipher("short").encrypt("test");
        });
    }
}