package com.trinket.trinketos.service;

import com.trinket.trinketos.dto.AuthResponse;
import com.trinket.trinketos.dto.LoginRequest;
import com.trinket.trinketos.dto.RegisterRequest;
import com.trinket.trinketos.dto.TenantRegisterRequest;
import com.trinket.trinketos.model.Organization;
import com.trinket.trinketos.model.Role;
import com.trinket.trinketos.model.User;
import com.trinket.trinketos.repository.OrganizationRepository;
import com.trinket.trinketos.repository.UserRepository;
import com.trinket.trinketos.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private final UserRepository userRepository;
  private final OrganizationRepository organizationRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  @Transactional
  public AuthResponse registerTenant(TenantRegisterRequest request) {
    var organization = Organization.builder()
        .name(request.organizationName())
        .slug(request.organizationSlug())
        .build();
    organizationRepository.save(organization);

    var user = User.builder()
        .name(request.adminName())
        .email(request.adminEmail())
        .password(passwordEncoder.encode(request.adminPassword()))
        .role(Role.ROLE_ADMIN)
        .organizationId(organization.getId())
        .build();
    userRepository.save(user);

    // Standard user details adapter
    var userDetails = new org.springframework.security.core.userdetails.User(
        user.getEmail(),
        user.getPassword(),
        java.util.List
            .of(new org.springframework.security.core.authority.SimpleGrantedAuthority(user.getRole().name())));

    var jwtToken = jwtService.generateToken(userDetails, user.getOrganizationId(), user.getRole().name());
    return new AuthResponse(jwtToken, user.getId(), user.getName(), user.getEmail(), user.getRole(),
        user.getOrganizationId());
  }

  public AuthResponse authenticate(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    var user = userRepository.findByEmail(request.email())
        .orElseThrow();

    var userDetails = new org.springframework.security.core.userdetails.User(
        user.getEmail(),
        user.getPassword(),
        java.util.List
            .of(new org.springframework.security.core.authority.SimpleGrantedAuthority(user.getRole().name())));

    var jwtToken = jwtService.generateToken(userDetails, user.getOrganizationId(), user.getRole().name());
    return new AuthResponse(jwtToken, user.getId(), user.getName(), user.getEmail(), user.getRole(),
        user.getOrganizationId());
  }

  public User registerUser(RegisterRequest request, User admin) {
    var user = User.builder()
        .name(request.name())
        .email(request.email())
        .password(passwordEncoder.encode(request.password()))
        .role(request.role())
        .organizationId(admin.getOrganizationId()) // Enforce Admin's Org
        .build();
    return userRepository.save(user);
  }
}
