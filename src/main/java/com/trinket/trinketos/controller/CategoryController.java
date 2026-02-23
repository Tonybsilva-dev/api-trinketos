package com.trinket.trinketos.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import com.trinket.trinketos.model.Category;
import com.trinket.trinketos.model.User;
import com.trinket.trinketos.repository.CategoryRepository;
import com.trinket.trinketos.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

  private final CategoryRepository categoryRepository;
  private final UserRepository userRepository;

  @GetMapping
  @Operation(summary = "List all categories (Paged)", description = "Search by name or description")
  public ResponseEntity<Page<Category>> getCategories(
      Authentication authentication,
      @org.springdoc.core.annotations.ParameterObject @PageableDefault(size = 10, sort = "name") Pageable pageable,
      @RequestParam(required = false) String search) {
    User user = getUser(authentication);

    Specification<Category> spec = (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("organizationId"), user.getOrganizationId()));

      if (search != null && !search.isBlank()) {
        String likePattern = "%" + search.toLowerCase() + "%";
        predicates.add(cb.or(
            cb.like(cb.lower(root.get("name")), likePattern),
            cb.like(cb.lower(root.get("description")), likePattern)));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };

    return ResponseEntity.ok(categoryRepository.findAll(spec, pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get category by ID")
  public ResponseEntity<Category> getCategoryById(@PathVariable UUID id, Authentication authentication) {
    User user = getUser(authentication);
    Category category = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));

    if (!category.getOrganizationId().equals(user.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }

    return ResponseEntity.ok(category);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
  @Operation(summary = "Update a category")
  public ResponseEntity<Category> updateCategory(@PathVariable UUID id, @RequestBody Category request,
      Authentication authentication) {
    User user = getUser(authentication);
    Category category = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));

    if (!category.getOrganizationId().equals(user.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }

    if (request.getName() != null) {
      if (request.getName().isBlank()) {
        throw new IllegalArgumentException("Name cannot be empty");
      }
      com.trinket.trinketos.util.StringUtils.validateString(request.getName(), "Name",
          com.trinket.trinketos.util.StringUtils.ValidationMode.STRICT_NAME, true);
      category.setName(request.getName());
    }
    if (request.getDescription() != null) {
      com.trinket.trinketos.util.StringUtils.validateString(request.getDescription(), "Description",
          com.trinket.trinketos.util.StringUtils.ValidationMode.DESCRIPTION_NO_EMOJI, true);
      category.setDescription(request.getDescription());
    }

    return ResponseEntity.ok(categoryRepository.save(category));
  }

  @GetMapping("/count")
  @Operation(summary = "Count categories")
  public ResponseEntity<Long> countCategories(Authentication authentication) {
    User user = getUser(authentication);
    long count = categoryRepository
        .count((root, query, cb) -> cb.equal(root.get("organizationId"), user.getOrganizationId()));
    return ResponseEntity.ok(count);
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
  @Operation(summary = "Create a new category")
  public ResponseEntity<Category> createCategory(@RequestBody Category category, Authentication authentication) {
    User user = getUser(authentication);
    if (category.getName() == null || category.getName().isBlank()) {
      throw new IllegalArgumentException("Name is required");
    }
    com.trinket.trinketos.util.StringUtils.validateString(category.getName(), "Name",
        com.trinket.trinketos.util.StringUtils.ValidationMode.STRICT_NAME, true);
    com.trinket.trinketos.util.StringUtils.validateString(category.getDescription(), "Description",
        com.trinket.trinketos.util.StringUtils.ValidationMode.DESCRIPTION_NO_EMOJI, true);

    category.setOrganizationId(user.getOrganizationId());
    return ResponseEntity.ok(categoryRepository.save(category));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
  @Operation(summary = "Delete a category")
  public ResponseEntity<Void> deleteCategory(@PathVariable UUID id, Authentication authentication) {
    User user = getUser(authentication);
    Category category = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));

    if (!category.getOrganizationId().equals(user.getOrganizationId())) {
      return ResponseEntity.status(403).build();
    }

    categoryRepository.delete(category);
    return ResponseEntity.noContent().build();
  }

  private User getUser(Authentication authentication) {
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    return userRepository.findByEmail(userDetails.getUsername())
        .orElseThrow(() -> new RuntimeException("User not found"));
  }

}
