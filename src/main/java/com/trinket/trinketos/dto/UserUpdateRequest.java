package com.trinket.trinketos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record UserUpdateRequest(
    @Schema(description = "User's full name", example = "Jane Doe") String name,

    @Schema(description = "Team ID to assign user to", example = "d290f1ee-6c54-4b01-90e6-d701748f0851") UUID teamId,

    @Schema(description = "Document (CPF/CNPJ). Digits only.", example = "12345678901") String document) {
}
