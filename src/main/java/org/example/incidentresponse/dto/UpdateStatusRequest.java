package org.example.incidentresponse.dto;

import jakarta.validation.constraints.NotNull;
import org.example.incidentresponse.enums.IncidentStatus;

import java.util.UUID;

public record UpdateStatusRequest(
        @NotNull IncidentStatus status,
        @NotNull UUID userId
) {}
