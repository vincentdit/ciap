-- Additive audit and version columns. Existing rows keep working:
-- quotations start at version 1, request lock versions start at 0.

ALTER TABLE quotations
    ADD COLUMN version_no INT NOT NULL DEFAULT 1,
    ADD COLUMN supersedes_id BIGINT NULL,
    ADD CONSTRAINT fk_quotations_supersedes FOREIGN KEY (supersedes_id) REFERENCES quotations (id);

ALTER TABLE payments
    ADD COLUMN confirmed_by VARCHAR(150) NULL,
    ADD COLUMN confirmation_note VARCHAR(500) NULL;

ALTER TABLE consultancy_requests
    ADD COLUMN lock_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE clients
    ADD COLUMN country VARCHAR(80) NULL;

CREATE TABLE request_status_events (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id  BIGINT       NOT NULL,
    from_status VARCHAR(40)  NULL,
    to_status   VARCHAR(40)  NOT NULL,
    actor       VARCHAR(150) NOT NULL,
    reason      VARCHAR(500) NULL,
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT fk_req_events_request FOREIGN KEY (request_id) REFERENCES consultancy_requests (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_req_events_request ON request_status_events (request_id);

CREATE TABLE app_notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_email VARCHAR(150) NOT NULL,
    message         VARCHAR(500) NOT NULL,
    read_flag       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)  NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_notifications_email ON app_notifications (recipient_email);
