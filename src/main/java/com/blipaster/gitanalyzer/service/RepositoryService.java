package com.blipaster.gitanalyzer.service;

import com.blipaster.gitanalyzer.entity.Commit;
import com.blipaster.gitanalyzer.entity.Developer;
import com.blipaster.gitanalyzer.entity.FileChange;
import com.blipaster.gitanalyzer.entity.Repository;
import com.blipaster.gitanalyzer.repository.CommitRepository;
import com.blipaster.gitanalyzer.repository.DeveloperRepository;
import com.blipaster.gitanalyzer.repository.FileChangeRepository;
import com.blipaster.gitanalyzer.repository.RepositoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RepositoryService {

    private final RepositoryRepository repoRepo;
    private final DeveloperRepository devRepo;
    private final CommitRepository commitRepo;
    private final FileChangeRepository fileChangeRepository;

    @Transactional
    public Repository getOrCreateRepository(String gitUrl) {
        return repoRepo.findByUrl(gitUrl)
                .orElseGet(() -> {
                    Repository newRepo = new Repository();
                    newRepo.setUrl(gitUrl);
                    return repoRepo.saveAndFlush(newRepo);
                });
    }

    @Transactional
    public Developer getOrCreateDeveloper(String email, String name) {
        return devRepo.findByEmail(email)
                .orElseGet(() -> {
                    Developer dev = new Developer();
                    dev.setEmail(email);
                    dev.setName(name);
                    return devRepo.save(dev);
                });
    }

    @Transactional
    public void saveCommitsBatch(List<Commit> commits) {
        commitRepo.saveAll(commits);
    }

    @Transactional
    public void markAsCloned(Long repoId) {
        repoRepo.findById(repoId).ifPresent(repo -> {
            repo.setClonedAt(Instant.now());
            repoRepo.save(repo);
        });
    }

    @Transactional
    public Commit saveSingleCommit(Commit commit) {
        return commitRepo.saveAndFlush(commit);
    }

    @Transactional
    public void saveFileChangesBatch(List<FileChange> changes) {
        fileChangeRepository.saveAll(changes);
    }

}
