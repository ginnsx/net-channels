package com.github.xioshe.net.channels.common.lock.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLockExecutorTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    private RedisLockExecutor lockExecutor;

    @BeforeEach
    void setUp() {
        lockExecutor = new RedisLockExecutor(redissonClient, "test-lock:");
    }

    @Test
    void shouldAcquireLockSuccessfully() throws InterruptedException {
        // given
        LockInfo lockInfo = createLockInfo("test-key", false);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // when
        boolean result = lockExecutor.tryLock(lockInfo);

        // then
        assertTrue(result);
        verify(redissonClient).getLock("test-lock:test-key");
        verify(rLock).tryLock(
                eq(lockInfo.getWaitTime()),
                eq(lockInfo.getLeaseTime()),
                eq(lockInfo.getTimeUnit())
        );
    }

    @Test
    void shouldAcquireFairLockSuccessfully() throws InterruptedException {
        // given
        LockInfo lockInfo = createLockInfo("test-key", true);
        when(redissonClient.getFairLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // when
        boolean result = lockExecutor.tryLock(lockInfo);

        // then
        assertTrue(result);
        verify(redissonClient).getFairLock("test-lock:test-key");
        verify(rLock).tryLock(
                eq(lockInfo.getWaitTime()),
                eq(lockInfo.getLeaseTime()),
                eq(lockInfo.getTimeUnit())
        );
    }

    @Test
    void shouldReturnFalseWhenLockAcquisitionFails() throws InterruptedException {
        // given
        LockInfo lockInfo = createLockInfo("test-key", false);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        // when
        boolean result = lockExecutor.tryLock(lockInfo);

        // then
        assertFalse(result);
    }

    @Test
    void shouldHandleInterruptedException() throws InterruptedException {
        // given
        LockInfo lockInfo = createLockInfo("test-key", false);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException());

        // when
        boolean result = lockExecutor.tryLock(lockInfo);

        // then
        assertFalse(result);
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void shouldUnlockSuccessfully() {
        // given
        LockInfo lockInfo = createLockInfo("test-key", false);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // when
        lockExecutor.unlock(lockInfo);

        // then
        verify(rLock).unlock();
    }

    @Test
    void shouldNotUnlockWhenNotHeldByCurrentThread() {
        // given
        LockInfo lockInfo = createLockInfo("test-key", false);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(false);

        // when
        lockExecutor.unlock(lockInfo);

        // then
        verify(rLock, never()).unlock();
    }

    private LockInfo createLockInfo(String key, boolean fairLock) {
        return LockInfo.builder()
                .key(key)
                .waitTime(1000)
                .leaseTime(5000)
                .timeUnit(TimeUnit.MILLISECONDS)
                .fairLock(fairLock)
                .build();
    }
}