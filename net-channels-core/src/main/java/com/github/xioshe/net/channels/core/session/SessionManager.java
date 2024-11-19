package com.github.xioshe.net.channels.core.session;

import com.github.xioshe.net.channels.common.lock.annotation.WithLock;
import com.github.xioshe.net.channels.core.exception.NetChannelsException;
import com.github.xioshe.net.channels.core.exception.SessionStateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class SessionManager {

    private final SessionStorage sessionStorage;
    private final int maxSessions;

    // 监控指标
    private final AtomicInteger totalSessionsCreated = new AtomicInteger(0);
    private final AtomicInteger totalSessionsCompleted = new AtomicInteger(0);
    private final AtomicInteger totalSessionsFailed = new AtomicInteger(0);

    /**
     * 创建新的传输会话
     */
    public TransferSession createSession(String sessionId, int totalChunks, int totalSize) {
        validateSessionCreation(sessionId, totalChunks, totalSize);

        TransferSession session = new TransferSession(sessionId, totalChunks, totalSize);
        if (sessionStorage.getSession(sessionId).isPresent()) {
            throw new NetChannelsException("Session already exists: " + sessionId);
        }

        sessionStorage.saveSession(session);
        totalSessionsCreated.incrementAndGet();
        log.info("Created new session: {}", sessionId);
        return session;
    }

    /**
     * 获取现有会话
     */
    public TransferSession getSession(String sessionId) {
        return sessionStorage.getSession(sessionId)
                .orElseThrow(() -> new NetChannelsException("Session not found: " + sessionId));
    }

    public TransferSession getOrCreateSession(String sessionId, int totalChunks, int totalSize) {
        return sessionStorage.getSession(sessionId)
                .orElseGet(() -> createSession(sessionId, totalChunks, totalSize));
    }

    /**
     * 更新会话状态
     */
    @WithLock(key = "'session:' + #sessionId")
    public void updateSession(String sessionId, int chunkIndex) {
        TransferSession session = getSession(sessionId);

        try {
            validateSessionState(session);
            validateChunkIndex(session, chunkIndex);

            session.markChunkReceived(chunkIndex);

            if (session.isComplete()) {
                completeSession(session);
            } else {
                session.setState(SessionState.IN_PROGRESS);
                sessionStorage.saveSession(session);
            }
        } catch (SessionStateException e) {
            // 已经是终态的异常，直接抛出
            throw e;
        } catch (Exception e) {
            markSessionFailed(session, e);
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new NetChannelsException("Failed to update session: " + sessionId, e);
        }
    }

    public void removeSession(String sessionId) {
        sessionStorage.removeSession(sessionId);
    }

    private void validateSessionState(TransferSession session) {
        SessionState state = session.getState();
        switch (state) {
            case COMPLETED:
                throw new SessionStateException(
                        String.format("Session already completed: %s",
                                session.getSessionId()));
            case FAILED:
                throw new SessionStateException(
                        String.format("Session already failed: %s",
                                session.getSessionId()));
            case EXPIRED:
                throw new SessionStateException(
                        String.format("Session expired: %s",
                                session.getSessionId()));
            default:
                // INITIALIZED 或 IN_PROGRESS 状态是有效的
                break;
        }
    }

    private void validateChunkIndex(TransferSession session, int chunkIndex) {
        if (chunkIndex < 0 || chunkIndex >= session.getTotalChunks()) {
            throw new IllegalArgumentException(
                    String.format("Invalid chunk index: %d, total chunks: %d, sessionId: %s",
                            chunkIndex, session.getTotalChunks(), session.getSessionId()));
        }
    }

    private void markSessionFailed(TransferSession session, Throwable cause) {
        if (session.getState() != SessionState.FAILED) {
            session.setState(SessionState.FAILED);
            totalSessionsFailed.incrementAndGet();
            log.error("Session failed: {}, cause: {}",
                    session.getSessionId(), cause.getMessage());

            try {
                sessionStorage.saveSession(session);
            } catch (Exception e) {
                log.error("Failed to save failed session state: {}",
                        session.getSessionId(), e);
            }
        }
    }

    private void completeSession(TransferSession session) {
        session.setState(SessionState.COMPLETED);
        totalSessionsCompleted.incrementAndGet();
        log.info("Session completed: {}", session.getSessionId());

        try {
            sessionStorage.removeSession(session.getSessionId());
        } catch (Exception e) {
            log.error("Failed to remove completed session: {}",
                    session.getSessionId(), e);
        }
    }

    /**
     * 关闭会话管理器
     */
    public void shutdown() {
        sessionStorage.shutdown();
    }

    /**
     * 获取会话统计信息
     */
    public SessionStats getStats() {
        return SessionStats.builder()
                .activeSessions(sessionStorage.getActiveSessionCount())
                .totalCreated(totalSessionsCreated.get())
                .totalCompleted(totalSessionsCompleted.get())
                .totalFailed(totalSessionsFailed.get())
                .build();
    }

    private void validateSessionCreation(String sessionId, int totalChunks, int totalSize) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        if (totalChunks <= 0) {
            throw new IllegalArgumentException("Total chunks must be positive");
        }
        if (totalSize <= 0) {
            throw new IllegalArgumentException("Total size must be positive");
        }
        if (sessionStorage.getActiveSessionCount() >= maxSessions) {
            throw new NetChannelsException("Maximum session limit reached");
        }
    }

    public void markSessionFailed(String sessionId, Throwable e) {
        TransferSession session = getSession(sessionId);
        markSessionFailed(session, e);
    }
}