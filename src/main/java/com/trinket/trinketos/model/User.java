package com.trinket.trinketos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "team_id")
  private UUID teamId;

  @Column(name = "document")
  private String document;

  @Enumerated(EnumType.STRING)
  @Column(name = "document_type")
  private DocumentType documentType;
}
