package com.trinket.trinketos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String title;

  @Column(unique = true, nullable = false)
  private String code; // TKT-1234ABCD

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  private TicketStatus status;

  @Enumerated(EnumType.STRING)
  private Priority priority;

  private String category;

  private String sentiment;

  @Column(columnDefinition = "TEXT")
  private String diagnosis;

  @Column(columnDefinition = "TEXT")
  private String suggestedSolution;

  @Column(name = "customer_id")
  private UUID customerId;

  @Column(name = "agent_id")
  private UUID agentId;

  @Column(name = "team_id")
  private UUID teamId;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createdAt;
}
