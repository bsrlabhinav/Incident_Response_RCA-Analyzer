package org.example.incidentresponse.dto;

import java.time.Instant;

public record SlaResponse(
        String incidentId,
        Instant acknowledgedAt,
        Instant resolvedAt,
        Long acknowledgeSlaMs,
        Long resolutionSlaMs,
        boolean acknowledgeSlaBreached,
        boolean resolutionSlaBreached,
        Long actualAcknowledgeMs,
        Long actualResolutionMs
) {}
