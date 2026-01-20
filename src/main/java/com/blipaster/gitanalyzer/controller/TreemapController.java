package com.blipaster.gitanalyzer.controller;

import com.blipaster.gitanalyzer.dto.*;
import com.blipaster.gitanalyzer.service.TreemapDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/treemap")
@RequiredArgsConstructor
@Tag(name = "Visualization", description = "Данные для отрисовки интерактивного Treemap")
public class TreemapController {

    private final TreemapDataService treemapDataService;

    @Operation(summary = "Данные по уровням", description = "Для Drill-down навигации (уровни папок)")
    @GetMapping("/{repoId}/level")
    public List<TreemapNodeDto> getLevel(
            @PathVariable Long repoId,
            @RequestParam(required = false) String path,
            @RequestParam(defaultValue = "20") int minChurn,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return treemapDataService.buildLevel(repoId, path, minChurn, limit);
    }

    @Operation(summary = "Все данные", description = "Плоский список всех путей и их Churn")
    @GetMapping("/{repoId}/all")
    public List<PathChurnDto> getAll(@PathVariable Long repoId) {
        return treemapDataService.getPathChurns(repoId, null);
    }

    @Operation(summary = "Поддерево", description = "Получить данные для конкретной директории")
    @GetMapping("/{repoId}/subtree")
    public List<PathChurnDto> getSubtree(
            @PathVariable Long repoId,
            @RequestParam String path
    ) {
        return treemapDataService.getPathChurns(repoId, path);
    }
}