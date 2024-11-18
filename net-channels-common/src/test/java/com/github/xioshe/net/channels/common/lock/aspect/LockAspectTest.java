package com.github.xioshe.net.channels.common.lock.aspect;

import com.github.xioshe.net.channels.common.lock.annotation.WithLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class LockAspectTest {
    @Autowired
    private TestService testService;

    @Test
    void testLockAspect() throws InterruptedException, ExecutionException {
        AtomicInteger counter = new AtomicInteger();
        List<Future<String>> futures = new ArrayList<>();

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {

            // 并发测试
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        return testService.testMethod("key", counter);
                    } finally {
                        latch.countDown();
                    }
                }));
            }

            latch.await(5, TimeUnit.SECONDS);
            executor.shutdown();
        }

        // 验证结果
        assertEquals(threadCount, counter.get());
        for (Future<String> future : futures) {
            assertNotNull(future.get());
        }
    }
}

@Component
class TestService {
    @WithLock(key = "'test:' + #key")
    public String testMethod(String key, AtomicInteger counter) {
        counter.incrementAndGet();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "success";
    }
}