package com.trinket.trinketos.service;

import com.trinket.trinketos.dto.AuthResponse;
import com.trinket.trinketos.dto.LoginRequest;
import com.trinket.trinketos.dto.RegisterRequest;
import com.trinket.trinketos.dto.TenantRegisterRequest;
import com.trinket.trinketos.model.DocumentType;
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
                if (request.documentType() == null || request.documentType().isBlank()) {
                        throw new IllegalArgumentException("Tipo do documento (CPF ou CNPJ) é obrigatório.");
                }
                if (request.taxId() == null || request.taxId().isBlank()) {
                        throw new IllegalArgumentException("CPF/CNPJ (tax_id) é obrigatório.");
                }
                String cleanTaxId = request.taxId().replaceAll("\\D", "");
                DocumentType docType = "CNPJ".equalsIgnoreCase(request.documentType()) ? DocumentType.CNPJ : DocumentType.CPF;
                if (docType == DocumentType.CPF && cleanTaxId.length() != 11) {
                        throw new IllegalArgumentException("CPF deve conter 11 dígitos.");
                }
                if (docType == DocumentType.CNPJ && cleanTaxId.length() != 14) {
                        throw new IllegalArgumentException("CNPJ deve conter 14 dígitos.");
                }

                var organization = Organization.builder()
                                .name(request.organizationName())
                                .slug(request.organizationSlug())
                                .documentType(docType)
                                .taxId(cleanTaxId)
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
                                                .of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                user.getRole().name())));

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
                                                .of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                user.getRole().name())));

                var jwtToken = jwtService.generateToken(userDetails, user.getOrganizationId(), user.getRole().name());
                return new AuthResponse(jwtToken, user.getId(), user.getName(), user.getEmail(), user.getRole(),
                                user.getOrganizationId());
        }

        public AuthResponse registerUser(RegisterRequest request, User adminUser) {
                var userBuilder = User.builder()
                                .name(request.name())
                                .email(request.email())
                                .password(passwordEncoder.encode(request.password()))
                                .role(request.role())
                                .organizationId(adminUser.getOrganizationId()) // Enforce Admin's Org
                                .teamId(request.teamId());

                if (request.document() != null && !request.document().isBlank()) {
                        String cleanDoc = request.document().replaceAll("\\D", "");
                        if (cleanDoc.length() == 11) {
                                userBuilder.document(cleanDoc);
                                userBuilder.documentType(com.trinket.trinketos.model.DocumentType.CPF);
                        } else if (cleanDoc.length() == 14) {
                                userBuilder.document(cleanDoc);
                                userBuilder.documentType(com.trinket.trinketos.model.DocumentType.CNPJ);
                        } else {
                                throw new IllegalArgumentException(
                                                "Documento inválido. Deve ter 11 (CPF) ou 14 (CNPJ) dígitos.");
                        }
                }

                var user = userBuilder.build();
                userRepository.save(user);

                // Standard user details adapter
                var userDetails = new org.springframework.security.core.userdetails.User(
                                user.getEmail(),
                                user.getPassword(),
                                java.util.List
                                                .of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                user.getRole().name())));

                var jwtToken = jwtService.generateToken(userDetails, user.getOrganizationId(), user.getRole().name());
                return new AuthResponse(jwtToken, user.getId(), user.getName(), user.getEmail(), user.getRole(),
                                user.getOrganizationId());
        }
}
