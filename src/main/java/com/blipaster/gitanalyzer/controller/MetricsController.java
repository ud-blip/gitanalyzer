package com.blipaster.gitanalyzer.controller;

import com.blipaster.gitanalyzer.dto.*;
import com.blipaster.gitanalyzer.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "Вычисление аналитических показателей (Bus Factor, Hotspots)")
public class MetricsController {

    private final MetricsService metricsService;

    @Operation(summary = "Сводка по проекту", description = "Возвращает Bus Factor, Health Score и общую статистику")
    @GetMapping("/summary/{repoId}")
    public ResponseEntity<MetricSummaryDto> getSummary(@PathVariable Long repoId) {
        return ResponseEntity.ok(metricsService.calculateSummary(repoId));
    }

    @Operation(summary = "Опасные зоны (Hotspots)", description = "Список файлов с наибольшим Churn и риском багов")
    @GetMapping("/hotspots/{repoId}")
    public ResponseEntity<List<HotspotDto>> getHotspots(
            @PathVariable Long repoId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(metricsService.calculateHotspots(repoId, limit));
    }
}