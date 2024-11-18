package com.github.xioshe.net.channels.core.exception;

public class CryptoException extends NetChannelsException {
    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}