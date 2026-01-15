package com.aide.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record AskRequest(
        @NotBlank String question,
        @NotBlank String project
) {
}