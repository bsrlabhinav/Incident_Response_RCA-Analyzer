package org.example.incidentresponse.reporting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MeasurementRequest(
        @NotBlank String name,
        @NotNull MeasurementType type,
        String field
) {}
