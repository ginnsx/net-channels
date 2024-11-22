package com.github.xioshe.net.channels.common.lock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithLock {
    /**
     * 锁的key，支持SpEL表达式
     */
    String key();

    /**
     * 等待锁的时间，默认使用配置文件中的值。0 代表不等待
     */
    long waitTime() default -1;
    
    /**
     * 持有锁的时间，默认使用配置文件中的值。使用 Redisson 实现锁时，0 代表启用自动续期
     */
    long leaseTime() default -1;
    
    /**
     * 时间单位，默认毫秒
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
    
    /**
     * 是否使用公平锁
     */
    boolean fair() default false;
}