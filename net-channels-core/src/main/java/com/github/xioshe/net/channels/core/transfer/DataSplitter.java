package com.github.xioshe.net.channels.core.transfer;

import com.github.xioshe.net.channels.core.codec.DataCompressor;
import com.github.xioshe.net.channels.core.crypto.AESCipher;
import com.github.xioshe.net.channels.core.exception.NetChannelsException;
import com.github.xioshe.net.channels.core.model.PacketHeader;
import com.github.xioshe.net.channels.core.model.TransferPacket;
import com.github.xioshe.net.channels.core.protocol.QRCodeProtocol;
import com.github.xioshe.net.channels.core.session.SessionManager;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Builder
public class DataSplitter {
    private static final int DEFAULT_CHUNK_SIZE = 1024; // 1KB
    private final SessionManager sessionManager;
    private final QRCodeProtocol protocol;
    private final DataCompressor compressor;
    private final AESCipher cipher;
    private final int maxQRDataSize;

    private final ConcurrentMap<String, String> processedDataCache = new ConcurrentHashMap<>();
    private final TransferDataCache<List<String>> dataCache;

    public List<String> split(String data, String sessionId) {
        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        try {
            // 压缩原始数据
            String compressedData = compressor.compress(data);
            // 加密压缩后的数据
            String encryptedData = cipher.encrypt(compressedData);

            // 计算分片
            int totalSize = encryptedData.length();
            int chunkSize = calculateOptimalChunkSize(totalSize);
            int totalChunks = (int) Math.ceil((double) totalSize / chunkSize);

            sessionManager.createSession(sessionId, totalChunks, totalSize);
            List<String> packets = new ArrayList<>(totalChunks);

            // 分片处理
            for (int i = 0; i < totalChunks; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, totalSize);
                String chunk = encryptedData.substring(start, end);

                TransferPacket packet = createPacket(sessionId, i, totalChunks,
                        chunkSize, totalSize, chunk);

                // 验证数据包大小是否超过二维码容量
                if (!packet.isWithinSizeLimit(maxQRDataSize)) {
                    throw new NetChannelsException("Packet size exceeds QR code capacity");
                }

                String qrCode = protocol.packetToQRCode(packet);
                packets.add(qrCode);
            }

            // 缓存处理后的数据用于重传
            dataCache.store(sessionId, packets);
            log.info("Split data into {} chunks, sessionId: {}", totalChunks, sessionId);
            return packets;
        } catch (Exception e) {
            processedDataCache.remove(sessionId);
            sessionManager.markSessionFailed(sessionId, e);
            throw new NetChannelsException("Failed to split data", e);
        }
    }

    public List<String> retransmit(String sessionId, List<Integer> chunks) {
        // 确保会话仍然有效
        sessionManager.getSession(sessionId);

        List<String> processedData = dataCache.get(sessionId)
                .orElseThrow(() -> new NetChannelsException("Session data not found: " + sessionId));

        List<String> packets = new ArrayList<>(chunks.size());

        for (Integer chunkIndex : chunks) {
            String chunk = processedData.get(chunkIndex);
            if (chunk == null) {
                throw new NetChannelsException("Chunk %d not found".formatted(chunkIndex));
            }

            packets.add(chunk);
        }

        return packets;
    }

    private TransferPacket createPacket(String sessionId, int currentChunk,
                                        int totalChunks, int chunkSize, int totalSize, String chunk) {
        PacketHeader header = PacketHeader.builder()
                .sessionId(sessionId)
                .currentChunk(currentChunk)
                .totalChunks(totalChunks)
                .chunkSize(chunkSize)
                .totalSize(totalSize)
                .checksum(protocol.calculateChecksum(chunk))
                .version("1.0")
                .encoding("UTF-8")
                .compression("gzip")
                .encryption("aes")
                .build();

        return TransferPacket.builder()
                .header(header)
                .data(chunk)
                .build();
    }

    private int calculateOptimalChunkSize(int totalSize) {
        // 考虑头部信息占用的空间，预留200字节
        int maxDataSize = maxQRDataSize - 200;
        return Math.min(maxDataSize, Math.max(DEFAULT_CHUNK_SIZE,
                totalSize / 10)); // 至少分10片
    }

    public void cleanup(String sessionId) {
        dataCache.remove(sessionId);
    }
}