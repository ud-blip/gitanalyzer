package com.blipaster.gitanalyzer.repository;
import com.blipaster.gitanalyzer.entity.AnalysisTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisTaskRepository extends JpaRepository<AnalysisTask, Long> {
}
