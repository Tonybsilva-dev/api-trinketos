package com.trinket.trinketos.controller;

import com.trinket.trinketos.dto.RefineRequest;
import com.trinket.trinketos.service.TicketAIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Tools", description = "AI Helper tools")
public class AIController {

  private final TicketAIService ticketAIService;

  @PostMapping("/process")
  @Operation(summary = "Process text (Refine or Summarize) using AI", responses = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Text processed successfully"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<Map<String, String>> processText(@RequestBody RefineRequest request) {
    var instruction = request.instruction() != null ? request.instruction()
        : com.trinket.trinketos.model.AIInstructionType.REFINE;
    String result = ticketAIService.processText(request.text(), instruction);
    return ResponseEntity.ok(Map.of("result", result));
  }
}
