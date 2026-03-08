package org.example.incidentresponse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.incidentresponse.enums.Severity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateIncidentRequest(
        @NotBlank String title,
        String description,
        @NotNull Severity severity,
        @NotNull UUID reporterId,
        Map<String, List<String>> tags
) {}
