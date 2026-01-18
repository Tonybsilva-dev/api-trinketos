package com.trinket.trinketos.dto;

import com.trinket.trinketos.model.Priority;
import java.util.UUID;

public record TicketRequest(
    String title,
    String description,
    Priority priority, // User might set it, or let AI suggest.
    UUID customerId) {
}
