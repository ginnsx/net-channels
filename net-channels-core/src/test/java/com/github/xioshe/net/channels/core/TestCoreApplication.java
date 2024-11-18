package com.github.xioshe.net.channels.core;

import com.github.xioshe.net.channels.core.transfer.TransferDataCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TestCoreApplication {

    @Bean
    public TransferDataCache<String> transferDataCache() {
        return new TransferDataCache<>();
    }

    public static void main(String[] args) {
        SpringApplication.run(TestCoreApplication.class, args);
    }
}
