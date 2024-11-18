package com.github.xioshe.net.channels.core.transfer;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class TransferDataCacheRedisIT {

    @Resource
    private TransferDataCache<String> cache;

    @Resource
    private CacheManager cacheManager;

    private static final String TEST_KEY = "test-session-1";
    private static final String TEST_DATA = "test-data-content";

    @BeforeEach
    void setUp() {
        cache.cleanup();
    }

    @Test
    void shouldWorkWithRedis() {
        // when
        var retrieved = cache.get(TEST_KEY, () -> TEST_DATA);

        // then
        assertThat(retrieved)
                .contains(TEST_DATA);

        // verify it's actually in Redis
        assertThat(cacheManager.getCache("qrcodes"))
                .isNotNull()
                .satisfies(c -> assertThat(c.get(TEST_KEY)).isNotNull());
    }

    @Test
    void shouldHandleExpiration() throws InterruptedException {
        // given
        cache.store(TEST_KEY, TEST_DATA);

        // when
        Thread.sleep(1000); // wait for potential expiration
        Optional<String> retrieved = cache.get(TEST_KEY);

        // then
        assertThat(retrieved)
                .isPresent()
                .contains(TEST_DATA);
    }
}