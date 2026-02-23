package com.trinket.trinketos.dto;

import java.util.Map;

public record AnalyticsResponse(
    // 1. Operational Metrics
    String avgFrt, // First Response Time (formatted "0 min")
    String avgArt, // Resolution Time (formatted "0h")
    double slaRate,
    long resolvedCount,

    // 2. Quality Metrics
    double csatScore, // Placeholder for now
    double fcrRate, // First Contact Resolution
    Map<String, Long> sentimentShift, // "Negative -> Positive": 0

    // 3. AI Efficiency
    double suggestionAcceptanceRate,
    double triageAccuracy,

    // Charts Data
    Map<String, Long> ticketStatusDistribution,
    Map<String, Long> priorityDistribution,
    Map<String, Long> sentimentDistribution,

    // Key Insights
    long criticalIssuesCount,
    double overallResolutionRate) {
}
