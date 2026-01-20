package com.blipaster.gitanalyzer.repository;

import com.blipaster.gitanalyzer.entity.Commit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CommitRepository extends JpaRepository<Commit, Long> {
    @Query("SELECT c.commitDate FROM Commit c WHERE c.repository.id = :repoId")
    List<Instant> findCommitDatesByRepoId(@Param("repoId") Long repoId);


    @Query(value = """
    SELECT 
      CAST(COUNT(CASE WHEN EXTRACT(ISODOW FROM commit_date) IN (6, 7) OR EXTRACT(HOUR FROM commit_date) NOT BETWEEN 9 AND 19 THEN 1 END) AS FLOAT) 
      / COUNT(*) * 100
    FROM commits 
    WHERE repository_id = :repoId
""", nativeQuery = true)
    Double calculateBurnoutPercentage(@Param("repoId") Long repoId);

}