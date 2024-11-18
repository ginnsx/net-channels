package com.github.xioshe.net.channels.core.session;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

@Data
public class TransferSession {
    private final String sessionId;
    private final int totalChunks;
    private final int totalSize;
    private final Instant createdAt;
    private final BitSet receivedChunks;
    private Instant lastUpdatedAt;
    private SessionState state;
    private int receivedCount;

    public TransferSession(String sessionId, int totalChunks, int totalSize) {
        this.sessionId = sessionId;
        this.totalChunks = totalChunks;
        this.totalSize = totalSize;
        this.createdAt = Instant.now();
        this.lastUpdatedAt = Instant.now();
        this.state = SessionState.INITIALIZED;
        this.receivedChunks = new BitSet(totalChunks);
        this.receivedCount = 0;
    }

    public void markChunkReceived(int chunkIndex) {
        if (!receivedChunks.get(chunkIndex)) {
            receivedChunks.set(chunkIndex);
            receivedCount++;
            lastUpdatedAt = Instant.now();
        }
    }

    public boolean isComplete() {
        return receivedCount == totalChunks;
    }

    public double getProgress() {
        return (double) receivedCount / totalChunks;
    }

    /**
     * 获取所有未接收的分片索引
     */
    public List<Integer> getMissingChunks() {
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            if (!receivedChunks.get(i)) {
                missing.add(i);
            }
        }
        return missing;
    }

    /**
     * 获取连续的未接收分片范围
     * 返回格式：[开始索引, 结束索引]的列表
     */
    public List<int[]> getMissingRanges() {
        List<int[]> ranges = new ArrayList<>();
        int start = -1;

        for (int i = 0; i < totalChunks; i++) {
            if (!receivedChunks.get(i)) {
                if (start == -1) {
                    start = i;
                }
            } else if (start != -1) {
                ranges.add(new int[]{start, i - 1});
                start = -1;
            }
        }

        if (start != -1) {
            ranges.add(new int[]{start, totalChunks - 1});
        }

        return ranges;
    }
}