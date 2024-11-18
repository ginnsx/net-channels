package com.github.xioshe.net.channels.core.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xioshe.net.channels.core.exception.NetChannelsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class RedisSessionStorage implements SessionStorage {
    private static final String SESSION_KEY_PREFIX = "net-channels:session:";
    private static final String SESSION_COUNT_KEY = "net-channels:session-count";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration sessionTimeout;

    @Override
    public void saveSession(TransferSession session) {
        try {
            String sessionKey = getSessionKey(session.getSessionId());
            String sessionJson = objectMapper.writeValueAsString(session);

            redisTemplate.opsForValue().set(
                    sessionKey,
                    sessionJson,
                    sessionTimeout
            );

            // 更新活跃会话计数
            redisTemplate.opsForValue().increment(SESSION_COUNT_KEY);

            log.debug("Saved session to Redis: {}", session.getSessionId());
        } catch (Exception e) {
            throw new NetChannelsException("Failed to save session to Redis", e);
        }
    }

    @Override
    public Optional<TransferSession> getSession(String sessionId) {
        try {
            String sessionKey = getSessionKey(sessionId);
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);

            if (sessionJson == null) {
                return Optional.empty();
            }

            // 刷新过期时间
            redisTemplate.expire(sessionKey, sessionTimeout);

            TransferSession session = objectMapper.readValue(
                    sessionJson, TransferSession.class);
            return Optional.of(session);

        } catch (Exception e) {
            throw new NetChannelsException("Failed to get session from Redis", e);
        }
    }

    @Override
    public void removeSession(String sessionId) {
        try {
            String sessionKey = getSessionKey(sessionId);
            redisTemplate.delete(sessionKey);
            redisTemplate.opsForValue().decrement(SESSION_COUNT_KEY);

            log.debug("Removed session from Redis: {}", sessionId);
        } catch (Exception e) {
            throw new NetChannelsException("Failed to remove session from Redis", e);
        }
    }

    @Override
    public List<String> getAllSessionIds() {
        try {
            String pattern = SESSION_KEY_PREFIX + "*";
            return Objects.requireNonNull(redisTemplate.keys(pattern)).stream()
                    .map(key -> key.substring(SESSION_KEY_PREFIX.length()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new NetChannelsException("Failed to get session IDs from Redis", e);
        }
    }

    @Override
    public int getActiveSessionCount() {
        String count = redisTemplate.opsForValue().get(SESSION_COUNT_KEY);
        return count != null ? Integer.parseInt(count) : 0;
    }

    @Override
    public void shutdown() {
        // Redis client 会由 Spring 管理关闭
    }

    private String getSessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }
}