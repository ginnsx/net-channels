# 通用组件

## 锁框架

目录结构说明

```text
├── annotation
│   ├── EnableLocking.java     # 开启锁支持
│   └── WithLock.java          # 方法级锁注解
├── aspect
│   └── LockAspect.java        # AOP 切面，用于支持 @WithLock 注解
├── config
│   ├── LockAutoConfiguration.java    # 自动装配类
│   ├── LockingEnabledCondition.java  # 启用配置类，用于支持 @EnableLocking 注解
│   └── LockProperties.java           # 配置类
├── core
│   ├── LocalLockExecutor.java        # JVM 锁实现类
│   ├── LockExecutor.java             # 加锁操作接口
│   ├── LockInfo.java
│   └── RedisLockExecutor.java        # Redis 锁实现类，使用 Redisson 实现
├── exception
│   └── LockException.java
├── template
│   └── LockTemplate.java             # 编程式模板
└── util
    └── SpELUtil.java                 # SpEL 表达式工具类
```

## 使用说明

基于 Spring Boot 的自动装配机制，通过 `@EnableLocking` 注解开启锁支持。同时，提供了 LockProperties 配置类，用于配置锁的默认参数。

```yaml
app:
  lock:
    enabled: true # 与 @EnableLocking 注解功能一致
    type: local # 锁类型，local/redis
    wait-time: 200 # 等待锁的时间
    lease-time: 0 # 锁的持有时间，JVM 锁不支持这个参数，Redis 锁中 0 表示开启自动续期
    default-fair: true # 是否默认为公平锁，大多数时候 false 就好
    key-store-prefix: ‘lock:‘ # 锁的 key 存储前缀，针对 Redis 锁有效
```
