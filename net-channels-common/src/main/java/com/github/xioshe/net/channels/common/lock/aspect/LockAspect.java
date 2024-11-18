package com.github.xioshe.net.channels.common.lock.aspect;

import com.github.xioshe.net.channels.common.lock.annotation.WithLock;
import com.github.xioshe.net.channels.common.lock.core.LockInfo;
import com.github.xioshe.net.channels.common.lock.exception.LockException;
import com.github.xioshe.net.channels.common.lock.template.LockTemplate;
import com.github.xioshe.net.channels.common.lock.util.SpELUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
@Slf4j
@RequiredArgsConstructor
public class LockAspect {
    private final LockTemplate lockTemplate;

    @Around("@annotation(withLock)")
    public Object around(ProceedingJoinPoint point, WithLock withLock) {
        LockInfo lockInfo = parseLockInfo(point, withLock);

        return lockTemplate.execute(lockInfo, () -> {
            try {
                return point.proceed();
            } catch (Throwable throwable) {
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                }
                throw new LockException("Error occurred while executing locked method", throwable);
            }
        });
    }

    private LockInfo parseLockInfo(ProceedingJoinPoint point, WithLock withLock) {
        try {
            // 解析SpEL表达式获取锁的key
            String key = SpELUtil.parseExpression(withLock.key(), point);

            return LockInfo.builder()
                    .key(key)
                    .waitTime(getWaitTime(withLock))
                    .leaseTime(getLeaseTime(withLock))
                    .timeUnit(withLock.timeUnit())
                    .fairLock(withLock.fairLock())
                    .build();
        } catch (Exception e) {
            throw new LockException("Failed to parse lock info", e);
        }
    }

    private long getWaitTime(WithLock withLock) {
        return withLock.waitTime() < 0 ? lockTemplate.getDefaultWaitTime() : withLock.waitTime();
    }

    private long getLeaseTime(WithLock withLock) {
        return withLock.leaseTime() < 0 ? lockTemplate.getDefaultLeaseTime() : withLock.leaseTime();
    }
}