package com.github.xioshe.net.channels.common.lock.core;

import org.junit.jupiter.api.AfterEach;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

@SpringBootTest
class RedisLockExecutorIT extends LockExecutorContractTest {

    @Autowired
    private RedissonClient redissonClient;

    @Override
    protected LockExecutor createLockExecutor() {
        return new RedisLockExecutor(redissonClient, "test-lock:");
    }

    @AfterEach
    void clean() {
        lockExecutor.shutdown();
        redissonClient.getKeys().deleteByPattern("test-lock:*");
    }

    @Override
    protected LockInfo createLockInfo(String key) {
        return LockInfo.builder()
                .key(key)
                .waitTime(200)
                .leaseTime(0) // 自动续期
                .timeUnit(TimeUnit.MILLISECONDS)
                .fairLock(true)
                .build();
    }
}
