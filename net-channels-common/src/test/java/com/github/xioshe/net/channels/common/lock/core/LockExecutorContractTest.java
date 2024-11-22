package com.github.xioshe.net.channels.common.lock.core;

import com.github.xioshe.net.channels.common.lock.exception.LockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("This is a contract test base class")
public abstract class LockExecutorContractTest {

    // 由具体实现类提供 LockExecutor 实例
    protected abstract LockExecutor createLockExecutor();

    // 由具体实现类提供 LockInfo 实例，适应不同的 waitTime and leaseTime
    protected abstract LockInfo createLockInfo(String key);

    protected LockExecutor lockExecutor;

    @BeforeEach
    void setUp() {
        lockExecutor = createLockExecutor();
    }

    @Test
    void shouldAcquireLockSuccessfully() {
        // given
        LockInfo lockInfo = createLockInfo("test-lock");

        // when
        boolean acquired = lockExecutor.tryLock(lockInfo);

        // then
        assertTrue(acquired, "Should acquire lock when it's available");

        // Cleanup
        lockExecutor.unlock(lockInfo);
    }

    @Test
    void shouldSupportReentrantLock() {
        // given
        LockInfo lockInfo = createLockInfo("test-lock");

        // when
        boolean firstAcquire = lockExecutor.tryLock(lockInfo);
        boolean secondAcquire = lockExecutor.tryLock(lockInfo);

        // then
        assertTrue(firstAcquire, "First acquire should succeed");
        assertTrue(secondAcquire, "Second acquire should succeed");

        // Cleanup
        lockExecutor.unlock(lockInfo);
        lockExecutor.unlock(lockInfo);
    }

    @Test
    void shouldNotAcquireSameLockTwiceConcurrently() throws InterruptedException {
        // given
        LockInfo lockInfo = createLockInfo("test-lock");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean secondAcquire = new AtomicBoolean(false);


        // when
        boolean firstAcquire = lockExecutor.tryLock(lockInfo);
        new Thread(() -> {
            secondAcquire.set(lockExecutor.tryLock(lockInfo));
            latch.countDown();
        }).start();

        // then
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(firstAcquire, "First acquire should succeed");
        assertFalse(secondAcquire.get(), "Second acquire should failed");

        // Cleanup
        lockExecutor.unlock(lockInfo);
    }

    @Test
    void shouldRespectWaitTime() throws InterruptedException {
        // given
        LockInfo lockInfo = LockInfo.builder()
                .key("test-lock")
                .waitTime(500)
                .leaseTime(1000)
                .timeUnit(TimeUnit.MILLISECONDS)
                .fair(true)
                .build();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean acquired = new AtomicBoolean(false);
        AtomicLong waitTime = new AtomicLong();

        // First thread acquires the lock
        assertTrue(lockExecutor.tryLock(lockInfo));

        // when
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            acquired.set(lockExecutor.tryLock(lockInfo));
            waitTime.set(System.currentTimeMillis() - startTime);
            latch.countDown();
        }).start();

        // then
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertFalse(acquired.get(), "Should not acquire lock within wait time");
        assertTrue(waitTime.get() >= 500, "Should wait for specified time");

        // Cleanup
        lockExecutor.unlock(lockInfo);
    }

    @Test
    void shouldReleaseLocksCorrectly() throws InterruptedException {
        // given
        LockInfo lockInfo = createLockInfo("test-lock");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean secondAcquire = new AtomicBoolean(false);


        // when
        boolean firstAcquire = lockExecutor.tryLock(lockInfo);
        lockExecutor.unlock(lockInfo);
        new Thread(() -> {
            secondAcquire.set(lockExecutor.tryLock(lockInfo));
            latch.countDown();
            lockExecutor.unlock(lockInfo);
        }).start();

        // then
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(firstAcquire, "First acquire should succeed");
        assertTrue(secondAcquire.get(), "Should acquire lock after release");

        // Cleanup
        lockExecutor.unlock(lockInfo);
    }

    @Test
    void shouldHandleConcurrentAccess() throws InterruptedException {
        // given
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        LockInfo lockInfo = createLockInfo("test-lock");

        // when
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // 等待所有线程就绪
                    if (lockExecutor.tryLock(lockInfo)) {
                        successCount.incrementAndGet();
                        Thread.sleep(200);
                        lockExecutor.unlock(lockInfo);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown(); // 启动所有线程
        completionLatch.await(); // 等待所有线程完成

        // then
        assertEquals(1, successCount.get(), "Only one thread should acquire the lock");
    }

    @Test
    void shouldHandleNullLockInfo() {
        assertThrows(LockException.class, () -> lockExecutor.tryLock(null),
                "Should throw LockException for null LockInfo");
        assertThrows(LockException.class, () -> lockExecutor.unlock(null),
                "Should throw LockException for null LockInfo");
    }

    @Test
    void shouldHandleEmptyKey() {
        // given
        LockInfo lockInfo = createLockInfo("");

        // when & then
        assertThrows(LockException.class, () -> lockExecutor.tryLock(lockInfo),
                "Should throw LockException for empty key");
    }

    @Test
    void shouldNotAcquireDifferentFairnessLock() {
        // given
        LockInfo lockInfo = createLockInfo("test-lock");

        // when & then
        boolean firstAcquire = lockExecutor.tryLock(lockInfo, true);
        assertTrue(firstAcquire, "First acquire should succeed");
        lockInfo.setFair(false);
        boolean secondAcquire = lockExecutor.tryLock(lockInfo, true);
        assertFalse(secondAcquire, "Second acquire should failed for different fairness");

        // Cleanup
        lockExecutor.unlock(lockInfo);
    }
}