package com.trinket.trinketos.controller;

import com.trinket.trinketos.dto.AuthResponse;
import com.trinket.trinketos.dto.LoginRequest;
import com.trinket.trinketos.dto.RegisterRequest;
import com.trinket.trinketos.dto.TenantRegisterRequest;
import com.trinket.trinketos.model.User;
import com.trinket.trinketos.repository.UserRepository;
import com.trinket.trinketos.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth management")
public class AuthController {

  private final AuthenticationService service;
  private final UserRepository userRepository;

  @PostMapping("/register-tenant")
  @Operation(summary = "Register a new Organization (Tenant) and Admin", responses = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization created successfully"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Organization slug or Admin email already exists")
  })
  public ResponseEntity<AuthResponse> registerTenant(@RequestBody TenantRegisterRequest request) {
    return ResponseEntity.ok(service.registerTenant(request));
  }

  @PostMapping("/login")
  @Operation(summary = "Login to get JWT", responses = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authentication successful"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
  })
  public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    return ResponseEntity.ok(service.authenticate(request));
  }

  @PostMapping("/register-user")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @Operation(summary = "Register a new user (Agent/Customer) for the organization", responses = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User registered successfully"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input (e.g. invalid Document format)"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Only Admin can register users"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already exists")
  })
  public ResponseEntity<AuthResponse> registerUser(
      @RequestBody RegisterRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    // Fetch full admin user to get OrganizationID
    User admin = userRepository.findByEmail(userDetails.getUsername())
        .orElseThrow(() -> new RuntimeException("Admin not found"));

    return ResponseEntity.ok(service.registerUser(request, admin));
  }
}
