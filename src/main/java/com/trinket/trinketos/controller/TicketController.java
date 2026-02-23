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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
  @Operation(summary = "Create a new ticket and trigger AI analysis", responses = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket created successfully (AI analysis started)"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<TicketResponse> createTicket(@RequestBody TicketRequest request,
      Authentication authentication) {
    User user = getUser(authentication);

    String ticketCode = generateUniqueCode();

    Ticket ticket = Ticket.builder()
        .title(request.title())
        .description(request.description())
        .code(ticketCode)
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

  private String generateUniqueCode() {
    int maxRetries = 5;
    for (int i = 0; i < maxRetries; i++) {
      String code = "TKT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
      if (!ticketRepository.existsByCode(code)) {
        return code;
      }
    }
    throw new RuntimeException("Failed to generate unique ticket code after retries");
  }

  @GetMapping
  @Operation(summary = "List all tickets (Paged)", description = "Search by Title, Description or Ticket Code (e.g. 'TKT-1234' or '1234'). Filters by Status and Priority.", responses = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of tickets retrieved"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<org.springframework.data.domain.Page<TicketResponse>> getTickets(
      Authentication authentication,
      @org.springdoc.core.annotations.ParameterObject @org.springframework.data.web.PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable,
      @RequestParam(required = false) TicketStatus status,
      @RequestParam(required = false) com.trinket.trinketos.model.Priority priority,
      @RequestParam(required = false) String search) {

    User user = getUser(authentication);

    org.springframework.data.jpa.domain.Specification<Ticket> spec = (root, query, cb) -> {
      java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

      // Mandatory: Organization
      predicates.add(cb.equal(root.get("organizationId"), user.getOrganizationId()));

      // Filters
      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }
      if (priority != null) {
        predicates.add(cb.equal(root.get("priority"), priority));
      }
      if (search != null && !search.isBlank()) {
        String likePattern = "%" + search.toLowerCase() + "%";

        // Handle Ticket Code Search
        String codeSearch = search.trim().toUpperCase();
        if (codeSearch.startsWith("TKT-")) {
          // Already full code, try exact match or like
          predicates.add(cb.or(
              cb.like(cb.upper(root.get("code")), "%" + codeSearch + "%"),
              cb.like(cb.lower(root.get("title")), likePattern),
              cb.like(cb.lower(root.get("description")), likePattern)));
        } else if (codeSearch.matches("^[A-Z0-9]{8}$")) {
          // Potential short code
          String fullCode = "TKT-" + codeSearch;
          predicates.add(cb.or(
              cb.equal(root.get("code"), fullCode),
              cb.like(cb.lower(root.get("title")), likePattern),
              cb.like(cb.lower(root.get("description")), likePattern)));
        } else {
          predicates.add(cb.or(
              cb.like(cb.lower(root.get("title")), likePattern),
              cb.like(cb.lower(root.get("description")), likePattern)));
        }
      }

      return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
    };

    org.springframework.data.domain.Page<Ticket> page = ticketRepository.findAll(spec, pageable);
    return ResponseEntity.ok(page.map(this::mapToResponse));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get ticket details")
  public ResponseEntity<TicketResponse> getTicket(@PathVariable UUID id, Authentication authentication) {
    User currentUser = getUser(authentication);

    Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new RuntimeException("Ticket not found"));

    if (!ticket.getOrganizationId().equals(currentUser.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }
    return ResponseEntity.ok(mapToResponse(ticket));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update ticket")
  public ResponseEntity<TicketResponse> updateTicket(@PathVariable UUID id, @RequestBody TicketRequest request,
      Authentication authentication) {
    User currentUser = getUser(authentication);
    Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new RuntimeException("Ticket not found"));

    if (!ticket.getOrganizationId().equals(currentUser.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }

    // Allow updating basic fields. For sophisticated workflow (status transitions)
    // maybe separate endpoint, but standard CRUD implies PUT.
    if (request.title() != null)
      ticket.setTitle(request.title());
    if (request.description() != null)
      ticket.setDescription(request.description());
    if (request.priority() != null)
      ticket.setPriority(request.priority());
    if (request.status() != null)
      ticket.setStatus(request.status());

    // TicketRequest now includes status.

    Ticket updated = ticketRepository.save(ticket);
    return ResponseEntity.ok(mapToResponse(updated));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @Operation(summary = "Delete ticket")
  public ResponseEntity<Void> deleteTicket(@PathVariable UUID id, Authentication authentication) {
    User currentUser = getUser(authentication);
    Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new RuntimeException("Ticket not found"));

    if (!ticket.getOrganizationId().equals(currentUser.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }

    ticketRepository.delete(ticket);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/count")
  @Operation(summary = "Count tickets")
  public ResponseEntity<Long> countTickets(Authentication authentication) {
    User currentUser = getUser(authentication);
    long count = ticketRepository
        .count((root, query, cb) -> cb.equal(root.get("organizationId"), currentUser.getOrganizationId()));
    return ResponseEntity.ok(count);
  }

  private User getUser(Authentication authentication) {
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    User user = userRepository.findByEmail(userDetails.getUsername())
        .orElseThrow(() -> new RuntimeException("User not found"));

    // Business Rule: Agents must have a Team.
    if (user.getRole() == com.trinket.trinketos.model.Role.ROLE_AGENT && user.getTeamId() == null) {
      throw new org.springframework.security.access.AccessDeniedException(
          "Agentes não vinculados a um time não podem visualizar ou atuar em tickets.");
    }

    return user;
  }

  private TicketResponse mapToResponse(Ticket t) {
    return new TicketResponse(
        t.getId(), t.getCode(), t.getTitle(), t.getDescription(), t.getStatus(),
        t.getPriority(), t.getCategory(), t.getSentiment(),
        t.getDiagnosis(), t.getSuggestedSolution(),
        t.getCustomerId(), t.getAgentId(), t.getTeamId(), t.getOrganizationId(), t.getCreatedAt());
  }
}
