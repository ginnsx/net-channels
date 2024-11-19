package com.github.xioshe.net.channels.core.session;

import java.security.SecureRandom;

public class TimestampSessionIdGenerator {
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        // 使用当前时间戳后6位
        long timestamp = System.currentTimeMillis() % 1000000;
        // 3位随机数
        int rand = random.nextInt(1000);
        return String.format("%06d%03d", timestamp, rand);
    }
}