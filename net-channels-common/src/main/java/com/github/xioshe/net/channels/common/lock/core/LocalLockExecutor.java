package com.github.xioshe.net.channels.common.lock.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 用 JVM 锁进行加锁，启动后台清理任务，避免内存泄露
 */
@Slf4j
public class LocalLockExecutor implements LockExecutor {
    // 清理任务的执行间隔
    private static final long CLEANUP_INTERVAL = TimeUnit.MINUTES.toMillis(1);
    // 锁的最大空闲时间
    private static final long MAX_IDLE_TIME = TimeUnit.MINUTES.toMillis(10);

    private final ConcurrentHashMap<String, LockHolder> lockMap = new ConcurrentHashMap<>();

    public LocalLockExecutor() {
        // 启动清理任务，防止内存泄露
        startCleanupTask();
    }

    @Override
    public boolean tryLock(LockInfo lockInfo) {
        LockHolder holder = lockMap.compute(lockInfo.getKey(), (key, existingHolder) -> {
            if (existingHolder != null) {
                // 检查公平性设置是否一致，避免同一 key 申请了不同的公平性
                if (existingHolder.isFair() != lockInfo.isFairLock()) {
                    log.warn("Lock fairness setting mismatch for key: {}. Existing: {}, Requested: {}",
                            key, existingHolder.isFair(), lockInfo.isFairLock());
                }
                existingHolder.updateLastAccessTime();
                return existingHolder;
            }
            return new LockHolder(new ReentrantLock(lockInfo.isFairLock()), lockInfo.isFairLock());
        });
        try {
            return holder.getLock().tryLock(lockInfo.getWaitTime(), lockInfo.getTimeUnit());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(LockInfo lockInfo) {
        lockMap.computeIfPresent(lockInfo.getKey(), (key, holder) -> {
            if (holder.getLock().isHeldByCurrentThread()) {
                holder.getLock().unlock();
                // 复用锁对象，只有在没有等待线程且超过空闲时间时才移除
                if (!holder.getLock().hasQueuedThreads() &&
                    System.currentTimeMillis() - holder.getLastAccessTime() > MAX_IDLE_TIME) {
                    return null; // 返回null会移除该entry
                }
            }
            return holder;
        });
    }

    private void startCleanupTask() {
        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "lock-cleanup-thread");
            thread.setDaemon(true);
            return thread;
        })) {

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    cleanup();
                } catch (Exception e) {
                    log.error("Error during lock cleanup", e);
                }
            }, CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        lockMap.entrySet().removeIf(entry -> {
            LockHolder holder = entry.getValue();
            return !holder.getLock().isLocked() &&
                   !holder.getLock().hasQueuedThreads() &&
                   now - holder.getLastAccessTime() > MAX_IDLE_TIME;
        });
    }


    @Data
    private static class LockHolder {
        private final ReentrantLock lock;
        private final boolean fair;
        private volatile long lastAccessTime;

        public LockHolder(ReentrantLock lock, boolean fair) {
            this.lock = lock;
            this.fair = fair;
            this.lastAccessTime = System.currentTimeMillis();
        }

        public void updateLastAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
}