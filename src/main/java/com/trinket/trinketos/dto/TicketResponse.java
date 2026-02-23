package com.trinket.trinketos.dto;

import com.trinket.trinketos.model.Priority;
import com.trinket.trinketos.model.TicketStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
    UUID id,
    String code,
    String title,
    String description,
    TicketStatus status,
    Priority priority,
    String category,
    String sentiment,
    String diagnosis,
    String suggestedSolution,
    UUID customerId,
    UUID agentId,
    UUID teamId,
    UUID organizationId,
    LocalDateTime createdAt) {
}
