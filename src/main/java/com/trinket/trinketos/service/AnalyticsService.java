package com.trinket.trinketos.service;

import com.trinket.trinketos.dto.AnalyticsResponse;
import com.trinket.trinketos.dto.TimePeriod;
import com.trinket.trinketos.model.Ticket;
import com.trinket.trinketos.model.TicketStatus;
import com.trinket.trinketos.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

  private final TicketRepository ticketRepository;

  /**
   * Get analytics filtered by agent's team visibility:
   * tickets assigned to the team OR tickets in categories the team handles.
   * If teamId is null, returns empty analytics (agent without a team sees nothing).
   */
  public AnalyticsResponse getAnalyticsByTeam(UUID organizationId, UUID teamId, List<String> allowedCategories, TimePeriod range) {
    if (teamId == null) {
      return emptyAnalytics();
    }

    LocalDateTime startDate = calculateStartDate(range);
    List<Ticket> allTickets = ticketRepository.findByOrganizationId(organizationId);

    List<Ticket> filtered = allTickets.stream()
        .filter(t -> t.getCreatedAt().isAfter(startDate))
        .filter(t -> {
          boolean sameTeam = teamId.equals(t.getTeamId());
          boolean handlesCategory = t.getCategory() != null && allowedCategories.contains(t.getCategory());
          return sameTeam || handlesCategory;
        })
        .toList();

    return buildAnalyticsResponse(filtered);
  }

  public AnalyticsResponse getAnalytics(UUID organizationId, UUID agentId, TimePeriod range) {
    LocalDateTime startDate = calculateStartDate(range);

    List<Ticket> allTickets = ticketRepository.findByOrganizationId(organizationId);

    // Filter by Date and Agent
    List<Ticket> filtered = allTickets.stream()
        .filter(t -> t.getCreatedAt().isAfter(startDate))
        .filter(t -> agentId == null || (t.getAgentId() != null && t.getAgentId().equals(agentId)))
        .toList();

    return buildAnalyticsResponse(filtered);
  }

  private AnalyticsResponse buildAnalyticsResponse(List<Ticket> filtered) {
    long total = filtered.size();
    long resolved = filtered.stream()
        .filter(t -> t.getStatus() == TicketStatus.RESOLVED || t.getStatus() == TicketStatus.CLOSED).count();

    // Avg ART (Resolution Time)
    double avgResolutionMinutes = filtered.stream()
        .filter(t -> t.getResolvedAt() != null)
        .mapToLong(t -> Duration.between(t.getCreatedAt(), t.getResolvedAt()).toMinutes())
        .average().orElse(0);
    String avgArt = formatDuration(avgResolutionMinutes);

    // 2. Distributions
    Map<String, Long> statusDist = filtered.stream()
        .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));

    Map<String, Long> priorityDist = filtered.stream()
        .filter(t -> t.getPriority() != null)
        .collect(Collectors.groupingBy(t -> t.getPriority().name(), Collectors.counting()));

    Map<String, Long> sentimentDist = filtered.stream()
        .filter(t -> t.getSentiment() != null)
        .collect(Collectors.groupingBy(Ticket::getSentiment, Collectors.counting()));

    // 3. Key Insights
    long criticalCount = filtered.stream()
        .filter(t -> "CRITICAL".equals(t.getPriority() != null ? t.getPriority().name() : ""))
        .filter(t -> t.getStatus() == TicketStatus.OPEN)
        .count();

    return new AnalyticsResponse(
        "0 min", // Placeholder for FRT
        avgArt,
        0.0, // SLA Placeholder
        resolved,
        0.0, // CSAT Placeholder
        0.0, // FCR Placeholder
        Map.of("Negative -> Positive", 0L, "Positive -> Negative", 0L),
        0.0, // Suggestion Acceptance
        0.0, // Triage Accuracy
        statusDist,
        priorityDist,
        sentimentDist,
        criticalCount,
        total > 0 ? (double) resolved / total * 100 : 0);
  }

  private AnalyticsResponse emptyAnalytics() {
    return new AnalyticsResponse(
        "0 min", "0 min", 0.0, 0L, 0.0, 0.0,
        Map.of(), 0.0, 0.0,
        Map.of(), Map.of(), Map.of(), 0L, 0.0);
  }

  private LocalDateTime calculateStartDate(TimePeriod range) {
    LocalDateTime now = LocalDateTime.now();
    return switch (range) {
      case WEEK -> now.minusWeeks(1);
      case MONTH -> now.minusMonths(1);
      case THREE_MONTHS, QUARTER -> now.minusMonths(3);
      case SEMESTER -> now.minusMonths(6);
      case YEAR -> now.minusYears(1);
    };
  }

  private String formatDuration(double minutes) {
    if (minutes < 60) {
      return (int) minutes + " min";
    }
    return (int) (minutes / 60) + "h";
  }
}
