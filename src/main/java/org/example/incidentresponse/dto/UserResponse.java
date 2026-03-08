package org.example.incidentresponse.dto;

import java.time.Instant;

public record UserResponse(
        String id,
        String displayName,
        String email,
        String role,
        Instant createdAt
) {}
