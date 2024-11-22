package com.github.xioshe.net.channels.common.lock.core;

import com.github.xioshe.net.channels.common.lock.exception.LockException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

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
    private final ScheduledExecutorService scheduler;

    public LocalLockExecutor() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "lock-cleanup-thread");
            thread.setDaemon(true);
            return thread;
        });
        // 启动清理任务，防止内存泄露
        startCleanupTask();
    }

    @Override
    public boolean tryLock(LockInfo lockInfo, boolean strictFair) {
        if (lockInfo == null || !StringUtils.hasText(lockInfo.getKey())) {
            throw new LockException("LockInfo cannot be null or empty");
        }
        LockHolder holder = lockMap.compute(lockInfo.getKey(), (key, existingHolder) -> {
            if (existingHolder != null) {
                existingHolder.updateLastAccessTime();
                return existingHolder;
            }
            return new LockHolder(new ReentrantLock(lockInfo.isFairLock()), lockInfo.isFairLock());
        });
        // 检查公平性设置是否一致，避免同一 key 申请了不同的公平性
        if (strictFair && holder.isFair() != lockInfo.isFairLock()) {
            log.warn("Lock fairness setting mismatch for key: {}. Existing: {}, Requested: {}",
                    lockInfo.getKey(), holder.isFair(), lockInfo.isFairLock());
            return false;
        }

        try {
            return holder.getLock().tryLock(lockInfo.getWaitTime(), lockInfo.getTimeUnit());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(LockInfo lockInfo) {
        if (lockInfo == null || !StringUtils.hasText(lockInfo.getKey())) {
            throw new LockException("LockInfo cannot be null or empty");
        }
        lockMap.computeIfPresent(lockInfo.getKey(), (key, holder) -> {
            if (holder.getLock().isHeldByCurrentThread()) {
                holder.getLock().unlock();
                // 复用锁对象，只有在没有等待线程且超过空闲时间时才移除
                if (!holder.getLock().hasQueuedThreads() && holder.isExpired()) {
                    return null; // 返回null会移除该entry
                }
            }
            return holder;
        });
    }

    @Override
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                // 等待清理任务完成，但最多等待 5 秒
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
        }
        lockMap.clear();
    }

    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanup();
            } catch (Exception e) {
                log.error("Error during lock cleanup", e);
            }
        }, CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS); // 1 min
    }

    private void cleanup() {
        lockMap.entrySet().removeIf(entry -> {
            LockHolder holder = entry.getValue();
            return !holder.getLock().isLocked() &&
                   !holder.getLock().hasQueuedThreads() &&
                   holder.isExpired();
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

        public boolean isExpired() {
            return System.currentTimeMillis() - lastAccessTime > MAX_IDLE_TIME; // 10 min
        }
    }
}