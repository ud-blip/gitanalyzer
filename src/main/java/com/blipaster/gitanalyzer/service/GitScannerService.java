package com.blipaster.gitanalyzer.service;


import com.blipaster.gitanalyzer.entity.*;
import com.blipaster.gitanalyzer.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitScannerService {

    private final RepositoryService repositoryService;
    private final RepositoryRepository repositoryRepository;
    private final AnalysisTaskService taskService;

    /**
     * Основной метод для асинхронного запуска с отслеживанием прогресса
     */
    @Async
    public void scanAndSaveWithProgress(String gitUrl, Long taskId) {
        Repository repository = repositoryService.getOrCreateRepository(gitUrl);
        File tempDir = null;

        try {
            tempDir = Files.createTempDirectory("gitinsight-").toFile();

            log.info("Task {}: Cloning {}...", taskId, gitUrl);
            try (Git git = Git.cloneRepository()
                    .setURI(gitUrl)
                    .setDirectory(tempDir)
                    .setBare(true)
                    .setNoCheckout(true) // Оптимизация: нужен только .git
                    .setTimeout(60)      // Таймаут 1 минута
                    .call()) {
                taskService.updateStatus(taskId, TaskStatus.RUNNING, 10, null);
            }

            processCommitsWithProgress(tempDir, repository, taskId);

            repositoryService.markAsCloned(repository.getId());

            taskService.updateStatus(taskId, TaskStatus.COMPLETED, 100, null);
            repositoryService.markAsCloned(repository.getId());

        } catch (Exception e) {
            log.error("Task {} failed: {}", taskId, gitUrl, e);
            throw new RuntimeException(e);
        } finally {
            cleanup(tempDir);
        }
    }

    private void processCommitsWithProgress(File gitDir, Repository repo, Long taskId) throws Exception {
        List<FileChange> fileChangeBatch = new ArrayList<>();
        try (var jgitRepo = new FileRepositoryBuilder().setGitDir(gitDir).build();
             var revWalk = new RevWalk(jgitRepo);
             var diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

            diffFormatter.setRepository(jgitRepo);
            diffFormatter.setDetectRenames(true);


            for (Ref ref : jgitRepo.getRefDatabase().getRefs()) {
                if (ref.getName().startsWith("refs/heads/")) {
                    revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
                }
            }

            List<Commit> batch = new ArrayList<>();
            Map<String, Developer> devCache = new HashMap<>();
            int count = 0;

            for (RevCommit rc : revWalk) {

                if (rc.getParentCount() > 1 || isBot(rc)) {
                    continue;
                }

                Developer dev = getDeveloperFromCacheOrDb(rc, devCache);

                // Сохраняем commit сразу
                Commit commit = repositoryService.saveSingleCommit(
                        createCommitEntity(rc, dev, repo)
                );

                // Diff analysis
                RevCommit parent = rc.getParentCount() > 0 ? rc.getParent(0) : null;
                if (parent != null) {
                    revWalk.parseHeaders(parent);
                }

                List<DiffEntry> diffs = diffFormatter.scan(
                        parent != null ? parent.getTree() : null,
                        rc.getTree()
                );

                for (DiffEntry entry : diffs) {
                    FileChange fc = FileChange.builder()
                            .commit(commit)
                            .filePath(entry.getNewPath())
                            .changeType(entry.getChangeType())
                            .build();

                    if (entry.getChangeType() != DiffEntry.ChangeType.DELETE) {
                        fillLineStats(diffFormatter, entry, fc);
                    }

                    fileChangeBatch.add(fc);

                    if (fileChangeBatch.size() >= 1000) {
                        repositoryService.saveFileChangesBatch(fileChangeBatch);
                        fileChangeBatch.clear();
                    }
                }
            }

            repositoryService.saveCommitsBatch(batch);
        }
    }

    private Developer getDeveloperFromCacheOrDb(RevCommit rc, Map<String, Developer> cache) {
        String email = rc.getAuthorIdent().getEmailAddress().toLowerCase().trim();
        String name = rc.getAuthorIdent().getName();
        return cache.computeIfAbsent(email, e -> repositoryService.getOrCreateDeveloper(e, name));
    }

    private void cleanup(File tempDir) {
        if (tempDir == null || !tempDir.exists()) return;
        try {
            Files.walk(tempDir.toPath())
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(java.nio.file.Path::toFile)
                    .forEach(File::delete);
            log.info("Cleanup successful.");
        } catch (Exception e) {
            log.warn("Cleanup failed: {}", e.getMessage());
        }
    }

    public java.util.Optional<Repository> findById(Long id) {
        return repositoryRepository.findById(id);
    }

    private boolean isBot(RevCommit rc) {
        String email = rc.getAuthorIdent().getEmailAddress().toLowerCase();
        String name = rc.getAuthorIdent().getName().toLowerCase();
        String msg = rc.getFullMessage().toLowerCase();

        return email.contains("noreply")
                || name.contains("bot")
                || msg.contains("bot")
                || msg.contains("merge pull request");
    }

    private Commit createCommitEntity(RevCommit rc, Developer dev, Repository repo) {
        Commit commit = new Commit();
        commit.setHash(rc.getName());
        commit.setMessage(rc.getFullMessage().trim());
        commit.setCommitDate(Instant.ofEpochSecond(rc.getCommitTime()));
        commit.setRepository(repo);
        commit.setDeveloper(dev);
        return commit;
    }

    private void fillLineStats(DiffFormatter df, DiffEntry entry, FileChange change) {
        try {
            FileHeader header = df.toFileHeader(entry);
            int adds = 0;
            int dels = 0;

            for (HunkHeader hunk : header.getHunks()) {
                for (Edit edit : hunk.toEditList()) {
                    adds += edit.getLengthB();
                    dels += edit.getLengthA();
                }
            }

            change.setAdditions(adds);
            change.setDeletions(dels);

        } catch (Exception e) {
            log.debug("Skipping binary or large file: {}", entry.getNewPath());
        }
    }


}