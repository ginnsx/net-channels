package com.github.xioshe.net.channels.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacketHeader {
    private static final byte VERSION = 1;

    //                          1 + // 版本号
    //                          9 + // sessionId 长度固定为 9
    //                          4 + // totalChunks
    //                          4 + // currentChunk
    //                          4 + // chunkSize
    //                          4 + // totalSize
    //                          8; // checksum 长度为 8 bytes
    public static final int HEADER_SIZE = 34;

    private String sessionId;      // 会话ID，长度固定为 9 字节
    private int totalChunks;       // 总分片数
    private int currentChunk;      // 当前分片序号
    private int chunkSize;         // 分片大小
    private int totalSize;        // 总数据大小
    private String checksum;       // 数据校验和，使用 crc32c 算法，转换为固定长度 8 bytes 的 hex 字符串
//    private long timestamp;        // 时间戳
//    private String version;        // 协议版本
//    private String encoding;       // 编码方式
//    private String compression;    // 压缩方式
//    private String encryption;     // 加密方式

    /**
     * 将 Header 对象转换为字节数组，以节约空间
     *
     * @return byte[]
     */
    public byte[] toBytes() {
        byte[] sessionIdBytes = sessionId.getBytes(StandardCharsets.UTF_8);
        assert sessionIdBytes.length == 9 : "sessionId length must be 9";
        byte[] checksumBytes = checksum.getBytes(StandardCharsets.UTF_8);
        assert checksumBytes.length == 8 : "checksum length must be 8";


        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
        buffer.put(VERSION);

        // sessionId
        buffer.put(sessionIdBytes);

        // 其他字段
        buffer.putInt(totalChunks);
        buffer.putInt(currentChunk);
        buffer.putInt(chunkSize);
        buffer.putInt(totalSize);

        // checksum
        buffer.put(checksumBytes);

        return buffer.array();
    }

    public static PacketHeader fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        // 读取版本号
        byte version = buffer.get();
        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported version: " + version);
        }

        // 读取 sessionId
        byte[] sessionIdBytes = new byte[9];
        buffer.get(sessionIdBytes);
        String sessionId = new String(sessionIdBytes, StandardCharsets.UTF_8);

        // 读取其他字段
        int totalChunks = buffer.getInt();
        int currentChunk = buffer.getInt();
        int chunkSize = buffer.getInt();
        int totalSize = buffer.getInt();

        // 读取 checksum
        byte[] checksumBytes = new byte[8];
        buffer.get(checksumBytes);
        String checksum = new String(checksumBytes, StandardCharsets.UTF_8);

        return PacketHeader.builder()
                .sessionId(sessionId)
                .totalChunks(totalChunks)
                .currentChunk(currentChunk)
                .chunkSize(chunkSize)
                .totalSize(totalSize)
                .checksum(checksum)
                .build();
    }
}