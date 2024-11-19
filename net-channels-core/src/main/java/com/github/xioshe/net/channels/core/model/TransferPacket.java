package com.github.xioshe.net.channels.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * 数据传输包
 * 用于封装传输的数据和元数据
 */
@Data
@Builder
public class TransferPacket implements Serializable {
    private PacketHeader header;
    private byte[] data;

    // 添加用于Jackson反序列化的构造函数
    @JsonCreator
    public TransferPacket(
            @JsonProperty("header") PacketHeader header,
            @JsonProperty("data") byte[] data) {
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
        return (int) ((data.length + PacketHeader.HEADER_SIZE) * 4 / 3.0);
    }

    /**
     * 验证数据包是否超过最大允许大小
     */
    public boolean isWithinSizeLimit(int maxSize) {
        return estimateSize() <= maxSize;
    }

    public byte[] toBytes() {
        byte[] headerBytes = header.toBytes();
        assert headerBytes.length == PacketHeader.HEADER_SIZE : "headerBytes length must be " + PacketHeader.HEADER_SIZE;

        ByteBuffer buffer = ByteBuffer.allocate(PacketHeader.HEADER_SIZE + 4 + data.length);

        // 写入 header
        buffer.put(headerBytes);

        // 写入 data
        buffer.putInt(data.length);
        buffer.put(data);

        return buffer.array();
    }

    public static TransferPacket fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        // 读取 header
        byte[] headerBytes = new byte[PacketHeader.HEADER_SIZE];
        buffer.get(headerBytes);
        PacketHeader header = PacketHeader.fromBytes(headerBytes);

        // 读取 data
        int dataLength = buffer.getInt();
        byte[] data = new byte[dataLength];
        buffer.get(data);


        return TransferPacket.builder()
                .header(header)
                .data(data)
                .build();
    }
}