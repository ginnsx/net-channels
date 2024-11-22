package com.github.xioshe.net.channels.common.lock.core;

/**
 * 锁执行器接口
 * 不同的锁实现只需要实现这个接口即可
 */
public interface LockExecutor {

    default boolean tryLock(LockInfo lockInfo) {
        return tryLock(lockInfo, false);
    }

    /**
     * 尝试获取锁
     *
     * @param strictFair 为 true 时，如果同一个 key 前后公平性不一致，加锁失败
     */
    boolean tryLock(LockInfo lockInfo, boolean strictFair);

    /**
     * 释放锁
     */
    void unlock(LockInfo lockInfo);

    default void shutdown() {
    }
}