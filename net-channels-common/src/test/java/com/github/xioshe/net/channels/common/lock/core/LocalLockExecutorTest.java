package com.github.xioshe.net.channels.common.lock.core;

import java.util.concurrent.TimeUnit;

class LocalLockExecutorTest extends LockExecutorContractTest {

    @Override
    protected LockExecutor createLockExecutor() {
        return new LocalLockExecutor();
    }

    @Override
    protected LockInfo createLockInfo(String key) {
        return LockInfo.builder()
                .key(key)
                .waitTime(0) // 不等待
                .leaseTime(1000)
                .timeUnit(TimeUnit.MILLISECONDS)
                .fairLock(true)
                .build();
    }
}