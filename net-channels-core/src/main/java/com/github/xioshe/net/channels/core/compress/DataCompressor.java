package com.github.xioshe.net.channels.core.compress;

import com.github.xioshe.net.channels.core.exception.NetChannelsException;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 数据压缩工具类，用 gzip 将原始数据（String）压缩为更小的数据。
 * <br/>
 * 对于 JSON 数据，能保证 80% 的压缩率
 */
@Slf4j
public class DataCompressor {
    private static final int BUFFER_SIZE = 8192; // 8KB buffer

    public byte[] compress(byte[] data) {
        if (data == null || data.length == 0) {
            throw new NetChannelsException("Input data cannot be null or empty");
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
            gzip.finish(); // 关闭流，以写入剩余数据
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to compress data", e);
            throw new NetChannelsException("Compression failed", e);
        }
    }

    public byte[] decompress(byte[] compressedData) {
        if (compressedData == null || compressedData.length == 0) {
            throw new NetChannelsException("Compressed data cannot be null or empty");
        }

        try {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
                 GZIPInputStream gzipIn = new GZIPInputStream(bis);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int len;
                while ((len = gzipIn.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }

                return bos.toByteArray();
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 input", e);
            throw new NetChannelsException("Invalid Base64 input: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to decompress data", e);
            throw new NetChannelsException("Decompression failed: " + e.getMessage(), e);
        }
    }
}