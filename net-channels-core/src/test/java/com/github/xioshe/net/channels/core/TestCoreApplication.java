package com.github.xioshe.net.channels.core;

import com.github.xioshe.net.channels.core.cache.TransferDataCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TestCoreApplication {

    @Bean
    public TransferDataCache<String> transferDataCache(CacheManager cacheManager) {
        return new TransferDataCache<>("test:packets", cacheManager);
    }

    public static void main(String[] args) {
        SpringApplication.run(TestCoreApplication.class, args);
    }
}
