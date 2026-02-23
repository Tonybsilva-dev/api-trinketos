package com.trinket.trinketos.dto;

import com.trinket.trinketos.model.AIInstructionType;

public record RefineRequest(String text, AIInstructionType instruction) {
}
