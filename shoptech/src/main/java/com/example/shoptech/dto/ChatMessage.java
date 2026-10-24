package com.example.shoptech.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatMessage(
        @NotBlank
        @Pattern(regexp = "user|model")
        String role,

        @NotBlank
        @Size(max = 1000)
        String text
) {
}
