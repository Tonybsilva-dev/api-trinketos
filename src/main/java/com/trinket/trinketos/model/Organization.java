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
@Table(name = "organizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String slug;

  @Enumerated(EnumType.STRING)
  @Column(name = "document_type", nullable = false)
  private DocumentType documentType;

  @Column(name = "tax_id", nullable = false, unique = true)
  private String taxId;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createdAt;
}
