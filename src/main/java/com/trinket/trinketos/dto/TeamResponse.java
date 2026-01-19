package com.trinket.trinketos.dto;

import java.util.UUID;

public record TeamResponse(UUID id, String name, String displayName, String description, UUID organizationId) {
}
