package com.github.xioshe.net.channels.core.transfer;

import com.github.xioshe.net.channels.core.cache.TransferDataCache;
import com.github.xioshe.net.channels.core.compress.DataCompressor;
import com.github.xioshe.net.channels.core.crypto.AESCipher;
import com.github.xioshe.net.channels.core.exception.NetChannelsException;
import com.github.xioshe.net.channels.core.model.PacketHeader;
import com.github.xioshe.net.channels.core.model.TransferPacket;
import com.github.xioshe.net.channels.core.protocol.QRCodeProtocol;
import com.github.xioshe.net.channels.core.session.SessionManager;
import com.github.xioshe.net.channels.core.session.TimestampSessionIdGenerator;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Builder
public class DataSplitter {
    // 商业扫码枪 500-800 字节，工业级 2900-3000 字节，根据实际设备调整
    private static final int DEFAULT_CHUNK_SIZE = 1024; // 1KB
    private final SessionManager sessionManager;
    private final QRCodeProtocol protocol;
    private final DataCompressor compressor;
    private final AESCipher cipher;
    private final TimestampSessionIdGenerator sessionIdGenerator;
    private final int maxQRDataSize;

    private final ConcurrentMap<String, String> processedDataCache = new ConcurrentHashMap<>();
    private final TransferDataCache<List<String>> dataCache;

    public List<String> split(byte[] data) {
        return split(data, sessionIdGenerator.generate());
    }

    public List<String> split(String data, String sessionId) {
        return split(data.getBytes(StandardCharsets.UTF_8), sessionId);
    }

    public List<String> split(byte[] data, String sessionId) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        try {
            // 压缩原始数据
            byte[] compressedData = compressor.compress(data);
            // 加密压缩后的数据
            byte[] encryptedData = cipher.encrypt(compressedData);

            // 计算分片
            int totalSize = encryptedData.length;
            int chunkSize = calculateOptimalChunkSize(totalSize);
            int totalChunks = (int) Math.ceil((double) totalSize / chunkSize);

            sessionManager.createSession(sessionId, totalChunks, totalSize);
            List<String> packets = new ArrayList<>(totalChunks);

            // 分片处理
            for (int i = 0; i < totalChunks; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, totalSize);

                byte[] chunk = Arrays.copyOfRange(encryptedData, start, end);

                TransferPacket packet = createPacket(sessionId, i, totalChunks,
                        chunkSize, totalSize, chunk);

                String qrCode = protocol.packetToQRCode(packet);

                // 验证数据包大小是否超过二维码容量
                if (qrCode.length() > maxQRDataSize) {
                    throw new NetChannelsException("Packet size exceeds QR code capacity");
                }
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
                                        int totalChunks, int chunkSize, int totalSize, byte[] chunk) {
        PacketHeader header = PacketHeader.builder()
                .sessionId(sessionId)
                .currentChunk(currentChunk)
                .totalChunks(totalChunks)
                .chunkSize(chunkSize)
                .totalSize(totalSize)
                .checksum(protocol.calculateChecksum(chunk))
                .build();

        return TransferPacket.builder()
                .header(header)
                .data(chunk)
                .build();
    }

    private int calculateOptimalChunkSize(int totalSize) {
        // 考虑 Base64 编码后的膨胀比例 (4/3)
        // 考虑头部大小 (HEADER_SIZE)
        // 预留一些空间给其他开销
        double base64Factor = 3.0 / 4.0;  // Base64 解码后的比例
        int reservedSpace = 50;  // 预留空间

        // 计算实际可用的数据空间
        int maxEncodedSize = maxQRDataSize - reservedSpace;
        int maxDecodedSize = (int) (maxEncodedSize * base64Factor) - PacketHeader.HEADER_SIZE;

        // 确保分片大小合理
        return Math.min(maxDecodedSize, Math.max(DEFAULT_CHUNK_SIZE,
                totalSize / 10));  // 至少分10片
    }
}