package com.example.shoptech.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatRequest(
        @NotBlank
        @Size(max = 1000)
        String message,

        @Size(max = 10)
        List<@NotNull @Valid ChatMessage> history
) {
}
