package com.github.xioshe.net.channels.core.exception;

public class NetChannelsException extends RuntimeException {
    public NetChannelsException(String message) {
        super(message);
    }

    public NetChannelsException(String message, Throwable cause) {
        super(message, cause);
    }
}