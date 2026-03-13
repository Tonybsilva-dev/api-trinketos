package com.trinket.trinketos.dto;

import java.util.List;
import java.util.UUID;

import java.time.LocalDateTime;

public record TeamResponse(UUID id, String name, String displayName, String description, UUID organizationId,
    LocalDateTime createdAt, LocalDateTime updatedAt, List<UUID> categoryIds) {
}
