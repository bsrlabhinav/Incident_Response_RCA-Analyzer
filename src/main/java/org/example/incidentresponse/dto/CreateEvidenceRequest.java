package org.example.incidentresponse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateEvidenceRequest(
        @NotBlank String fileName,
        @NotBlank String fileUrl,
        String description,
        @NotNull UUID uploadedBy
) {}
