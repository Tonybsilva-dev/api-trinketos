package com.trinket.trinketos.dto;

import com.trinket.trinketos.model.DocumentType;
import com.trinket.trinketos.model.Role;

import java.util.UUID;

public record UserResponse(
    UUID id,
    String name,
    String email,
    Role role,
    UUID organizationId,
    UUID teamId,
    String document,
    DocumentType documentType) {
}
