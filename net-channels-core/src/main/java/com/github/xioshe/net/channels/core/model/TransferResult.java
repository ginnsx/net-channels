package com.github.xioshe.net.channels.core.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TransferResult {
    private TransferStatus status;
    private String data;
    private double progress;
    private List<Integer> missingChunks;
    private String sessionId;

    public enum TransferStatus {
        IN_PROGRESS,
        COMPLETED
    }

    public boolean isCompleted() {
        return status == TransferStatus.COMPLETED;
    }
}