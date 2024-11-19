package com.github.xioshe.net.channels.core.protocol;

import com.github.xioshe.net.channels.core.exception.NetChannelsException;
import com.github.xioshe.net.channels.core.model.TransferPacket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.PureJavaCrc32C;

import java.util.Base64;

/**
 * 使用 MessagePack 进行序列化，以减小包大小
 */
@Slf4j
public class QRCodeProtocol implements TransferProtocol {
    private static final int MAX_QR_DATA_SIZE = 2953; // QR 码最大容量

    @Override
    public String packetToQRCode(TransferPacket packet) {
        validatePacketBeforeConversion(packet);
        try {
            byte[] bytes = packet.toBytes();
            String result = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            if (result.length() > MAX_QR_DATA_SIZE) {
                throw new NetChannelsException(
                        String.format("Packet size %d exceeds maximum QR code capacity %d",
                                result.length(), MAX_QR_DATA_SIZE));
            }
            return result;
        } catch (Throwable e) {
            log.error("Failed to serialize packet: {}", e.getMessage());
            throw new NetChannelsException("Failed to serialize packet", e);
        }
    }

    @Override
    public TransferPacket qrCodeToPacket(String qrCodeData) {
        validateQRCodeData(qrCodeData);
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(qrCodeData);
            TransferPacket packet = TransferPacket.fromBytes(bytes);
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
    public String calculateChecksum(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        // md5 32 bytes, crc32 only 8 bytes
        PureJavaCrc32C crc32 = new PureJavaCrc32C();
        crc32.update(data);
        return String.format("%08x", crc32.getValue());
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