package com.github.xioshe.net.channels.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xioshe.net.channels.common.lock.template.LockTemplate;
import com.github.xioshe.net.channels.core.cache.TransferDataCache;
import com.github.xioshe.net.channels.core.compress.DataCompressor;
import com.github.xioshe.net.channels.core.crypto.AESCipher;
import com.github.xioshe.net.channels.core.protocol.QRCodeProtocol;
import com.github.xioshe.net.channels.core.session.InMemorySessionStorage;
import com.github.xioshe.net.channels.core.session.RedisSessionStorage;
import com.github.xioshe.net.channels.core.session.SessionManager;
import com.github.xioshe.net.channels.core.session.SessionStorage;
import com.github.xioshe.net.channels.core.session.TimestampSessionIdGenerator;
import com.github.xioshe.net.channels.core.transfer.DataAssembler;
import com.github.xioshe.net.channels.core.transfer.DataSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;

@Slf4j
@AutoConfiguration
@Import(CacheConfig.class)
@EnableConfigurationProperties(NetChannelsProperties.class)
public class NetChannelsAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "net.channels.session.storage.type", havingValue = "local", matchIfMissing = true)
    @ConditionalOnMissingBean
    public SessionStorage sessionStorage(NetChannelsProperties properties) {
        return new InMemorySessionStorage(Duration.ofSeconds(properties.getMaxSessionTimeoutSeconds()));
    }

    @Bean
    @ConditionalOnProperty(name = "net.channels.session.storage.type", havingValue = "redis")
    @ConditionalOnMissingBean
    public SessionStorage redisSessionStorage(StringRedisTemplate redisTemplate,
                                              ObjectMapper objectMapper,
                                              NetChannelsProperties properties) {
        return new RedisSessionStorage(redisTemplate, objectMapper, Duration.ofSeconds(properties.getMaxSessionTimeoutSeconds()));
    }


    @Bean
    @ConditionalOnMissingBean
    public SessionManager sessionManager(SessionStorage sessionStorage,
                                         NetChannelsProperties properties) {
        return new SessionManager(sessionStorage, properties.getMaxSessionSize());
    }

    @Bean
    @ConditionalOnMissingBean
    public QRCodeProtocol qrCodeProtocol() {
        return new QRCodeProtocol();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataCompressor dataCompressor() {
        return new DataCompressor();
    }

    @Bean
    @ConditionalOnMissingBean
    public AESCipher aesCipher(NetChannelsProperties properties) {
        return new AESCipher(properties.getEncryptionKey());
    }

    @Bean
    @ConditionalOnMissingBean
    public TimestampSessionIdGenerator sessionIdGenerator() {
        return new TimestampSessionIdGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSplitter dataSplitter(
            SessionManager sessionManager,
            QRCodeProtocol protocol,
            DataCompressor compressor,
            AESCipher cipher,
            TimestampSessionIdGenerator sessionIdGenerator,
            TransferDataCache<List<String>> splitterDataCache,
            NetChannelsProperties properties
    ) {
        return DataSplitter.builder()
                .sessionManager(sessionManager)
                .protocol(protocol)
                .compressor(compressor)
                .cipher(cipher)
                .sessionIdGenerator(sessionIdGenerator)
                .dataCache(splitterDataCache)
                .maxQRDataSize(properties.getMaxQrDataSize())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataAssembler dataAssembler(
            SessionManager sessionManager,
            QRCodeProtocol protocol,
            DataCompressor compressor,
            AESCipher cipher,
            TransferDataCache<DataAssembler.ByteBufferDataBuffer> assemblerDataCache,
            LockTemplate lockTemplate
    ) {
        return DataAssembler.builder()
                .sessionManager(sessionManager)
                .protocol(protocol)
                .compressor(compressor)
                .cipher(cipher)
                .dataCache(assemblerDataCache)
                .lockTemplate(lockTemplate)
                .build();
    }
}