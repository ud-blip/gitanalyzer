package com.blipaster.gitanalyzer.repository;


import com.blipaster.gitanalyzer.entity.FileChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FileChangeRepository extends JpaRepository<FileChange, Long> {

    /**
     * Статистика для Bus Factor.
     * Оптимизировано: считаем изменения строк (churn), а не просто количество записей,
     * так как это точнее отражает вклад разработчика.
     */
    @Query(value = """
        SELECT c.developer_id, SUM(fc.additions + fc.deletions) AS total_churn
        FROM file_changes fc
        JOIN commits c ON fc.commit_id = c.id
        WHERE c.repository_id = :repoId
        GROUP BY c.developer_id
        ORDER BY total_churn DESC
    """, nativeQuery = true)
    List<Object[]> countChangesByDeveloper(@Param("repoId") Long repoId);

    /**
     * Поиск Hotspots. Native query здесь оправдан для производительности.
     */
    @Query(value = """
        SELECT fc.file_path,
               COUNT(*) AS changes_count,
               SUM(fc.additions + fc.deletions) AS total_churn
        FROM file_changes fc
        JOIN commits c ON fc.commit_id = c.id
        WHERE c.repository_id = :repoId
        GROUP BY fc.file_path
        ORDER BY total_churn DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findTopHotspots(@Param("repoId") Long repoId, @Param("limit") int limit);

    /**
     * Основной запрос для Treemap с фильтрацией по префиксу.
     * Используем JPQL для чистоты кода.
     */

    @Query("""
        SELECT fc.filePath, SUM(fc.additions + fc.deletions)
        FROM FileChange fc
        WHERE fc.commit.repository.id = :repoId
          AND (:prefix IS NULL OR :prefix = '' OR fc.filePath LIKE CONCAT(:prefix, '%'))
        GROUP BY fc.filePath
    """)
    List<Object[]> findPathsWithChurn(@Param("repoId") Long repoId, @Param("prefix") String prefix);


}
