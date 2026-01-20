package com.blipaster.gitanalyzer.dto;

public record HotspotDto(
        String filePath,
        int churn,
        int changesCount,
        RiskLevel riskLevel
) {}

