package com.trinket.trinketos.service;

import com.trinket.trinketos.model.Priority;
import com.trinket.trinketos.model.Ticket;
import com.trinket.trinketos.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketAIService {

  private final ChatClient.Builder chatClientBuilder;
  private final TicketRepository ticketRepository;

  public String refineDescription(String draft) {
    ChatClient chatClient = chatClientBuilder.build();
    return chatClient.prompt()
        .user("Refine the following ticket description to be professional, detailed, and clear: " + draft)
        .call()
        .content();
  }

  @Async
  @Transactional
  public void analyzeTicket(UUID ticketId) {
    log.info("Starting AI analysis for ticket: {}", ticketId);
    Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
    if (ticket == null)
      return;

    ChatClient chatClient = chatClientBuilder.build();

    // We can ask for JSON or specific format, or just Ask 3 questions.
    // For simplicity, I'll ask for a structured string or handle loose parsing.
    // Prompt: "Analyze this ticket: [desc]. Return strictly in format: Sentiment:
    // [S]; Category: [C]; Priority: [P]."

    String response = chatClient.prompt()
        .system(
            "You are a helpful support assistant. Analyze the ticket content. Return the Sentiment (Frustrated, Neutral, Happy), Category (Bug, Finance, Support, Feature), and Priority (LOW, MEDIUM, HIGH, CRITICAL). Format: Sentiment: X; Category: Y; Priority: Z.")
        .user("Title: " + ticket.getTitle() + "\nDescription: " + ticket.getDescription())
        .call()
        .content();

    log.info("AI Analysis result: {}", response);

    // Simple parsing logic (robustness would require JSON mode or better parsing)
    try {
      String[] parts = response.split(";");
      for (String part : parts) {
        String[] kv = part.trim().split(":");
        if (kv.length < 2)
          continue;
        String key = kv[0].trim().toLowerCase();
        String value = kv[1].trim();

        if (key.contains("sentiment")) {
          ticket.setSentiment(value);
        } else if (key.contains("category")) {
          ticket.setCategory(value);
        } else if (key.contains("priority")) {
          try {
            ticket.setPriority(Priority.valueOf(value.toUpperCase()));
          } catch (IllegalArgumentException e) {
            // Keep default or fallback
            log.warn("Could not parse priority: {}", value);
          }
        }
      }
      ticketRepository.save(ticket);
    } catch (Exception e) {
      log.error("Error parsing AI response", e);
    }
  }
}
