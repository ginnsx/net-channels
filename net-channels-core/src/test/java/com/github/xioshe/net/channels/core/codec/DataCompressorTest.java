package com.github.xioshe.net.channels.core.codec;

import com.github.xioshe.net.channels.core.exception.NetChannelsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataCompressorTest {
    private final DataCompressor compressor = new DataCompressor();

    @Test
    void shouldCompressAndDecompressSuccessfully() {
        String originalData = "Hello, World! ".repeat(100);
        String compressed = compressor.compress(originalData);
        String decompressed = compressor.decompress(compressed);

        assertTrue(compressed.length() < originalData.length());
        assertEquals(originalData, decompressed);
    }

    @Test
    void shouldHandleEmptyInput() {
        assertThrows(NetChannelsException.class, () ->
                compressor.compress(""));
        assertThrows(NetChannelsException.class, () ->
                compressor.compress(null));
    }

    @Test
    void shouldHandleSpecialCharacters() {
        String originalData = "特殊字符测试：!@#$%^&*()_+你好世界";
        String compressed = compressor.compress(originalData);
        String decompressed = compressor.decompress(compressed);

        assertEquals(originalData, decompressed);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Invalid Base64!",
            "ThisIsNotBase64",
            "====",
    })
    void shouldHandleInvalidBase64Input(String invalidInput) {
        assertThrows(NetChannelsException.class, () ->
                compressor.decompress(invalidInput));
    }

    @Test
    void shouldHandleLargeData() {
        // 创建一个大约1MB的字符串
        String originalData = "Large Data Test ".repeat(50000);
        String compressed = compressor.compress(originalData);
        String decompressed = compressor.decompress(compressed);

        assertTrue(compressed.length() < originalData.length());
        assertEquals(originalData, decompressed);
    }
}