package com.github.xioshe.net.channels.core.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;
import java.util.concurrent.Callable;

@Getter
@Slf4j
@RequiredArgsConstructor
public class TransferDataCache<T> {
    private final String cacheName;
    private final CacheManager cacheManager;

    public Optional<T> get(String key) {
        Cache.ValueWrapper wrapper = getCache().get(key);
        return Optional.ofNullable(wrapper).map(w -> (T) w.get());
    }

    public T get(String key, Callable<T> valueLoader) {
        return getCache().get(key, valueLoader);
    }

    public T store(String key, T data) {
        getCache().put(key, data);
        return data;
    }

    public void remove(String key) {
        getCache().evict(key);
        log.debug("[{}] Removed data with key: {}", cacheName, key);
    }

    public void cleanup() {
        getCache().clear();
        log.debug("[{}] Cleaned up all cached data", cacheName);
    }

    private Cache getCache() {
        return cacheManager.getCache(cacheName);
    }
}