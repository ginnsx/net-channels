package com.github.xioshe.net.channels.core.transfer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class TransferDataCache<T> {
    private static final String CACHE_NAME = "packets";

    @Cacheable(value = CACHE_NAME, key = "#key")
    public Optional<T> get(String key) {
        return Optional.empty();
    }

    @Cacheable(value = CACHE_NAME, key = "#key")
    public T get(String key, Supplier<T> supplier) {
        return supplier.get();
    }

    @CachePut(value = CACHE_NAME, key = "#key")
    public T store(String key, T data) {
        return data;
    }

    @CacheEvict(value = CACHE_NAME, key = "#key")
    public void remove(String key) {
        log.debug("Removed data with key: {}", key);
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void cleanup() {
        log.debug("Cleaned up all cached data");
    }
}