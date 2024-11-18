package com.github.xioshe.net.channels.core.session;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionStats {
    private final int activeSessions;
    private final int totalCreated;
    private final int totalCompleted;
    private final int totalFailed;
}