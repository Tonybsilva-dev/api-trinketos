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
  private final com.trinket.trinketos.repository.CategoryRepository categoryRepository;

  public String processText(String text, com.trinket.trinketos.model.AIInstructionType instruction) {
    ChatClient chatClient = chatClientBuilder.build();

    String systemPrompt = """
        Reescreva o seguinte problema de suporte técnico para torná-lo profissional, estruturado e claro.
        Use o formato:
        Contexto: [Breve explicação]
        Problema: [O que não está funcionando]
        Impacto: [Como isso afeta o usuário]

        Mantenha um tom neutro e técnico.
        """;

    if (instruction == com.trinket.trinketos.model.AIInstructionType.SUMMARIZE) {
      systemPrompt = "Resuma o texto abaixo de forma concisa em um único parágrafo, focando nos pontos principais para um agente de suporte.";
    }

    // User requested Temperature 0.1 for better assertiveness
    return chatClient.prompt()
        .system(systemPrompt)
        .user("Texto original: " + text)
        .options(org.springframework.ai.google.genai.GoogleGenAiChatOptions.builder()
            .model("gemini-3-flash-preview")
            .temperature(0.1)
            .build())
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

    // Fetch existing categories
    java.util.List<String> categories = categoryRepository.findByOrganizationId(ticket.getOrganizationId())
        .stream().map(com.trinket.trinketos.model.Category::getName).toList();
    String categoriesStr = String.join(", ", categories);

    // Prompt for JSON analysis
    String systemPrompt = """
        Atue como um especialista em suporte técnico. Analise o ticket abaixo e retorne um JSON com:
        {
          "title": "(String: Um título profissional, curto e direto para o ticket)",
          "sentiment": "(String: Positivo, Neutro ou Frustrado/Urgente)",
          "priority": "(String: LOW, MEDIUM, HIGH, CRITICAL)",
          "category": "(String: Escolha uma das seguintes: [%s]. Se nenhuma se encaixar, sugira uma nova)",
          "diagnosis": "(Resumo técnico da provável causa - Max 2 linhas)",
          "suggested_solution": "(Passo a passo para o agente resolver)"
        }
        Retorne APENAS o JSON.
        """.formatted(categoriesStr);

    String response = chatClient.prompt()
        .system(systemPrompt)
        .user("Ticket: " + ticket.getTitle() + " - " + ticket.getDescription())
        .options(org.springframework.ai.google.genai.GoogleGenAiChatOptions.builder()
            .model("gemini-3-flash-preview")
            .temperature(0.1)
            .build())
        .call()
        .content();

    log.info("AI Analysis result: {}", response);

    try {
      // Removing Markdown code blocks (```json ... ```) if present
      String cleanJson = response.replaceAll("```json", "").replaceAll("```", "").trim();

      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(cleanJson);

      if (root.has("title"))
        ticket.setTitle(root.get("title").asText());
      if (root.has("sentiment"))
        ticket.setSentiment(root.get("sentiment").asText());
      if (root.has("category"))
        ticket.setCategory(root.get("category").asText());
      if (root.has("diagnosis"))
        ticket.setDiagnosis(root.get("diagnosis").asText());
      if (root.has("suggested_solution"))
        ticket.setSuggestedSolution(root.get("suggested_solution").asText());

      if (root.has("priority")) {
        String p = root.get("priority").asText().toUpperCase();
        try {
          ticket.setPriority(Priority.valueOf(p));
        } catch (IllegalArgumentException e) {
          log.warn("Could not parse priority: {}", p);
        }
      }

      ticketRepository.save(ticket);
    } catch (Exception e) {
      log.error("Error parsing AI response", e);
    }
  }
}
