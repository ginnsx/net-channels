package com.github.xioshe.net.channels.common.lock.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalLockExecutorTest {
    private LocalLockExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new LocalLockExecutor();
    }

    @Test
    void shouldAcquireLockSuccessfully() {
        // given
        LockInfo lockInfo = createLockInfo("test-key");

        // when
        boolean acquired = executor.tryLock(lockInfo);

        // then
        assertTrue(acquired);
    }

    @Test
    void shouldNotAcquireLockWhenAlreadyLocked() throws InterruptedException {
        // given
        String key = "test-key";
        LockInfo lockInfo = createLockInfo(key);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean secondThreadResult = new AtomicBoolean();

        // when
        executor.tryLock(lockInfo);

        Thread secondThread = new Thread(() -> {
            secondThreadResult.set(executor.tryLock(lockInfo));
            latch.countDown();
        });
        secondThread.start();

        // then
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertFalse(secondThreadResult.get());
    }

    @Test
    void shouldReleaseLockSuccessfully() {
        // given
        String key = "test-key";
        LockInfo lockInfo = createLockInfo(key);
        executor.tryLock(lockInfo);

        // when
        executor.unlock(lockInfo);

        // then
        assertTrue(executor.tryLock(lockInfo));
    }

    @Test
    void shouldSupportReentrantLock() {
        // given
        LockInfo lockInfo = createLockInfo("test-key");

        // when & then
        assertTrue(executor.tryLock(lockInfo));
        assertTrue(executor.tryLock(lockInfo));

        executor.unlock(lockInfo);
        executor.unlock(lockInfo);
    }

    private LockInfo createLockInfo(String key) {
        return LockInfo.builder()
                .key(key)
                .waitTime(100)
                .leaseTime(1000)
                .timeUnit(TimeUnit.MILLISECONDS)
                .fairLock(false)
                .build();
    }
}