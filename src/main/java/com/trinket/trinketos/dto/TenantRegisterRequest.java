package com.trinket.trinketos.dto;

public record TenantRegisterRequest(
    String organizationName,
    String organizationSlug,
    String adminName,
    String adminEmail,
    String adminPassword) {
}
