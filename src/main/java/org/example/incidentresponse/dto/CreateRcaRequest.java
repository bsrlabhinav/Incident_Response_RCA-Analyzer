package org.example.incidentresponse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.incidentresponse.enums.RcaCategory;

import java.util.List;
import java.util.UUID;

public record CreateRcaRequest(
        @NotNull RcaCategory category,
        @NotBlank String summary,
        String details,
        List<String> actionItems,
        @NotNull UUID createdBy
) {}
