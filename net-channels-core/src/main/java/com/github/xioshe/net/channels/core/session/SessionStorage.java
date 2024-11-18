package com.github.xioshe.net.channels.core.session;

import com.github.xioshe.net.channels.common.lock.annotation.WithLock;

import java.util.List;
import java.util.Optional;

public interface SessionStorage {
    /**
     * 保存会话
     */
    @WithLock(key = "'session:' + #session.sessionId")
    void saveSession(TransferSession session);

    /**
     * 获取会话
     */
    @WithLock(key = "'session:' + #sessionId")
    Optional<TransferSession> getSession(String sessionId);

    /**
     * 删除会话
     */
    void removeSession(String sessionId);

    /**
     * 获取所有会话ID
     */
    List<String> getAllSessionIds();

    /**
     * 获取活跃会话数量
     */
    int getActiveSessionCount();

    /**
     * 关闭存储
     */
    void shutdown();
}