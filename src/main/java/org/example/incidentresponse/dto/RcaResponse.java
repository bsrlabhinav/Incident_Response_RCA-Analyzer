package org.example.incidentresponse.dto;

import java.time.Instant;
import java.util.List;

public record RcaResponse(
        String id,
        String incidentId,
        String category,
        String summary,
        String details,
        List<String> actionItems,
        String createdBy,
        Instant createdAt
) {}
