-- =============================================================
-- CIAP — Initial schema (Phase 1 modules)
-- Engine: InnoDB / utf8mb4
-- =============================================================

CREATE TABLE roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(30)  NOT NULL UNIQUE,
    description VARCHAR(200)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    phone         VARCHAR(30),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    mfa_enabled   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6),
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE clients (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name      VARCHAR(200) NOT NULL,
    tin               VARCHAR(20),
    client_type       VARCHAR(30)  NOT NULL,
    industry_category VARCHAR(40)  NOT NULL,
    contact_person    VARCHAR(150),
    phone             VARCHAR(30),
    email             VARCHAR(150),
    region            VARCHAR(100),
    physical_address  VARCHAR(300),
    user_id           BIGINT,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    CONSTRAINT uq_clients_tin UNIQUE (tin),
    CONSTRAINT uq_clients_user UNIQUE (user_id),
    CONSTRAINT fk_clients_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE consultants (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name             VARCHAR(150) NOT NULL,
    category              VARCHAR(40)  NOT NULL,
    skills                VARCHAR(500),
    years_experience      INT,
    availability_status   VARCHAR(30)  NOT NULL,
    active_assignments    INT          NOT NULL DEFAULT 0,
    performance_rating    DOUBLE       DEFAULT 0,
    billing_rate_per_hour DECIMAL(12,2),
    user_id               BIGINT,
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    CONSTRAINT uq_consultants_user UNIQUE (user_id),
    CONSTRAINT fk_consultants_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE consultancy_requests (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_no           VARCHAR(30)  NOT NULL,
    client_id              BIGINT       NOT NULL,
    consultancy_type       VARCHAR(30)  NOT NULL,
    title                  VARCHAR(200) NOT NULL,
    description            TEXT,
    expected_start_date    DATE,
    status                 VARCHAR(30)  NOT NULL,
    assigned_consultant_id BIGINT,
    review_notes           TEXT,
    created_at             DATETIME(6)  NOT NULL,
    updated_at             DATETIME(6),
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100),
    CONSTRAINT uq_req_reference UNIQUE (reference_no),
    CONSTRAINT fk_req_client FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT fk_req_consultant FOREIGN KEY (assigned_consultant_id) REFERENCES consultants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_req_status ON consultancy_requests(status);
CREATE INDEX idx_clients_industry ON clients(industry_category);
CREATE INDEX idx_consultants_category ON consultants(category);
