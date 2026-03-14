package com.trinket.trinketos.controller;

import com.trinket.trinketos.dto.TeamRequest;
import com.trinket.trinketos.dto.TeamResponse;
import com.trinket.trinketos.dto.UserResponse;
import com.trinket.trinketos.model.Category;
import com.trinket.trinketos.model.Role;
import com.trinket.trinketos.model.Team;
import com.trinket.trinketos.model.User;
import com.trinket.trinketos.repository.CategoryRepository;
import com.trinket.trinketos.repository.TeamRepository;
import com.trinket.trinketos.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.criteria.Predicate;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Team management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TeamController {

  private final TeamRepository teamRepository;
  private final UserRepository userRepository;
  private final CategoryRepository categoryRepository;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @Operation(summary = "Create a new Team", responses = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Team created successfully"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden: Only Admin can create teams"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict: Slug already exists")
  })
  public ResponseEntity<TeamResponse> createTeam(@RequestBody TeamRequest request, Authentication authentication) {
    User admin = getUser(authentication);

    String name = normalizeText(request.name());
    String displayName = normalizeText(request.displayName());

    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Name is required");
    }
    if (displayName == null || displayName.isBlank()) {
      throw new IllegalArgumentException("Display Name is required");
    }

    com.trinket.trinketos.util.StringUtils.validateString(name, "Name",
        com.trinket.trinketos.util.StringUtils.ValidationMode.STRICT_NAME, true);
    if (displayName != null) {
      com.trinket.trinketos.util.StringUtils.validateString(displayName, "Display Name",
          com.trinket.trinketos.util.StringUtils.ValidationMode.STRICT_NAME, true);
    }
    com.trinket.trinketos.util.StringUtils.validateString(request.description(), "Description",
        com.trinket.trinketos.util.StringUtils.ValidationMode.DESCRIPTION_NO_EMOJI, true);

    String slug = com.trinket.trinketos.util.SlugUtils.toSlug(name);
    if (teamRepository.existsBySlugAndOrganizationId(slug, admin.getOrganizationId())) {
      // Simple collision handling: append suffix? Or just fail?
      // User requirement implies "automatically based on name", but usually unique
      // constraint.
      // returning 409 for now.
      return ResponseEntity.status(409).build();
    }

    Team team = Team.builder()
        .name(name)
        .displayName(displayName)
        .slug(slug)
        .description(request.description())
        .organizationId(admin.getOrganizationId())
        .build();

    // Associate categories
    if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {
      Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.categoryIds()));
      team.setCategories(categories);
    }

    Team saved = teamRepository.save(team);
    return ResponseEntity.ok(mapToResponse(saved));
  }

  @GetMapping
  @Operation(summary = "List all teams for the organization (Paged)", responses = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of teams retrieved"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<Page<TeamResponse>> getTeams(
      Authentication authentication,
      @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
      @RequestParam(required = false) String search) {

    User user = getUser(authentication);

    Specification<Team> spec = (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("organizationId"), user.getOrganizationId()));

      if (search != null && !search.isBlank()) {
        String likePattern = "%" + search.toLowerCase() + "%";
        predicates.add(cb.or(
            cb.like(cb.lower(root.get("name")), likePattern),
            cb.like(cb.lower(root.get("displayName")), likePattern),
            cb.like(cb.lower(root.get("slug")), likePattern)));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };

    Page<Team> page = teamRepository.findAll(spec, pageable);
    return ResponseEntity.ok(page.map(this::mapToResponse));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get team details")
  public ResponseEntity<TeamResponse> getTeam(@PathVariable UUID id, Authentication authentication) {
    User user = getUser(authentication);
    Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));

    if (!team.getOrganizationId().equals(user.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }
    return ResponseEntity.ok(mapToResponse(team));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @Operation(summary = "Update team")
  public ResponseEntity<TeamResponse> updateTeam(@PathVariable UUID id, @RequestBody TeamRequest request,
      Authentication authentication) {
    User admin = getUser(authentication);
    Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));

    if (!team.getOrganizationId().equals(admin.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }

    String name = normalizeText(request.name());
    String displayName = normalizeText(request.displayName());

    if (name != null && !name.isBlank()) {
      com.trinket.trinketos.util.StringUtils.validateString(name, "Name",
          com.trinket.trinketos.util.StringUtils.ValidationMode.STRICT_NAME, true);
      team.setName(name);
    }

    if (displayName != null && !displayName.isBlank()) {
      com.trinket.trinketos.util.StringUtils.validateString(displayName, "Display Name",
          com.trinket.trinketos.util.StringUtils.ValidationMode.STRICT_NAME, true);
      team.setDisplayName(displayName);
    }

    if (request.description() != null) {
      com.trinket.trinketos.util.StringUtils.validateString(request.description(), "Description",
          com.trinket.trinketos.util.StringUtils.ValidationMode.DESCRIPTION_NO_EMOJI, true);
      team.setDescription(request.description());
    }

    // Update categories
    if (request.categoryIds() != null) {
      Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.categoryIds()));
      team.setCategories(categories);
    }

    Team updated = teamRepository.save(team);
    return ResponseEntity.ok(mapToResponse(updated));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @Operation(summary = "Delete a team")
  public ResponseEntity<Void> deleteTeam(@PathVariable UUID id, Authentication authentication) {
    User admin = getUser(authentication);
    Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));

    if (!team.getOrganizationId().equals(admin.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }

    teamRepository.delete(team);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/count")
  @Operation(summary = "Count teams")
  public ResponseEntity<Long> countTeams(Authentication authentication) {
    User user = getUser(authentication);
    long count = teamRepository
        .count((root, query, cb) -> cb.equal(root.get("organizationId"), user.getOrganizationId()));
    return ResponseEntity.ok(count);
  }

  // --- Team Members ---

  @PutMapping("/{id}/members")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @Operation(summary = "Assign an agent to a team")
  public ResponseEntity<UserResponse> addMember(
      @PathVariable UUID id,
      @RequestBody Map<String, UUID> body,
      Authentication authentication) {

    User admin = getUser(authentication);
    Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));

    if (!team.getOrganizationId().equals(admin.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }

    UUID agentId = body.get("agentId");
    if (agentId == null) {
      throw new IllegalArgumentException("agentId is required");
    }

    User agent = userRepository.findById(agentId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    if (!agent.getOrganizationId().equals(admin.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }

    if (agent.getRole() != Role.ROLE_AGENT) {
      throw new IllegalArgumentException("Only agents can be assigned to teams");
    }

    agent.setTeamId(team.getId());
    userRepository.save(agent);

    return ResponseEntity.ok(mapUserToResponse(agent));
  }

  @GetMapping("/{id}/members")
  @Operation(summary = "List team members")
  public ResponseEntity<List<UserResponse>> getMembers(
      @PathVariable UUID id,
      Authentication authentication) {

    User user = getUser(authentication);
    Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));

    if (!team.getOrganizationId().equals(user.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }

    List<User> members = userRepository.findAll((root, query, cb) -> cb.and(
        cb.equal(root.get("teamId"), team.getId()),
        cb.equal(root.get("organizationId"), user.getOrganizationId())
    ));

    return ResponseEntity.ok(members.stream().map(this::mapUserToResponse).collect(Collectors.toList()));
  }

  @DeleteMapping("/{id}/members/{userId}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @Operation(summary = "Remove an agent from a team")
  public ResponseEntity<Void> removeMember(
      @PathVariable UUID id,
      @PathVariable UUID userId,
      Authentication authentication) {

    User admin = getUser(authentication);
    Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));

    if (!team.getOrganizationId().equals(admin.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }

    User agent = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    if (!team.getId().equals(agent.getTeamId())) {
      throw new IllegalArgumentException("User is not a member of this team");
    }

    agent.setTeamId(null);
    userRepository.save(agent);

    return ResponseEntity.noContent().build();
  }

  private User getUser(Authentication authentication) {
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    return userRepository.findByEmail(userDetails.getUsername())
        .orElseThrow(() -> new RuntimeException("User not found"));
  }

  private String normalizeText(String text) {
    if (text == null) {
      return null;
    }
    return text.trim().replaceAll("\\s+", " ");
  }

  private TeamResponse mapToResponse(Team t) {
    List<UUID> categoryIds = t.getCategories() != null
        ? t.getCategories().stream().map(Category::getId).collect(Collectors.toList())
        : List.of();
    return new TeamResponse(t.getId(), t.getName(), t.getDisplayName(), t.getDescription(), t.getOrganizationId(),
        t.getCreatedAt(), t.getUpdatedAt(), categoryIds);
  }

  private UserResponse mapUserToResponse(User user) {
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
