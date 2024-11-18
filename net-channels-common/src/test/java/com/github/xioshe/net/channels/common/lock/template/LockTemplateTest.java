package com.github.xioshe.net.channels.common.lock.template;

import com.github.xioshe.net.channels.common.lock.config.LockProperties;
import com.github.xioshe.net.channels.common.lock.core.LockExecutor;
import com.github.xioshe.net.channels.common.lock.core.LockInfo;
import com.github.xioshe.net.channels.common.lock.exception.LockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LockTemplateTest {
    @Mock
    private LockExecutor lockExecutor;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private LockProperties properties;

    private LockTemplate lockTemplate;

    @BeforeEach
    void setUp() {
        when(properties.getWaitTime()).thenReturn(1000L);
        when(properties.getLeaseTime()).thenReturn(5000L);
        when(properties.isFairLock()).thenReturn(false);

        lockTemplate = new LockTemplate(lockExecutor, properties);
    }

    @Test
    void shouldExecuteSuccessfully() {
        // given
        String key = "test-key";
        when(lockExecutor.tryLock(any(LockInfo.class))).thenReturn(true);

        // when
        String result = lockTemplate.execute(key, () -> "success");

        // then
        assertEquals("success", result);
        verify(lockExecutor).tryLock(any(LockInfo.class));
        verify(lockExecutor).unlock(any(LockInfo.class));
    }

    @Test
    void shouldThrowExceptionWhenLockFails() {
        // given
        String key = "test-key";
        when(lockExecutor.tryLock(any(LockInfo.class))).thenReturn(false);

        // when & then
        assertThrows(LockException.class, () ->
                lockTemplate.execute(key, () -> "success")
        );
    }

    @Test
    void shouldUnlockWhenExecutionThrowsException() {
        // given
        String key = "test-key";
        when(lockExecutor.tryLock(any(LockInfo.class))).thenReturn(true);

        // when & then
        assertThrows(RuntimeException.class, () ->
                lockTemplate.execute(key, () -> {
                    throw new RuntimeException("test exception");
                })
        );

        verify(lockExecutor).unlock(any(LockInfo.class));
    }

    @Test
    void shouldExecuteWithCustomLockInfo() {
        // given
        LockInfo lockInfo = LockInfo.builder()
                .key("custom-key")
                .waitTime(2000)
                .leaseTime(10000)
                .timeUnit(TimeUnit.MILLISECONDS)
                .fairLock(true)
                .build();

        when(lockExecutor.tryLock(any(LockInfo.class))).thenReturn(true);

        // when
        lockTemplate.execute(lockInfo, () -> null);

        // then
        verify(lockExecutor).tryLock(argThat(info ->
                info.getKey().equals("custom-key") &&
                info.getWaitTime() == 2000 &&
                info.getLeaseTime() == 10000 &&
                info.isFairLock()
        ));
    }
}