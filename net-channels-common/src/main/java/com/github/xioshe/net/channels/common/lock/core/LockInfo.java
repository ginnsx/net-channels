package com.github.xioshe.net.channels.common.lock.core;

import lombok.Builder;
import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
@Builder
public class LockInfo {
    private String key;
    private long waitTime;
    private long leaseTime;
    private TimeUnit timeUnit;
    private boolean fairLock;

    public void handleKey(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return;
        }
        if (!key.startsWith(prefix)) {
            this.key = prefix + key;
        }
    }
}