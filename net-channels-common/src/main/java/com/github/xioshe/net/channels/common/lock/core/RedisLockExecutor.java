package com.github.xioshe.net.channels.common.lock.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

 /**
 * Redisson 分布式锁执行器
 */
@Slf4j
@RequiredArgsConstructor
public class RedisLockExecutor implements LockExecutor {

    private final RedissonClient redissonClient;

    @Override
    public boolean tryLock(LockInfo lockInfo) {
        RLock lock = getLock(lockInfo);
        try {
            return lock.tryLock(
                    lockInfo.getWaitTime(),
                    lockInfo.getLeaseTime(),
                    lockInfo.getTimeUnit()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(LockInfo lockInfo) {
        RLock lock = getLock(lockInfo);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    private RLock getLock(LockInfo lockInfo) {
        return lockInfo.isFairLock()
                ? redissonClient.getFairLock(lockInfo.getKey())
                : redissonClient.getLock(lockInfo.getKey());
    }
}