-- =============================================================
-- CIAP — Project Management (Module 6)
-- Projects spawned from a billed/assigned consultancy request,
-- with tasks and milestones.
-- =============================================================

CREATE TABLE projects (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_code       VARCHAR(30)  NOT NULL,
    request_id         BIGINT       NOT NULL,
    name               VARCHAR(200) NOT NULL,
    description        TEXT,
    status             VARCHAR(20)  NOT NULL,
    progress_percent   INT          NOT NULL DEFAULT 0,
    lead_consultant_id BIGINT,
    planned_start_date DATE,
    planned_end_date   DATE,
    actual_start_date  DATE,
    actual_end_date    DATE,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6),
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    CONSTRAINT uq_project_code UNIQUE (project_code),
    CONSTRAINT uq_project_request UNIQUE (request_id),
    CONSTRAINT fk_project_request FOREIGN KEY (request_id) REFERENCES consultancy_requests(id),
    CONSTRAINT fk_project_lead FOREIGN KEY (lead_consultant_id) REFERENCES consultants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE project_tasks (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id    BIGINT       NOT NULL,
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    status        VARCHAR(20)  NOT NULL,
    assignee_id   BIGINT,
    due_date      DATE,
    completed_at  DATETIME(6),
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6),
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES consultants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_task_project ON project_tasks(project_id);

CREATE TABLE project_milestones (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id    BIGINT       NOT NULL,
    title         VARCHAR(200) NOT NULL,
    due_date      DATE,
    achieved      BOOLEAN      NOT NULL DEFAULT FALSE,
    achieved_date DATE,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6),
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    CONSTRAINT fk_milestone_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_milestone_project ON project_milestones(project_id);
