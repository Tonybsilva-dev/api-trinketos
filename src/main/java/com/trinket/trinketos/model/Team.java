package com.trinket.trinketos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "teams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(name = "display_name")
  private String displayName;

  @Column(nullable = false)
  private String slug;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  private LocalDateTime updatedAt;
}
