package com.github.xioshe.net.channels.core.transfer;

import com.github.xioshe.net.channels.common.lock.template.LockTemplate;
import com.github.xioshe.net.channels.core.codec.DataCompressor;
import com.github.xioshe.net.channels.core.crypto.AESCipher;
import com.github.xioshe.net.channels.core.exception.NetChannelsException;
import com.github.xioshe.net.channels.core.model.PacketHeader;
import com.github.xioshe.net.channels.core.model.TransferPacket;
import com.github.xioshe.net.channels.core.model.TransferResult;
import com.github.xioshe.net.channels.core.protocol.QRCodeProtocol;
import com.github.xioshe.net.channels.core.session.SessionManager;
import com.github.xioshe.net.channels.core.session.TransferSession;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Builder
public class DataAssembler {
    private static final int RETRANSMIT_THRESHOLD = 5; // 5秒无新数据触发重传
    private final SessionManager sessionManager;
    private final QRCodeProtocol protocol;
    private final DataCompressor compressor;
    private final AESCipher cipher;
    private final TransferDataCache<StringBuilder> dataCache;
    private final LockTemplate lockTemplate;


    public TransferResult tryAssemble(String qrCodeData) {
        TransferPacket packet = protocol.qrCodeToPacket(qrCodeData);

        if (!protocol.validatePacket(packet)) {
            throw new NetChannelsException("Invalid packet");
        }

        PacketHeader packetHeader = packet.getHeader();
        String sessionId = packet.getHeader().getSessionId();

        try {
            TransferSession session = sessionManager.getOrCreateSession(sessionId,
                    packetHeader.getTotalChunks(),
                    packetHeader.getTotalSize());

            // 在正确的位置插入数据
            StringBuilder buffer = dataCache.get(sessionId,
                    () -> new StringBuilder(packetHeader.getTotalSize()));

            lockTemplate.execute("nc:assembler:" + sessionId, () -> {
                insertChunkData(buffer, packet);
            });

            // 更新会话状态
            sessionManager.updateSession(sessionId, packetHeader.getCurrentChunk());

            // 检查是否所有分片都已接收
            if (session.isComplete()) {
                return TransferResult.builder()
                        .sessionId(sessionId)
                        .status(TransferResult.TransferStatus.COMPLETED)
                        .data(assembleCompleteData(sessionId, buffer))
                        .progress(1.0)
                        .build();
            }

            // 返回进度信息
            List<Integer> missing = session.getMissingChunks();
            return TransferResult.builder()
                    .status(TransferResult.TransferStatus.IN_PROGRESS)
                    .sessionId(sessionId)
                    .missingChunks(missing)
                    .progress(session.getProgress())
                    .build();
        } catch (Exception e) {
            sessionManager.markSessionFailed(sessionId, e);
            throw new NetChannelsException("Failed to process QR code", e);
        }
    }

    private void insertChunkData(StringBuilder buffer, TransferPacket packet) {
        int position = packet.getHeader().getCurrentChunk() *
                       packet.getHeader().getChunkSize();

        String data = packet.getData();

        // 确保buffer有足够空间
        if (buffer.length() < position + data.length()) {
            buffer.setLength(position + data.length());
        }
        buffer.replace(position, position + data.length(), data);
    }

    private String assembleCompleteData(String sessionId, StringBuilder buffer) {
        try {
            String assembledData = buffer.toString();
            // 解密
            String decryptedData = cipher.decrypt(assembledData);
            // 解压
            String decompressedData = compressor.decompress(decryptedData);

            log.info("Successfully assembled data for session: {}", sessionId);
            cleanup(sessionId);
            return decompressedData;
        } catch (Exception e) {
            throw new NetChannelsException("Failed to assemble data", e);
        }
    }

    private void cleanup(String sessionId) {
        dataCache.remove(sessionId);
        try {
            sessionManager.removeSession(sessionId);
        } catch (Exception e) {
            log.warn("Failed to remove session: {}", sessionId, e);
        }
    }
}