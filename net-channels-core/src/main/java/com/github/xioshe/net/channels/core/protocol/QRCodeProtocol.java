package com.github.xioshe.net.channels.core.protocol;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xioshe.net.channels.core.exception.NetChannelsException;
import com.github.xioshe.net.channels.core.model.TransferPacket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

@Slf4j
public class QRCodeProtocol implements TransferProtocol {
    private static final int MAX_QR_DATA_SIZE = 2953; // QR 码最大容量
    private final ObjectMapper objectMapper;

    public QRCodeProtocol() {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
    }

    @Override
    public String packetToQRCode(TransferPacket packet) {
        validatePacketBeforeConversion(packet);
        try {
            String json = objectMapper.writeValueAsString(packet);
            if (json.length() > MAX_QR_DATA_SIZE) {
                throw new NetChannelsException(
                        String.format("Packet size %d exceeds maximum QR code capacity %d",
                                json.length(), MAX_QR_DATA_SIZE));
            }
            return json;
        } catch (Exception e) {
            log.error("Failed to serialize packet: {}", e.getMessage());
            throw new NetChannelsException("Failed to serialize packet", e);
        }
    }

    @Override
    public TransferPacket qrCodeToPacket(String qrCodeData) {
        validateQRCodeData(qrCodeData);
        try {
            TransferPacket packet = objectMapper.readValue(qrCodeData, TransferPacket.class);
            if (!validatePacket(packet)) {
                throw new NetChannelsException("Packet validation failed");
            }
            return packet;
        } catch (Exception e) {
            log.error("Failed to deserialize packet: {}", e.getMessage());
            throw new NetChannelsException("Failed to deserialize packet", e);
        }
    }

    @Override
    public String calculateChecksum(String data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        return DigestUtils.md5Hex(data);
    }

    @Override
    public boolean validatePacket(TransferPacket packet) {
        if (packet == null || packet.getHeader() == null || packet.getData() == null) {
            return false;
        }

        // 验证会话ID
        if (packet.getHeader().getSessionId() == null ||
            packet.getHeader().getSessionId().isEmpty()) {
            return false;
        }

        // 验证分片信息
        if (packet.getHeader().getCurrentChunk() < 0 ||
            packet.getHeader().getTotalChunks() <= 0 ||
            packet.getHeader().getCurrentChunk() >= packet.getHeader().getTotalChunks()) {
            return false;
        }

        // 验证校验和
        String calculatedChecksum = calculateChecksum(packet.getData());
        return calculatedChecksum.equals(packet.getHeader().getChecksum());
    }

    private void validatePacketBeforeConversion(TransferPacket packet) {
        if (packet == null) {
            throw new IllegalArgumentException("Packet cannot be null");
        }
        if (packet.getHeader() == null) {
            throw new IllegalArgumentException("Packet header cannot be null");
        }
        if (packet.getData() == null) {
            throw new IllegalArgumentException("Packet data cannot be null");
        }
    }

    private void validateQRCodeData(String qrCodeData) {
        if (qrCodeData == null || qrCodeData.isEmpty()) {
            throw new IllegalArgumentException("QR code data cannot be null or empty");
        }
        if (qrCodeData.length() > MAX_QR_DATA_SIZE) {
            throw new IllegalArgumentException(
                    String.format("QR code data size %d exceeds maximum capacity %d",
                            qrCodeData.length(), MAX_QR_DATA_SIZE));
        }
    }
}