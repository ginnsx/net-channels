package com.github.xioshe.net.channels.core;

import com.github.xioshe.net.channels.common.lock.template.LockTemplate;
import com.github.xioshe.net.channels.core.cache.TransferDataCache;
import com.github.xioshe.net.channels.core.compress.DataCompressor;
import com.github.xioshe.net.channels.core.crypto.AESCipher;
import com.github.xioshe.net.channels.core.model.TransferPacket;
import com.github.xioshe.net.channels.core.model.TransferResult;
import com.github.xioshe.net.channels.core.protocol.QRCodeProtocol;
import com.github.xioshe.net.channels.core.session.InMemorySessionStorage;
import com.github.xioshe.net.channels.core.session.SessionManager;
import com.github.xioshe.net.channels.core.session.TimestampSessionIdGenerator;
import com.github.xioshe.net.channels.core.transfer.DataAssembler;
import com.github.xioshe.net.channels.core.transfer.DataSplitter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Random;

@Slf4j
@SpringBootTest
public class PerformanceTest {

    @Autowired
    private LockTemplate lockTemplate;
    @Autowired
    private TransferDataCache<DataAssembler.ByteBufferDataBuffer> inboundCache;
    @Autowired
    private TransferDataCache<List<String>> outboundCache;

    private final QRCodeProtocol protocol = new QRCodeProtocol();

    private DataSplitter splitter;
    private DataAssembler assembler;

    @BeforeEach
    public void setup() {
        // 初始化必要组件
        SessionManager sessionManager = new SessionManager(
                new InMemorySessionStorage(Duration.ofMinutes(30)),
                100
        );
        DataCompressor compressor = new DataCompressor();
        AESCipher cipher = new AESCipher("1234567890123456"); // 16字节密钥
        TimestampSessionIdGenerator idGenerator = new TimestampSessionIdGenerator();

        splitter = DataSplitter.builder()
                .sessionManager(sessionManager)
                .protocol(protocol)
                .compressor(compressor)
                .cipher(cipher)
                .maxQRDataSize(2953)
                .dataCache(outboundCache)
                .sessionIdGenerator(idGenerator)
                .build();

        assembler = DataAssembler.builder()
                .sessionManager(sessionManager)
                .protocol(protocol)
                .compressor(compressor)
                .cipher(cipher)
                .dataCache(inboundCache)
                .lockTemplate(lockTemplate)
                .build();
    }

    @ParameterizedTest
    @ValueSource(ints = {1024, 10_240, 102_400, 1_024_000})
        // 1KB 到 1MB
    void testPerformance(int size) {
        String testData = generateTestData(size);
        PerformanceResult result = measurePerformance(testData.getBytes(StandardCharsets.UTF_8));
        logPerformanceResult(result);
    }

    @Disabled
    @Test
    void testActualDataPerformance() throws IOException {
        ClassPathResource resource = new ClassPathResource("data.json");
        var data = resource.getInputStream().readAllBytes();

        PerformanceResult result = measurePerformance(data);
        logPerformanceResult(result);
    }

    private PerformanceResult measurePerformance(byte[] data) {
        System.nanoTime();

        // 1. 测量发送处理时间
        long splitStartTime = System.nanoTime();
        List<String> qrCodes = splitter.split(data);
        long splitTime = System.nanoTime() - splitStartTime;

        // 2. 计算有效载荷比率
        int totalPacketSize = 0;
        int actualPacketSize = 0;
        int totalDataSize = 0;

        for (String qrCode : qrCodes) {
            TransferPacket packet = protocol.qrCodeToPacket(qrCode);
            actualPacketSize += packet.toBytes().length;
            totalDataSize += packet.getData().length;
            totalPacketSize += qrCode.length();
        }

        // 3. 测量接收处理时间
        long assembleStartTime = System.nanoTime();
        TransferResult finalResult = null;

        for (String qrCode : qrCodes) {
            finalResult = assembler.assemble(qrCode);
            if (finalResult.isCompleted()) {
                break;
            }
        }

        long assembleTime = System.nanoTime() - assembleStartTime;

        // 4. 验证数据完整性
        if (!new String(data, StandardCharsets.UTF_8).equals(finalResult.getData())) {
            throw new AssertionError("Data integrity check failed!");
        }

        return new PerformanceResult(
                splitTime / 1_000_000.0,  // 转换为毫秒
                assembleTime / 1_000_000.0,
                qrCodes.size(),
                data.length,
                totalDataSize,
                actualPacketSize,
                totalPacketSize
        );
    }

    private String generateTestData(int size) {
        StringBuilder sb = new StringBuilder(size);
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            sb.append((char) (random.nextInt(26) + 'a'));
        }
        return sb.toString();
    }

    private void logPerformanceResult(PerformanceResult result) {
        log.info("""
                        
                              性能测试结果 (数据大小: {} bytes):
                              发送处理时间: {} ms
                              接收处理时间: {} ms
                              数据压缩比率: {}
                              有效载荷比率: {}
                              实际荷载比率: {}
                              二维码数量: {}
                              总传输大小: {} bytes
                              数据膨胀率: {}
                              有效数据比率: {}
                        """,
                result.originalSize,
                String.format("%.2f", result.splitTimeMs),
                String.format("%.2f", result.assembleTimeMs),
                String.format("%.2f", 1 - (double) result.totalDataSize / result.originalSize),
                String.format("%.2f", (double) result.originalSize / result.totalPacketSize),
                String.format("%.2f", (double) result.totalDataSize / result.totalPacketSize),
                result.qrCodeCount,
                result.totalPacketSize,
                String.format("%.2f", (double) result.totalPacketSize / result.actualPacketSize),
                String.format("%.2f", (double) result.totalDataSize / result.actualPacketSize)
        );
    }

    private record PerformanceResult(double splitTimeMs,
                                     double assembleTimeMs,
                                     int qrCodeCount,
                                     int originalSize, // 源数据大小
                                     int totalDataSize, // 压缩加密后的实际数据大小
                                     int actualPacketSize, // 压缩加密后的总包大小
                                     int totalPacketSize // base64 编码后的总包大小
    ) {
    }
}