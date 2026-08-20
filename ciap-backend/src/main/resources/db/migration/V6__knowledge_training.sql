-- =============================================================
-- CIAP — Knowledge Portal (Module 9) & Training/Webinars (Module 10)
-- =============================================================

CREATE TABLE knowledge_articles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    category    VARCHAR(80),
    summary     VARCHAR(500),
    body        TEXT         NOT NULL,
    published   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_article_published ON knowledge_articles(published);

CREATE TABLE training_events (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    mode        VARCHAR(20)  NOT NULL,
    location    VARCHAR(300),
    starts_at   DATETIME(6)  NOT NULL,
    ends_at     DATETIME(6),
    capacity    INT          NOT NULL DEFAULT 0,
    published   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_event_published ON training_events(published);

CREATE TABLE training_registrations (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id         BIGINT       NOT NULL,
    registrant_email VARCHAR(160) NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    CONSTRAINT uq_reg_event_email UNIQUE (event_id, registrant_email),
    CONSTRAINT fk_reg_event FOREIGN KEY (event_id) REFERENCES training_events(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
