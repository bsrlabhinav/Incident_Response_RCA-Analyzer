package org.example.incidentresponse.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record IncidentResponse(
        String id,
        String title,
        String description,
        String severity,
        String status,
        String reporterId,
        String assigneeId,
        Map<String, List<String>> tags,
        Instant createdAt,
        Instant updatedAt
) {}
