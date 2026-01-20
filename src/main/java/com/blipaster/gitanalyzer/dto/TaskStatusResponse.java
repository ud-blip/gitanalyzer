package com.blipaster.gitanalyzer.dto;

public record TaskStatusResponse(
        Long taskId,
        String status,
        int progress,
        String repositoryUrl,
        String errorMessage
) {}
