package com.github.xioshe.net.channels.core.exception;

/**
 * 表示会话状态无效的异常
 * 用于标识会话已经处于终态（完成、失败或过期）
 */
public class SessionStateException extends NetChannelsException {
    
    public SessionStateException(String message) {
        super(message);
    }

    public SessionStateException(String message, Throwable cause) {
        super(message, cause);
    }
}