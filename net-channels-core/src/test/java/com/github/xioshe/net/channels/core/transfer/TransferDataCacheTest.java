package com.github.xioshe.net.channels.core.transfer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {TransferDataCacheTest.TestConfig.class})
class TransferDataCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("qrcodes");
        }

        @Bean
        public TransferDataCache<String> transferDataCache() {
            return new TransferDataCache<>();
        }
    }

    @Autowired
    private TransferDataCache<String> cache;

    private static final String TEST_KEY = "test-session-1";
    private static final String TEST_DATA = "test-data-content";

    @BeforeEach
    void setUp() {
        cache.cleanup();
    }

    @Test
    void shouldStoreAndRetrieveData() {
        // when
        var retrieved = cache.get(TEST_KEY, () -> TEST_DATA);

        // then
        assertThat(retrieved)
                .contains(TEST_DATA);
    }

    @Test
    void shouldReturnEmptyWhenKeyNotFound() {
        // when
        Optional<String> retrieved = cache.get("non-existent-key");

        // then
        assertThat(retrieved).isEmpty();
    }

    @Test
    void shouldRemoveData() {
        // given
        cache.store(TEST_KEY, TEST_DATA);

        // when
        cache.remove(TEST_KEY);
        Optional<String> retrieved = cache.get(TEST_KEY);

        // then
        assertThat(retrieved).isEmpty();
    }

    @Test
    void shouldCleanupAllData() {
        // given
        cache.store(TEST_KEY, TEST_DATA);
        cache.store("test-key-2", "other-data");

        // when
        cache.cleanup();

        // then
        assertThat(cache.get(TEST_KEY)).isEmpty();
        assertThat(cache.get("test-key-2")).isEmpty();
    }

    @Test
    void shouldUpdateExistingData() {
        // given
        cache.store(TEST_KEY, TEST_DATA);

        // when
        String newData = "updated-data";
        cache.store(TEST_KEY, newData);
        Optional<String> retrieved = cache.get(TEST_KEY);

        // then
        assertThat(retrieved)
                .isPresent()
                .contains(newData);
    }
}