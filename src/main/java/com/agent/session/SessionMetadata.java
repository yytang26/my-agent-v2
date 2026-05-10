package com.agent.session;

import java.time.LocalDateTime;

public record SessionMetadata(
        String id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt,
        int messageCount,
        String model
) {
}
