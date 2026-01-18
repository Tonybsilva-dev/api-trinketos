package com.trinket.trinketos.controller;

import com.trinket.trinketos.dto.TicketRequest;
import com.trinket.trinketos.dto.TicketResponse;
import com.trinket.trinketos.model.Ticket;
import com.trinket.trinketos.model.TicketStatus;
import com.trinket.trinketos.model.User;
import com.trinket.trinketos.repository.TicketRepository;
import com.trinket.trinketos.repository.UserRepository;
import com.trinket.trinketos.service.TicketAIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Ticket management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

  private final TicketRepository ticketRepository;
  private final UserRepository userRepository;
  private final TicketAIService ticketAIService;

  @PostMapping
  @Operation(summary = "Create a new ticket and trigger AI analysis")
  public ResponseEntity<TicketResponse> createTicket(@RequestBody TicketRequest request,
      Authentication authentication) {
    User user = getUser(authentication);

    Ticket ticket = Ticket.builder()
        .title(request.title())
        .description(request.description())
        .priority(request.priority()) // could be null
        .status(TicketStatus.OPEN)
        .organizationId(user.getOrganizationId())
        .customerId(request.customerId()) // or set from current user if they are customer
        .build();

    Ticket saved = ticketRepository.save(ticket);

    // Trigger AI analysis
    ticketAIService.analyzeTicket(saved.getId());

    return ResponseEntity.ok(mapToResponse(saved));
  }

  @GetMapping
  @Operation(summary = "List all tickets for the organization")
  public ResponseEntity<List<TicketResponse>> getTickets(Authentication authentication) {
    User user = getUser(authentication);
    List<Ticket> tickets = ticketRepository.findByOrganizationId(user.getOrganizationId());
    return ResponseEntity.ok(tickets.stream().map(this::mapToResponse).collect(Collectors.toList()));
  }

  private User getUser(Authentication authentication) {
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    return userRepository.findByEmail(userDetails.getUsername())
        .orElseThrow(() -> new RuntimeException("User not found"));
  }

  private TicketResponse mapToResponse(Ticket t) {
    return new TicketResponse(
        t.getId(), t.getTitle(), t.getDescription(), t.getStatus(),
        t.getPriority(), t.getCategory(), t.getSentiment(),
        t.getCustomerId(), t.getAgentId(), t.getOrganizationId(), t.getCreatedAt());
  }
}
