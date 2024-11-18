package com.github.xioshe.net.channels.common.lock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.lock")
@Data
public class LockProperties {
    /**
     * 锁的前缀
     */
    private String keyPrefix = "nc:lock:";
    /**
     * 是否启用分布式锁
     */
    private boolean enabled = true;
    /**
     * 锁类型
     */
    private LockType type = LockType.LOCAL;
    /**
     * 锁等待时间，单位：毫秒。使用 Redisson 实现时，0 代表启用自动续期
     */
    private long waitTime = 3000;
    /**
     * 锁持有时间，单位：毫秒。使用 Redisson 实现时，0 代表不等待加锁
     */
    private long leaseTime = 30000;
    /**
     * 是否使用公平锁
     */
    private boolean fairLock = false;

    public enum LockType {
        LOCAL, DISTRIBUTED
    }
}