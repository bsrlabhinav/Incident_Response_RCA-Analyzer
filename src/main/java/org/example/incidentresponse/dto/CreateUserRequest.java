package org.example.incidentresponse.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank String displayName,
        String email,
        String role
) {}
