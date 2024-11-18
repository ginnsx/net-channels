package com.github.xioshe.net.channels.core.session;

public enum SessionState {
    INITIALIZED,    // 会话初始化
    IN_PROGRESS,    // 传输中
    COMPLETED,      // 传输完成
    FAILED,         // 传输失败
    EXPIRED         // 会话过期
}