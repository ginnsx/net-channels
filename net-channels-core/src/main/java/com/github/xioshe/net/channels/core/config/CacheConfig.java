package com.github.xioshe.net.channels.core.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {
    @Value("${net.channels.cache.type:caffeine}")
    private String cacheType;

    @Value("${net.channels.cache.timeout:1h}")
    private Duration cacheTimeout;

    @Bean
    public CacheManager cacheManager(
            @Autowired(required = false) ObjectMapper objectMapper,
            @Autowired(required = false) RedisConnectionFactory redisConnectionFactory) {
        if ("redis".equalsIgnoreCase(cacheType) && redisConnectionFactory != null) {
            return createRedisCacheManager(objectMapper, redisConnectionFactory);
        }
        return createCaffeineCacheManager();
    }

    private CacheManager createRedisCacheManager(ObjectMapper objectMapper,
                                                 RedisConnectionFactory connectionFactory) {
        ObjectMapper customObjectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .activateDefaultTyping(new ObjectMapper().getPolymorphicTypeValidator(),
                        ObjectMapper.DefaultTyping.NON_FINAL_AND_ENUMS,
                        JsonTypeInfo.As.PROPERTY);
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheTimeout)
                .computePrefixWith(cacheName -> "nc:" + cacheName + ":") // 用单冒号替换调默认双冒号
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(customObjectMapper)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    private CacheManager createCaffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterAccess(cacheTimeout));
        return cacheManager;
    }
}