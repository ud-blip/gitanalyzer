package com.blipaster.gitanalyzer.dto;

import java.util.List;

public record MetricSummaryDto(
        int healthScore,
        int busFactor,
        List<HotspotDto> hotspots,
        String burnoutAlert
) {}
