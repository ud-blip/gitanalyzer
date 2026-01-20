package com.blipaster.gitanalyzer.dto;

import java.time.Instant;

public record RepositoryDto(
        Long id,
        String url,
        Instant clonedAt
) {}