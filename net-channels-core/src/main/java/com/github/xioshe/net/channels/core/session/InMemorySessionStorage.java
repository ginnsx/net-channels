package com.github.xioshe.net.channels.core.session;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class InMemorySessionStorage implements SessionStorage {
    private final Map<String, TransferSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Instant> expirationTimes = new ConcurrentHashMap<>();
    private final Duration sessionTimeout;
    private final ScheduledExecutorService cleanupExecutor;
    private final AtomicInteger activeSessionCount = new AtomicInteger(0);

    public InMemorySessionStorage(Duration sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "session-cleanup");
            thread.setDaemon(true);
            return thread;
        });

        // 启动定期清理任务
        startCleanupTask();
    }

    @Override
    public void saveSession(TransferSession session) {
        var sessionId = session.getSessionId();
        boolean isNew = !sessions.containsKey(sessionId);

        sessions.put(sessionId, session);
        expirationTimes.put(sessionId, Instant.now().plus(sessionTimeout));

        if (isNew) {
            activeSessionCount.incrementAndGet();
        }

        log.debug("Saved session: {}, total active: {}",
                sessionId, activeSessionCount.get());
    }

    @Override
    public Optional<TransferSession> getSession(String sessionId) {
        TransferSession session = sessions.get(sessionId);
        if (session == null) {
            return Optional.empty();
        }

        if (isSessionExpired(sessionId)) {
            removeSession(sessionId);
            return Optional.empty();
        }

        // 刷新过期时间
        expirationTimes.put(sessionId, Instant.now().plus(sessionTimeout));
        return Optional.of(session);
    }

    @Override
    public void removeSession(String sessionId) {
        TransferSession session = sessions.remove(sessionId);
        expirationTimes.remove(sessionId);

        if (session != null) {
            activeSessionCount.decrementAndGet();
            log.debug("Removed session: {}, total active: {}",
                    sessionId, activeSessionCount.get());
        }
    }

    @Override
    public List<String> getAllSessionIds() {
        return new ArrayList<>(sessions.keySet());
    }

    @Override
    public int getActiveSessionCount() {
        return activeSessionCount.get();
    }

    @Override
    public void shutdown() {
        try {
            cleanupExecutor.shutdown();
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cleanupExecutor.shutdownNow();
        }

        sessions.clear();
        expirationTimes.clear();
        activeSessionCount.set(0);
    }

    private boolean isSessionExpired(String sessionId) {
        Instant expirationTime = expirationTimes.get(sessionId);
        return expirationTime != null && expirationTime.isBefore(Instant.now());
    }

    private void startCleanupTask() {
        long cleanupInterval = sessionTimeout.toMillis() / 2;
        cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredSessions,
                cleanupInterval,
                cleanupInterval,
                TimeUnit.MILLISECONDS
        );
    }

    private void cleanupExpiredSessions() {
        try {
            expirationTimes.forEach((sessionId, expirationTime) -> {
                if (isSessionExpired(sessionId)) {
                    removeSession(sessionId);
                }
            });
        } catch (Exception e) {
            log.error("Error during session cleanup", e);
        }
    }
}
