package com.trinket.trinketos.dto;

import java.util.List;
import java.util.UUID;

public record TeamRequest(String name, String displayName, String description, List<UUID> categoryIds) {
}
