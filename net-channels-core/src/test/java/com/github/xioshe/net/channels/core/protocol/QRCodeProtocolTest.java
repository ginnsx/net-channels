package com.github.xioshe.net.channels.core.protocol;

import com.github.xioshe.net.channels.core.exception.NetChannelsException;
import com.github.xioshe.net.channels.core.model.PacketHeader;
import com.github.xioshe.net.channels.core.model.TransferPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QRCodeProtocolTest {
    private final QRCodeProtocol protocol = new QRCodeProtocol();
    private TransferPacket validPacket;

    @BeforeEach
    void setUp() {
        var header = PacketHeader.builder()
                .sessionId("test-session")
                .totalChunks(10)
                .currentChunk(1)
                .checksum(protocol.calculateChecksum("test-data"))
                .build();

        validPacket = TransferPacket.builder()
                .header(header)
                .data("test-data")
                .build();
    }

    @Test
    void shouldHandleValidPacket() {
        String qrCode = protocol.packetToQRCode(validPacket);
        TransferPacket deserializedPacket = protocol.qrCodeToPacket(qrCode);

        assertEquals(validPacket.getHeader().getSessionId(),
                deserializedPacket.getHeader().getSessionId());
        assertEquals(validPacket.getData(), deserializedPacket.getData());
        assertTrue(protocol.validatePacket(deserializedPacket));
    }

    @Test
    void shouldRejectOversizedPacket() {
        // 创建一个超大的数据包
        String largeData = "x".repeat(3000);
        var header = validPacket.getHeader();
        TransferPacket largePacket = TransferPacket.builder()
                .header(header)
                .data(largeData)
                .build();

        assertThrows(NetChannelsException.class, () ->
                protocol.packetToQRCode(largePacket));
    }

    @Test
    void shouldValidatePacketIntegrity() {
        // 测试无效的分片号
        var invalidHeader = PacketHeader.builder()
                .sessionId("test-session")
                .totalChunks(10)
                .currentChunk(11) // 无效的分片号
                .checksum(protocol.calculateChecksum("test-data"))
                .build();

        TransferPacket invalidPacket = TransferPacket.builder()
                .header(invalidHeader)
                .data("test-data")
                .build();

        assertFalse(protocol.validatePacket(invalidPacket));
    }

    @Test
    void shouldHandleNullAndEmptyInputs() {
        assertThrows(IllegalArgumentException.class, () ->
                protocol.packetToQRCode(null));
        assertThrows(IllegalArgumentException.class, () ->
                protocol.qrCodeToPacket(null));
        assertThrows(IllegalArgumentException.class, () ->
                protocol.qrCodeToPacket(""));
    }

    @Test
    void shouldValidateChecksumCorrectly() {
        String testData = "test-data";
        String checksum = protocol.calculateChecksum(testData);
        String differentChecksum = protocol.calculateChecksum("different-data");

        assertNotEquals(checksum, differentChecksum);
    }
}