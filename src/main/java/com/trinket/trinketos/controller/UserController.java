package com.trinket.trinketos.controller;

import com.trinket.trinketos.dto.AuthResponse;
import com.trinket.trinketos.dto.RegisterRequest;
import com.trinket.trinketos.dto.UserResponse;
import com.trinket.trinketos.dto.UserUpdateRequest;
import com.trinket.trinketos.model.User;
import com.trinket.trinketos.repository.UserRepository;
import com.trinket.trinketos.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private final UserRepository userRepository;
  private final AuthenticationService authenticationService;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @Operation(summary = "Create a new user (Admin only)", description = "Delegates to AuthenticationService.registerUser")
  public ResponseEntity<AuthResponse> createUser(@RequestBody RegisterRequest request, Authentication authentication) {
    User admin = getAuthenticatedUser(authentication);
    return ResponseEntity.ok(authenticationService.registerUser(request, admin));
  }

  @GetMapping
  @Operation(summary = "List users (Paged)")
  public ResponseEntity<Page<UserResponse>> listUsers(
      Authentication authentication,
      @org.springdoc.core.annotations.ParameterObject @org.springframework.data.web.PageableDefault(size = 10, sort = "name") Pageable pageable,
      @RequestParam(required = false) String search) {

    User currentUser = getAuthenticatedUser(authentication);

    Specification<User> spec = (root, query, cb) -> {
      var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

      // Filter by Organization
      predicates.add(cb.equal(root.get("organizationId"), currentUser.getOrganizationId()));

      if (search != null && !search.isBlank()) {
        String likePattern = "%" + search.toLowerCase() + "%";
        predicates.add(cb.or(
            cb.like(cb.lower(root.get("name")), likePattern),
            cb.like(cb.lower(root.get("email")), likePattern)));
      }

      return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
    };

    Page<User> page = userRepository.findAll(spec, pageable);
    return ResponseEntity.ok(page.map(this::mapToResponse));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get user details")
  public ResponseEntity<UserResponse> getUser(@PathVariable UUID id, Authentication authentication) {
    User currentUser = getAuthenticatedUser(authentication);
    User user = userRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("User not found"));

    if (!user.getOrganizationId().equals(currentUser.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }

    return ResponseEntity.ok(mapToResponse(user));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')") // Only Admin can update other users for now
  @Operation(summary = "Update user details")
  public ResponseEntity<UserResponse> updateUser(
      @PathVariable UUID id,
      @RequestBody UserUpdateRequest request,
      Authentication authentication) {

    User currentUser = getAuthenticatedUser(authentication);
    User user = userRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("User not found"));

    if (!user.getOrganizationId().equals(currentUser.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }

    if (request.name() != null)
      user.setName(request.name());
    if (request.teamId() != null)
      user.setTeamId(request.teamId());
    // Document update logic if needed (reuse auth service logic? or simple set)
    // For simplicity, implementing basic check similar to Auth Service
    if (request.document() != null && !request.document().isBlank()) {
      String cleanDoc = request.document().replaceAll("\\D", "");
      if (cleanDoc.length() == 11) {
        user.setDocument(cleanDoc);
        user.setDocumentType(com.trinket.trinketos.model.DocumentType.CPF);
      } else if (cleanDoc.length() == 14) {
        user.setDocument(cleanDoc);
        user.setDocumentType(com.trinket.trinketos.model.DocumentType.CNPJ);
      } else {
        throw new IllegalArgumentException("Documento inválido. Deve ter 11 (CPF) ou 14 (CNPJ) dígitos.");
      }
    }

    userRepository.save(user);
    return ResponseEntity.ok(mapToResponse(user));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @Operation(summary = "Delete user")
  public ResponseEntity<Void> deleteUser(@PathVariable UUID id, Authentication authentication) {
    User currentUser = getAuthenticatedUser(authentication);
    User user = userRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("User not found"));

    if (!user.getOrganizationId().equals(currentUser.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }

    // Prevent deleting self?
    if (user.getId().equals(currentUser.getId())) {
      throw new IllegalArgumentException("Cannot delete yourself.");
    }

    userRepository.delete(user);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/count")
  @Operation(summary = "Count users", description = "Optionally filter by role (e.g. ROLE_AGENT, ROLE_CUSTOMER)")
  public ResponseEntity<Long> countUsers(
      Authentication authentication,
      @RequestParam(required = false) com.trinket.trinketos.model.Role role) {

    User currentUser = getAuthenticatedUser(authentication);

    long count = userRepository.count((root, query, cb) -> {
      var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

      // Filter by Organization
      predicates.add(cb.equal(root.get("organizationId"), currentUser.getOrganizationId()));

      if (role != null) {
        predicates.add(cb.equal(root.get("role"), role));
      }

      return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
    });

    return ResponseEntity.ok(count);
  }

  private User getAuthenticatedUser(Authentication authentication) {
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    return userRepository.findByEmail(userDetails.getUsername())
        .orElseThrow(() -> new RuntimeException("User not found"));
  }

  private UserResponse mapToResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getRole(),
        user.getOrganizationId(),
        user.getTeamId(),
        user.getDocument(),
        user.getDocumentType());
  }
}
