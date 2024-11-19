package com.github.xioshe.net.channels.core.compress;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 测试数据压缩和解压缩的性能。如果需要更精确的性能测试，请使用 jmh。
 */
class DataCompressorPerformanceTest {
    private DataCompressor compressor;
    private Random random;

    @BeforeEach
    void setUp() {
        compressor = new DataCompressor();
        random = new Random();
    }

    @ParameterizedTest
    @ValueSource(ints = {1024, 10_240, 102_400, 1_024_000})
        // 1KB, 10KB, 100KB, 1MB
    void testCompressionRatio(int size) {
        // 生成随机数据
        String raw = generateRandomData(size);
        byte[] data = raw.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = compressor.compress(data);

        double ratio = 1.0 - (double) compressed.length / data.length;
        System.out.printf("Size: %d bytes, Compressed: %d bytes, Space saving ratio: %.2f%%%n",
                data.length, compressed.length, ratio * 100);

        assertTrue(ratio < 1.0, "压缩后的数据应该小于原始数据");
    }

    @ParameterizedTest
    @ValueSource(ints = {1024, 10_240, 102_400, 1_024_000})
    void testCompressionSpeed(int size) {
        String raw = generateRandomData(size);
        byte[] data = raw.getBytes(StandardCharsets.UTF_8);

        long startTime = System.nanoTime();
        byte[] compressed = compressor.compress(data);
        long compressTime = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        byte[] decompressed = compressor.decompress(compressed);
        long decompressTime = System.nanoTime() - startTime;

        assertEquals(raw, new String(decompressed, StandardCharsets.UTF_8), "解压后的数据应该与原始数据相同");

        System.out.printf("Size: %d bytes%n" +
                          "Compression time: %.2f ms%n" +
                          "Decompression time: %.2f ms%n" +
                          "Throughput: %.2f MB/s%n%n",
                size,
                TimeUnit.NANOSECONDS.toMillis(compressTime) / 1000.0,
                TimeUnit.NANOSECONDS.toMillis(decompressTime) / 1000.0,
                (size / 1024.0 / 1024.0) / (TimeUnit.NANOSECONDS.toSeconds(compressTime) + 1));
    }

    @Test
    void testDifferentDataTypes() {
        // 测试重复性高的数据
        String repeatingData = "abc".repeat(10000);
        // 测试JSON数据
        String jsonData = generateJsonData(1000);
        // 测试二进制数据
        String binaryData = generateBinaryData(10000);

        testCompression("重复性数据", repeatingData);
        testCompression("JSON数据", jsonData);
        testCompression("二进制数据", binaryData);
    }

    private void testCompression(String type, String raw) {
        byte[] data = raw.getBytes(StandardCharsets.UTF_8);
        long startTime = System.nanoTime();
        byte[] compressed = compressor.compress(data);
        long compressTime = System.nanoTime() - startTime;

        double ratio = 1.0 - (double) compressed.length / data.length;
        System.out.printf("%s:%n" +
                          "原始大小: %d bytes%n" +
                          "压缩大小: %d bytes%n" +
                          "空间节约率: %.2f%%%n" +
                          "压缩时间: %.2f ms%n%n",
                type,
                data.length,
                compressed.length,
                ratio * 100,
                TimeUnit.NANOSECONDS.toMillis(compressTime) / 1000.0);
    }

    private String generateRandomData(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append((char) (random.nextInt(26) + 'a'));
        }
        return sb.toString();
    }

    private String generateJsonData(int records) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < records; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format(
                    "{\"id\":%d,\"name\":\"user%d\",\"value\":\"%s\"}",
                    i, i, generateRandomData(20)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String generateBinaryData(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append(random.nextInt(2));
        }
        return sb.toString();
    }
}