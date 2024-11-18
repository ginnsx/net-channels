package com.github.xioshe.net.channels.common.lock.core;

/**
 * 锁执行器接口
 * 不同的锁实现只需要实现这个接口即可
 */
public interface LockExecutor {
    /**
     * 尝试获取锁
     */
    boolean tryLock(LockInfo lockInfo);

    /**
     * 释放锁
     */
    void unlock(LockInfo lockInfo);
}