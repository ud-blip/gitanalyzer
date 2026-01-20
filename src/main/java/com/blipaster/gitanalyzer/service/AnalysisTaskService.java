package com.blipaster.gitanalyzer.service;

import com.blipaster.gitanalyzer.entity.AnalysisTask;
import com.blipaster.gitanalyzer.entity.Repository;
import com.blipaster.gitanalyzer.entity.TaskStatus;
import com.blipaster.gitanalyzer.repository.AnalysisTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisTaskService {
    private final AnalysisTaskRepository taskRepository;
    //private final GitScannerService gitScannerService;

    @Transactional
    public AnalysisTask createTask(Repository repository) {
        AnalysisTask task = AnalysisTask.builder()
                .repository(repository)
                .build();
        return taskRepository.save(task);
    }



    @Transactional
    public void updateStatus(Long taskId, TaskStatus status, int progress, String error) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus(status);
            task.setProgress(progress);
            if (error != null) task.setErrorMessage(error);
            taskRepository.save(task);
        });
    }

    @Transactional(readOnly = true)
    public Optional<AnalysisTask> getTask(Long id) {
        return taskRepository.findById(id);
    }
}