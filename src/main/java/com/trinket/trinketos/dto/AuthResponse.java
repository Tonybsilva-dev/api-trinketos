package com.trinket.trinketos.dto;

import com.trinket.trinketos.model.Role;
import java.util.UUID;

public record AuthResponse(
    String token,
    UUID userId,
    String name,
    String email,
    Role role,
    UUID organizationId) {
}
