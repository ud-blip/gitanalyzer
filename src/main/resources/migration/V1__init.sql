CREATE TABLE developers (
                            id BIGSERIAL PRIMARY KEY,
                            email VARCHAR(255) UNIQUE NOT NULL,
                            name VARCHAR(255)
);

CREATE TABLE repositories (
                              id BIGSERIAL PRIMARY KEY,
                              url VARCHAR(500) UNIQUE NOT NULL,
                              cloned_at TIMESTAMP
);

CREATE TABLE analysis_tasks (
                                id BIGSERIAL PRIMARY KEY,
                                repository_id BIGINT REFERENCES repositories(id),
                                status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
                                progress INT DEFAULT 0,
                                error_message TEXT,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE commits (
                         id BIGSERIAL PRIMARY KEY,
                         hash VARCHAR(40) UNIQUE NOT NULL,
                         developer_id BIGINT REFERENCES developers(id),
                         repository_id BIGINT REFERENCES repositories(id),
                         commit_date TIMESTAMP NOT NULL,
                         message TEXT
);

CREATE TABLE file_changes (
                              id BIGSERIAL PRIMARY KEY,
                              commit_id BIGINT REFERENCES commits(id),
                              file_path VARCHAR(1000) NOT NULL,
                              change_type VARCHAR(20),  -- ADDED, MODIFIED, DELETED, RENAMED
                              additions INT DEFAULT 0,
                              deletions INT DEFAULT 0
);

-- Индексы для производительности
CREATE INDEX idx_file_changes_file_path ON file_changes(file_path);
CREATE INDEX idx_commits_repository_id ON commits(repository_id);
CREATE INDEX idx_commits_developer_id ON commits(developer_id);