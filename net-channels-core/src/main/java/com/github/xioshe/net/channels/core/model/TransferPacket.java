package com.github.xioshe.net.channels.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 数据传输包
 * 用于封装传输的数据和元数据
 */
@Data
@Builder
public class TransferPacket implements Serializable {
    private PacketHeader header;
    private String data;

    // 添加用于Jackson反序列化的构造函数
    @JsonCreator
    public TransferPacket(
            @JsonProperty("header") PacketHeader header,
            @JsonProperty("data") String data) {
        if (header == null) {
            throw new IllegalArgumentException("Header cannot be null");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        this.header = header;
        this.data = data;
    }

    /**
     * 获取数据包的大小估算（字节）
     */
    public int estimateSize() {
        return data.length() + 200; // 200是header的估算大小
    }

    /**
     * 验证数据包是否超过最大允许大小
     */
    public boolean isWithinSizeLimit(int maxSize) {
        return estimateSize() <= maxSize;
    }
}