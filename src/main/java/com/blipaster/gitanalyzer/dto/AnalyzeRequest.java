package com.blipaster.gitanalyzer.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalyzeRequest(
        @NotBlank(message = "Git URL is required")
        String gitUrl
) {}
