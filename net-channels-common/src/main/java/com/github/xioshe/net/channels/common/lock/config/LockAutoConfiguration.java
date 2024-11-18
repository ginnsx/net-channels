package com.github.xioshe.net.channels.common.lock.config;

import com.github.xioshe.net.channels.common.lock.aspect.LockAspect;
import com.github.xioshe.net.channels.common.lock.core.RedisLockExecutor;
import com.github.xioshe.net.channels.common.lock.core.LocalLockExecutor;
import com.github.xioshe.net.channels.common.lock.core.LockExecutor;
import com.github.xioshe.net.channels.common.lock.template.LockTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LockProperties.class)
@ConditionalOnProperty(prefix = "app.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LockAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.lock", name = "type", havingValue = "LOCAL", matchIfMissing = true)
    public LockExecutor localLockExecutor() {
        return new LocalLockExecutor();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.lock", name = "type", havingValue = "DISTRIBUTED")
    @ConditionalOnBean(RedissonClient.class)
    public LockExecutor distributedLockExecutor(RedissonClient redissonClient) {
        return new RedisLockExecutor(redissonClient);
    }

    @Bean
    public LockTemplate lockTemplate(LockExecutor lockExecutor, LockProperties properties) {
        return new LockTemplate(lockExecutor, properties);
    }

    @Bean
    public LockAspect lockAspect(LockTemplate lockTemplate) {
        return new LockAspect(lockTemplate);
    }
}