package com.github.xioshe.net.channels.core.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RedisSessionStorageTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private RedisSessionStorage sessionStorage;

    @BeforeEach
    void setUp() {
        sessionStorage = new RedisSessionStorage(
                redisTemplate,
                objectMapper,
                Duration.ofMinutes(5)
        );

        // 清理测试数据
        redisTemplate.delete(redisTemplate.keys("net-channels:*"));
    }

    @Test
    void shouldSaveAndRetrieveSession() {
        TransferSession session = new TransferSession("test-1", 10, 1000);
        sessionStorage.saveSession(session);

        Optional<TransferSession> retrieved = sessionStorage.getSession("test-1");
        assertTrue(retrieved.isPresent());
        assertEquals(session.getSessionId(), retrieved.get().getSessionId());
    }

    @Test
    void shouldRemoveSession() {
        TransferSession session = new TransferSession("test-2", 10, 1000);
        sessionStorage.saveSession(session);
        sessionStorage.removeSession("test-2");

        Optional<TransferSession> retrieved = sessionStorage.getSession("test-2");
        assertFalse(retrieved.isPresent());
    }

    @Test
    void shouldTrackSessionCount() {
        assertEquals(0, sessionStorage.getActiveSessionCount());

        TransferSession session1 = new TransferSession("test-3", 10, 1000);
        TransferSession session2 = new TransferSession("test-4", 10, 1000);

        sessionStorage.saveSession(session1);
        sessionStorage.saveSession(session2);

        assertEquals(2, sessionStorage.getActiveSessionCount());

        sessionStorage.removeSession("test-3");
        assertEquals(1, sessionStorage.getActiveSessionCount());
    }
}