package org.example.incidentresponse.dto;

import java.time.Instant;

public record IncidentAuditResponse(
        String id,
        String incidentId,
        String userId,
        String field,
        String oldValue,
        String newValue,
        Instant createdAt
) {}
