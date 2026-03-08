package org.example.incidentresponse.dto;

import java.time.Instant;

public record EvidenceResponse(
        String id,
        String incidentId,
        String fileName,
        String fileUrl,
        String description,
        String uploadedBy,
        Instant uploadedAt
) {}
