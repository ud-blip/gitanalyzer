package com.blipaster.gitanalyzer.controller;

import com.blipaster.gitanalyzer.dto.*;
import com.blipaster.gitanalyzer.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
@Tag(name = "Repositories", description = "Управление репозиториями и задачами анализа")
public class RepositoryController {

    private final GitScannerService gitScannerService;
    private final AnalysisTaskService analysisTaskService;
    private final RepositoryService repositoryService;

    @Operation(summary = "Запустить анализ", description = "Клонирует репозиторий и запускает фоновый процесс анализа")
    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeResponse> analyze(@Valid @RequestBody AnalyzeRequest request) {
        var repository = repositoryService.getOrCreateRepository(request.gitUrl());
        var task = analysisTaskService.createTask(repository);

        gitScannerService.scanAndSaveWithProgress(request.gitUrl(), task.getId());

        return ResponseEntity.accepted()
                .body(new AnalyzeResponse(task.getId(), "Analysis started"));
    }

    @Operation(summary = "Получить данные репозитория", description = "Возвращает метаданные репозитория по ID")
    @GetMapping("/{id}")
    public ResponseEntity<RepositoryDto> getById(@PathVariable Long id) {
        return gitScannerService.findById(id)
                .map(repo -> new RepositoryDto(repo.getId(), repo.getUrl(), repo.getClonedAt()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Статус задачи", description = "Проверить процент выполнения и текущее состояние анализа")
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<TaskStatusResponse> getTaskStatus(@PathVariable Long taskId) {
        return analysisTaskService.getTask(taskId)
                .map(task -> new TaskStatusResponse(
                        task.getId(),
                        task.getStatus().name(),
                        task.getProgress(),
                        task.getRepository().getUrl(),
                        task.getErrorMessage()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}