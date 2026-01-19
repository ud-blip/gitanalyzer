-- Если таблицы еще нет, создаем. Если есть — добавляем колонки.
CREATE TABLE IF NOT EXISTS file_changes (
                                            id BIGSERIAL PRIMARY KEY,
                                            commit_id BIGINT REFERENCES commits(id),
    file_path VARCHAR(1000) NOT NULL,
    change_type VARCHAR(20),
    additions INT DEFAULT 0,
    deletions INT DEFAULT 0
    );

CREATE INDEX IF NOT EXISTS idx_file_changes_commit_id ON file_changes(commit_id);
CREATE INDEX IF NOT EXISTS idx_file_changes_path ON file_changes(file_path);