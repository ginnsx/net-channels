package com.github.xioshe.net.channels.core.crypto;

import com.github.xioshe.net.channels.core.exception.CryptoException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * AES加密解密
 */
public class AESCipher {
    private static final String ALGORITHM = "AES";
    private final SecretKeySpec secretKey;

    public AESCipher(String key) {
        if (key.length() != 16) {
            throw new CryptoException("Invalid key length, must 128 bits(16 bytes)");
        }
        this.secretKey = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public byte[] encrypt(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new CryptoException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new CryptoException("Decryption failed", e);
        }
    }
}