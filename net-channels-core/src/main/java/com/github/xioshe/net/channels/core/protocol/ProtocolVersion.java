package com.github.xioshe.net.channels.core.protocol;


import lombok.Getter;

@Getter
public enum ProtocolVersion {
    V1_0("1.0"),
    V1_1("1.1");

    private final String version;

    ProtocolVersion(String version) {
        this.version = version;
    }
}