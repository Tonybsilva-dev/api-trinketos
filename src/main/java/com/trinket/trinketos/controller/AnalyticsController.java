package com.trinket.trinketos.controller;

import com.trinket.trinketos.dto.AnalyticsResponse;
import com.trinket.trinketos.dto.TimePeriod;
import com.trinket.trinketos.model.User;
import com.trinket.trinketos.repository.UserRepository;
import com.trinket.trinketos.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Dashboard and Performance metrics")
public class AnalyticsController {

  private final AnalyticsService analyticsService;
  private final UserRepository userRepository;

  @GetMapping("/dashboard")
  @Operation(summary = "Get main dashboard metrics (Admin: All, Agent: Personal)", responses = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Metrics retrieved successfully"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<AnalyticsResponse> getDashboardMetrics(
      @RequestParam(defaultValue = "MONTH") TimePeriod range,
      Authentication authentication) {

    User user = getUser(authentication);

    // If Admin, get Org wide metrics. If Agent, get Personal metrics.
    UUID agentIdFilter = "ROLE_AGENT".equals(user.getRole().name()) ? user.getId() : null;

    return ResponseEntity.ok(analyticsService.getAnalytics(user.getOrganizationId(), agentIdFilter, range));
  }

  @GetMapping("/advanced")
  @Operation(summary = "Get advanced analytics (Admin can filter by agent)", responses = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Metrics retrieved successfully"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden (Agent trying to see others)")
  })
  public ResponseEntity<AnalyticsResponse> getAdvancedAnalytics(
      @RequestParam(defaultValue = "MONTH") TimePeriod range,
      @RequestParam(required = false) UUID agentId,
      Authentication authentication) {

    User user = getUser(authentication);

    // Security check: Only Admin can view other agents. Agents can only view
    // themselves.
    if ("ROLE_AGENT".equals(user.getRole().name())) {
      if (agentId != null && !agentId.equals(user.getId())) {
        return ResponseEntity.status(403).build();
      }
      agentId = user.getId();
    }

    return ResponseEntity.ok(analyticsService.getAnalytics(user.getOrganizationId(), agentId, range));
  }

  private User getUser(Authentication authentication) {
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    return userRepository.findByEmail(userDetails.getUsername())
        .orElseThrow(() -> new RuntimeException("User not found"));
  }
}
