package com.github.xioshe.net.channels.core.compress;

import com.github.xioshe.net.channels.core.exception.NetChannelsException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataCompressorTest {
    private final DataCompressor compressor = new DataCompressor();

    @Test
    void shouldCompressAndDecompressSuccessfully() {
        String raw = "Hello, World! ".repeat(100);
        byte[] originalData = raw.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = compressor.compress(originalData);
        byte[] decompressed = compressor.decompress(compressed);

        assertTrue(compressed.length < originalData.length);
        assertEquals(raw, new String(decompressed, StandardCharsets.UTF_8));
    }

    @Test
    void shouldHandleEmptyInput() {
        assertThrows(NetChannelsException.class, () ->
                compressor.compress(null));
        assertThrows(NetChannelsException.class, () ->
                compressor.compress(new byte[0]));
    }

    @Test
    void shouldHandleSpecialCharacters() {
        String raw = "特殊字符测试：!@#$%^&*()_+你好世界";
        byte[] originalData = raw.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = compressor.compress(originalData);
        byte[] decompressed = compressor.decompress(compressed);

        assertEquals(raw, new String(decompressed, StandardCharsets.UTF_8));
    }

//    @ParameterizedTest
//    @ValueSource(strings = {
//            "Invalid Base64!",
//            "ThisIsNotBase64",
//            "====",
//    })
//    void shouldHandleInvalidBase64Input(String invalidInput) {
//        assertThrows(NetChannelsException.class, () ->
//                compressor.decompress(invalidInput));
//    }

    @Test
    void shouldHandleLargeData() {
        // 创建一个大约1MB的字符串
        String raw = "Large Data Test ".repeat(50000);
        byte[] originalData = raw.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = compressor.compress(originalData);
        byte[] decompressed = compressor.decompress(compressed);

        assertTrue(compressed.length < originalData.length);
        assertEquals(raw, new String(decompressed, StandardCharsets.UTF_8));
    }
}