package com.trinket.trinketos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RegisterRequest(
                @Schema(description = "User's full name", example = "John Doe") String name,

                @Schema(description = "User's email address", example = "john@example.com") String email,

                @Schema(description = "User's password", example = "securePassword123!") String password,

                @Schema(description = "Role (ADMIN, AGENT, CUSTOMER)", example = "ROLE_AGENT") com.trinket.trinketos.model.Role role,

                @Schema(hidden = true) java.util.UUID organizationId,

                @Schema(description = "Team ID (Required for Agents to access tickets)", example = "d290f1ee-6c54-4b01-90e6-d701748f0851") java.util.UUID teamId,

                @Schema(description = "Document (CPF or CNPJ). Digits only. Auto-detected by length (11=CPF, 14=CNPJ).", example = "12345678901") String document) {
}
