package com.github.xioshe.net.channels.common.lock.core;

import com.github.xioshe.net.channels.common.lock.exception.LockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Redisson 分布式锁执行器
 */
@Slf4j
@RequiredArgsConstructor
public class RedisLockExecutor implements LockExecutor {

    private final RedissonClient redissonClient;
    private final String lockPrefix;
    // 用于缓存每个锁的公平性设置
    // RLock 公平锁和非公平锁 key 相同，在不同线程中加锁会失败，因此确保重入时加锁失败即可
    private final ConcurrentHashMap<String, Boolean> fairnessCache = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(LockInfo lockInfo, boolean strictFair) {
        if (lockInfo == null || !StringUtils.hasText(lockInfo.getKey())) {
            throw new LockException("LockInfo cannot be null or empty");
        }

        // 检查并更新公平性设置
        if (hasFairnessMismatch(lockInfo, strictFair)) return false;

        RLock lock = getLock(lockInfo);
        try {
            boolean acquired = lock.tryLock(
                    lockInfo.getWaitTime(),
                    lockInfo.getLeaseTime(),
                    lockInfo.getTimeUnit()
            );
            if (acquired) {
                // 保证公平性缓存与锁一致，也避免 unlock() 提前删除缓存的问题
                fairnessCache.put(lockInfo.getKey(), lockInfo.isFairLock());
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean hasFairnessMismatch(LockInfo lockInfo, boolean strictFair) {
        boolean existingFairness = fairnessCache.compute(lockInfo.getKey(), (k, existing) -> {
            if (existing != null) {
                return existing;
            }
            return lockInfo.isFairLock();
        });

        // 检查公平性设置是否一致
        if (strictFair && existingFairness != lockInfo.isFairLock()) {
            log.warn("Lock fairness setting mismatch for key: {}. Existing: {}, Requested: {}",
                    lockInfo.getKey(), existingFairness, lockInfo.isFairLock());
            return true;
        }
        return false;
    }

    @Override
    public void unlock(LockInfo lockInfo) {
        if (lockInfo == null || !StringUtils.hasText(lockInfo.getKey())) {
            throw new LockException("LockInfo cannot be null or empty");
        }
        RLock lock = getLock(lockInfo);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            // 如果锁没有被任何线程持有，则清除缓存
            if (!lock.isLocked()) {
                // 无法实现 hasQueuedThreads() 判断，可能导致缓存被提前删除，在 tryLock() 进行了处理
                fairnessCache.remove(lockInfo.getKey());
            }
        }
    }

    @Override
    public void shutdown() {
        fairnessCache.clear();
    }

    private RLock getLock(LockInfo lockInfo) {
        String key = lockPrefix + lockInfo.getKey();
        return lockInfo.isFairLock()
                ? redissonClient.getFairLock(key)
                : redissonClient.getLock(key);
    }
}