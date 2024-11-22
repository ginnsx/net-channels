package com.github.xioshe.net.channels.common.lock.core;

import lombok.Builder;
import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
@Builder
public class LockInfo {
    private String key;
    private long waitTime;
    /**
     * 锁释放时间，对本地锁无效
     */
    private long leaseTime;
    private TimeUnit timeUnit;
    private boolean fairLock;
}