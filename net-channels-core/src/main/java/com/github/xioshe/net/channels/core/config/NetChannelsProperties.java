package com.github.xioshe.net.channels.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "net.channels")
public class NetChannelsProperties {
    /**
     * AES 加密密钥
     */
    private String encryptionKey = "1234567890abvdef";

    /**
     * QR 码最大数据容量
     */
    private int maxQrDataSize = 1024;

    /**
     * 最大会话数量
     */
    private int maxSessionSize = 100;

    /**
     * 最大会话超时时间
     */
    private int maxSessionTimeoutSeconds = 60;

}