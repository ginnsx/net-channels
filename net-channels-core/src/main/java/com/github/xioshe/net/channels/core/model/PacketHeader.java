package com.github.xioshe.net.channels.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacketHeader {
    private String sessionId;      // 会话ID
    private int totalChunks;       // 总分片数
    private int currentChunk;      // 当前分片序号
    private int chunkSize;         // 分片大小
    private int totalSize;        // 总数据大小
    private String checksum;       // 数据校验和
    private long timestamp;        // 时间戳
    private String version;        // 协议版本
    private String encoding;       // 编码方式
    private String compression;    // 压缩方式
    private String encryption;     // 加密方式
}