package com.trinket.trinketos.dto;

import com.trinket.trinketos.model.Priority;
import com.trinket.trinketos.model.TicketStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
    UUID id,
    String title,
    String description,
    TicketStatus status,
    Priority priority,
    String category,
    String sentiment,
    UUID customerId,
    UUID agentId,
    UUID organizationId,
    LocalDateTime createdAt) {
}
