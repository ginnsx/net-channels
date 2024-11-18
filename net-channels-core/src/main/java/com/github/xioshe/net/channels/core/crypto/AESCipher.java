package com.github.xioshe.net.channels.core.crypto;

import com.github.xioshe.net.channels.core.exception.CryptoException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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

    public String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            // 直接转换为 String 可能导致乱码，所以需要 Base64 编码
            return Base64.getEncoder().encodeToString(
                    cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new CryptoException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(
                    Base64.getDecoder().decode(encryptedData)));
        } catch (Exception e) {
            throw new CryptoException("Decryption failed", e);
        }
    }
}