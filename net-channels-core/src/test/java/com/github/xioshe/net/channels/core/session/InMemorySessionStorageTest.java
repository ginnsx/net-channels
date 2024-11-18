package com.github.xioshe.net.channels.core.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemorySessionStorageTest {

    private InMemorySessionStorage sessionStorage;
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(2);

    @BeforeEach
    void setUp() {
        sessionStorage = new InMemorySessionStorage(TEST_TIMEOUT);
    }

    @Test
    void shouldSaveAndRetrieveSession() {
        TransferSession session = new TransferSession("test-1", 10, 1000);
        sessionStorage.saveSession(session);

        Optional<TransferSession> retrieved = sessionStorage.getSession("test-1");
        assertTrue(retrieved.isPresent());
        assertEquals(session.getSessionId(), retrieved.get().getSessionId());
        assertEquals(1, sessionStorage.getActiveSessionCount());
    }

    @Test
    void shouldRemoveSession() {
        TransferSession session = new TransferSession("test-2", 10, 1000);
        sessionStorage.saveSession(session);
        assertEquals(1, sessionStorage.getActiveSessionCount());

        sessionStorage.removeSession("test-2");
        Optional<TransferSession> retrieved = sessionStorage.getSession("test-2");
        assertFalse(retrieved.isPresent());
        assertEquals(0, sessionStorage.getActiveSessionCount());
    }

    @Test
    @Timeout(value = 5)
    void shouldHandleSessionExpiration() throws InterruptedException {
        sessionStorage = new InMemorySessionStorage(Duration.ofMillis(100));
        TransferSession session = new TransferSession("test-3", 10, 1000);
        sessionStorage.saveSession(session);

        // 等待会话过期
        Thread.sleep(200);

        Optional<TransferSession> retrieved = sessionStorage.getSession("test-3");
        assertFalse(retrieved.isPresent());
        assertEquals(0, sessionStorage.getActiveSessionCount());
    }

    @Test
    void shouldHandleConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    String sessionId = "concurrent-" + index;
                    TransferSession session = new TransferSession(sessionId, 10, 1000);
                    sessionStorage.saveSession(session);
                    sessionStorage.getSession(sessionId);
                    sessionStorage.removeSession(sessionId);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(0, sessionStorage.getActiveSessionCount());
    }

    @Test
    void shouldRefreshExpirationOnAccess() throws InterruptedException {
        sessionStorage = new InMemorySessionStorage(Duration.ofMillis(300));
        TransferSession session = new TransferSession("test-4", 10, 1000);
        sessionStorage.saveSession(session);

        // 等待接近过期但未过期
        Thread.sleep(200);

        // 访问会话，刷新过期时间
        Optional<TransferSession> retrieved = sessionStorage.getSession("test-4");
        assertTrue(retrieved.isPresent());

        // 再次等待接近原过期时间
        Thread.sleep(200);

        // 会话应该仍然存在
        retrieved = sessionStorage.getSession("test-4");
        assertTrue(retrieved.isPresent());
    }

    @Test
    void shouldGetAllSessionIds() {
        for (int i = 0; i < 5; i++) {
            TransferSession session = new TransferSession("test-" + i, 10, 1000);
            sessionStorage.saveSession(session);
        }

        assertEquals(5, sessionStorage.getAllSessionIds().size());
    }

    @Test
    void shouldShutdownCleanly() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            TransferSession session = new TransferSession("test-" + i, 10, 1000);
            sessionStorage.saveSession(session);
        }

        sessionStorage.shutdown();
        assertEquals(0, sessionStorage.getActiveSessionCount());
        assertTrue(sessionStorage.getAllSessionIds().isEmpty());
    }
}