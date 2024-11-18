package com.github.xioshe.net.channels.common.lock.exception;

import lombok.Getter;

@Getter
public class LockException extends RuntimeException {
    private final String lockKey;
    private final String operation;

    public LockException(String message) {
        super(message);
        this.lockKey = null;
        this.operation = null;
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
        this.lockKey = null;
        this.operation = null;
    }

    public LockException(String message, String lockKey, String operation) {
        super(message);
        this.lockKey = lockKey;
        this.operation = operation;
    }

    public LockException(String message, String lockKey, String operation, Throwable cause) {
        super(message, cause);
        this.lockKey = lockKey;
        this.operation = operation;
    }
}
