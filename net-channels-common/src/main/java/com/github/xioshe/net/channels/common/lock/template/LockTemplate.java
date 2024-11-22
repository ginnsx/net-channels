package com.github.xioshe.net.channels.common.lock.template;

import com.github.xioshe.net.channels.common.lock.config.LockProperties;
import com.github.xioshe.net.channels.common.lock.core.LockExecutor;
import com.github.xioshe.net.channels.common.lock.core.LockInfo;
import com.github.xioshe.net.channels.common.lock.exception.LockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class LockTemplate {

    private final LockExecutor lockExecutor;
    private final LockProperties properties;

    /**
     * 执行需要加锁的操作（无返回值）
     */
    public void execute(String key, Runnable runnable) {
        execute(key, () -> {
            runnable.run();
            return null;
        });
    }

    public void execute(LockInfo lockInfo, Runnable runnable) {
        execute(lockInfo, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 执行需要加锁的操作（有返回值）
     */
    public <T> T execute(String key, Supplier<T> supplier) {
        return execute(createLockInfo(key), supplier);
    }

    /**
     * 使用自定义的锁信息执行操作
     */
    public <T> T execute(LockInfo lockInfo, Supplier<T> supplier) {
        if (!lockExecutor.tryLock(lockInfo)) {
            throw new LockException("Failed to acquire lock: " + lockInfo.getKey());
        }
        try {
            return supplier.get();
        } finally {
            lockExecutor.unlock(lockInfo);
        }
    }

    private LockInfo createLockInfo(String key) {
        return LockInfo.builder()
                .key(key)
                .waitTime(properties.getWaitTime())
                .leaseTime(properties.getLeaseTime())
                .timeUnit(TimeUnit.MILLISECONDS)
                .fair(properties.isDefaultFair())
                .build();
    }


    public long getDefaultWaitTime() {
        return properties.getWaitTime();
    }

    public long getDefaultLeaseTime() {
        return properties.getLeaseTime();
    }
}