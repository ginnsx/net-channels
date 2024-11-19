package com.github.xioshe.net.channels.core.session;

import com.github.xioshe.net.channels.core.exception.NetChannelsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionManagerTest {
    private SessionManager sessionManager;
    private static final Duration SESSION_TIMEOUT = Duration.ofSeconds(2);
    private static final int MAX_SESSIONS = 5;

    @BeforeEach
    void setUp() {
        SessionStorage sessionStorage = new InMemorySessionStorage(SESSION_TIMEOUT);
        sessionManager = new SessionManager(sessionStorage, MAX_SESSIONS);
    }

    @AfterEach
    void tearDown() {
        sessionManager.shutdown();
    }

    @Test
    void shouldCreateAndRetrieveSession() {
        String sessionId = "test-session";
        TransferSession session = sessionManager.createSession(sessionId, 10, 1000);

        assertNotNull(session);
        assertEquals(SessionState.INITIALIZED, session.getState());
        assertEquals(0, session.getReceivedCount());

        TransferSession retrieved = sessionManager.getSession(sessionId);
        assertEquals(session.getSessionId(), retrieved.getSessionId());

        SessionStats stats = sessionManager.getStats();
        assertEquals(1, stats.getTotalCreated());
    }

    @Test
    void shouldEnforceMaxSessions() {
        // 创建最大数量的会话
        for (int i = 0; i < MAX_SESSIONS; i++) {
            sessionManager.createSession("session-" + i, 10, 1000);
        }

        // 尝试创建超出限制的会话
        assertThrows(NetChannelsException.class, () ->
                sessionManager.createSession("extra-session", 10, 1000));
    }

    @Test
    void shouldHandleSessionCompletion() {
        String sessionId = "completion-test";
        sessionManager.createSession(sessionId, 2, 1000);

        // 更新所有分片
        sessionManager.updateSession(sessionId, 0);
        sessionManager.updateSession(sessionId, 1);

        // 验证会话完成
        assertThrows(NetChannelsException.class, () ->
                sessionManager.getSession(sessionId));

        SessionStats stats = sessionManager.getStats();
        assertEquals(1, stats.getTotalCompleted());
        assertEquals(0, stats.getTotalFailed());
    }

    @Test
    void shouldHandleSessionFailure() {
        // 使用 mock 存储来模拟失败场景
        SessionStorage mockStorage = mock(SessionStorage.class);
        when(mockStorage.getSession(anyString()))
                .thenReturn(Optional.of(new TransferSession("test", 2, 1000)));
        doThrow(new NetChannelsException("Simulated failure"))
                .when(mockStorage).saveSession(any());

        SessionManager manager = new SessionManager(mockStorage, MAX_SESSIONS);

        String sessionId = "failure-test";
        assertThrows(NetChannelsException.class, () ->
                manager.updateSession(sessionId, 0));

        SessionStats stats = manager.getStats();
        assertEquals(0, stats.getTotalCompleted());
        assertEquals(1, stats.getTotalFailed());
    }

    @Test
    @Timeout(value = 5)
    void shouldHandleSessionExpiration() throws InterruptedException {
        String sessionId = "expiration-test";
        sessionManager.createSession(sessionId, 10, 1000);

        // 等待会话过期
        Thread.sleep(SESSION_TIMEOUT.toMillis() + 1000);

        assertThrows(NetChannelsException.class, () ->
                sessionManager.getSession(sessionId));
    }

    @Test
    void shouldPreventInvalidStateTransitions() {
        String sessionId = "state-test";
        sessionManager.createSession(sessionId, 2, 1000);

        // 完成会话
        sessionManager.updateSession(sessionId, 0);
        sessionManager.updateSession(sessionId, 1);

        // 尝试更新已完成的会话
        assertThrows(NetChannelsException.class, () ->
                sessionManager.updateSession(sessionId, 0));
    }

    @Test
    void shouldHandleConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        AtomicInteger successCount;
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            CountDownLatch latch = new CountDownLatch(threadCount);
            successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        String sessionId = "concurrent-" + index;
                        sessionManager.createSession(
                                sessionId, 1, 1000);
                        sessionManager.updateSession(sessionId, 0);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 预期部分请求可能因为最大会话限制而失败
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            executor.shutdown();
        }

        SessionStats stats = sessionManager.getStats();
        assertEquals(successCount.get(), stats.getTotalCompleted());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void shouldValidateSessionId(String invalidSessionId) {
        assertThrows(IllegalArgumentException.class, () ->
                sessionManager.createSession(invalidSessionId, 10, 1000));
    }

    @Test
    void shouldValidateSessionParameters() {
        String sessionId = "validation-test";

        // 测试无效的分片数
        assertThrows(IllegalArgumentException.class, () ->
                sessionManager.createSession(sessionId, 0, 1000));
        assertThrows(IllegalArgumentException.class, () ->
                sessionManager.createSession(sessionId, -1, 1000));

        // 测试无效的总大小
        assertThrows(IllegalArgumentException.class, () ->
                sessionManager.createSession(sessionId, 10, 0));
        assertThrows(IllegalArgumentException.class, () ->
                sessionManager.createSession(sessionId, 10, -1));
    }

    @Test
    void shouldTrackSessionProgress() {
        String sessionId = "progress-test";
        TransferSession session = sessionManager.createSession(sessionId, 4, 1000);

        assertEquals(0.0, session.getProgress(), 0.01);

        sessionManager.updateSession(sessionId, 0);
        session = sessionManager.getSession(sessionId);
        assertEquals(0.25, session.getProgress(), 0.01);

        sessionManager.updateSession(sessionId, 1);
        session = sessionManager.getSession(sessionId);
        assertEquals(0.5, session.getProgress(), 0.01);
    }

    @Test
    void shouldMaintainSessionStats() {
        // 创建多个会话并进行不同操作
        sessionManager.createSession("stats-1", 1, 1000);
        sessionManager.createSession("stats-2", 1, 1000);
        sessionManager.createSession("stats-3", 1, 1000);

        // 完成一个会话
        sessionManager.updateSession("stats-1", 0);

        // 模拟一个失败的会话
        try {
            sessionManager.updateSession("stats-2", 999); // 无效的分片号
        } catch (Exception ignored) {
        }

        SessionStats stats = sessionManager.getStats();
        assertEquals(3, stats.getTotalCreated());
        assertEquals(1, stats.getTotalCompleted());
        assertTrue(stats.getTotalFailed() > 0);
    }
}