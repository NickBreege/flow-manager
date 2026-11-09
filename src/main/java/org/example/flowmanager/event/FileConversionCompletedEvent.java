package org.example.flowmanager.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FileConversionCompletedEvent(
        UUID eventId,
        String eventType,
        OffsetDateTime occurredAt,
        Payload payload
) {
    public record Payload(String filePath,
                          String errorMessage
    ) {
    }
}
