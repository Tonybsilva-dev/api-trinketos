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

  @PostMapping("/refine")
  @Operation(summary = "Refine ticket description using AI")
  public ResponseEntity<Map<String, String>> refine(@RequestBody RefineRequest request) {
    String refined = ticketAIService.refineDescription(request.draft());
    return ResponseEntity.ok(Map.of("refined", refined));
  }
}
