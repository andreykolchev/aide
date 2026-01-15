package com.aide.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record SearchContentRequest(
        @NotBlank String query,
        @NotBlank String project
) {
}