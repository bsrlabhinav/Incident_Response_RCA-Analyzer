package org.example.incidentresponse.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignOwnerRequest(
        @NotNull UUID assigneeId,
        @NotNull UUID userId
) {}
