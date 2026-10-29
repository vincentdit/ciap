-- Consultancy services under the existing four service lines, plus training fees.
-- Existing requests and training registrations stay valid: new columns are nullable
-- or default to a free, already-confirmed registration.

CREATE TABLE consultancy_categories (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(30)  NOT NULL,
    name             VARCHAR(120) NOT NULL,
    description      VARCHAR(1000),
    consultancy_type VARCHAR(30)  NOT NULL,
    display_order    INT          NOT NULL DEFAULT 0,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    CONSTRAINT uq_category_code UNIQUE (code),
    CONSTRAINT uq_category_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO consultancy_categories
    (code, name, description, consultancy_type, display_order, active, created_at)
VALUES
    ('INDUSTRIAL', 'Industrial Research',
     'Process, manufacturing, and laboratory work routed to Industrial Research.',
     'INDUSTRIAL', 1, TRUE, CURRENT_TIMESTAMP(6)),
    ('ENGINEERING', 'Engineering',
     'Plant design and equipment assessment routed to Engineering.',
     'ENGINEERING', 2, TRUE, CURRENT_TIMESTAMP(6)),
    ('ICT', 'ICT',
     'Information systems and digital work routed to ICT.',
     'ICT', 3, TRUE, CURRENT_TIMESTAMP(6)),
    ('ENVIRONMENTAL', 'Environmental',
     'Pollution and waste work routed to Environmental.',
     'ENVIRONMENTAL', 4, TRUE, CURRENT_TIMESTAMP(6));

CREATE TABLE consultancy_services (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id    BIGINT        NOT NULL,
    name           VARCHAR(200)  NOT NULL,
    description    TEXT,
    pricing_method VARCHAR(20)   NOT NULL,
    amount         DECIMAL(15,2),
    currency       VARCHAR(3)    NOT NULL DEFAULT 'TZS',
    unit_label     VARCHAR(80),
    conditions     VARCHAR(1000),
    published      BOOLEAN       NOT NULL DEFAULT FALSE,
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6),
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    CONSTRAINT uq_service_category_name UNIQUE (category_id, name),
    CONSTRAINT fk_service_category FOREIGN KEY (category_id) REFERENCES consultancy_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE service_price_versions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_id     BIGINT        NOT NULL,
    pricing_method VARCHAR(20)   NOT NULL,
    amount         DECIMAL(15,2),
    currency       VARCHAR(3)    NOT NULL,
    unit_label     VARCHAR(80),
    conditions     VARCHAR(1000),
    note           VARCHAR(500),
    changed_by     VARCHAR(100)  NOT NULL,
    changed_at     DATETIME(6)   NOT NULL,
    CONSTRAINT fk_price_service FOREIGN KEY (service_id) REFERENCES consultancy_services(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_price_service ON service_price_versions(service_id);

ALTER TABLE consultancy_requests
    ADD COLUMN service_id BIGINT NULL,
    ADD COLUMN service_name VARCHAR(200) NULL,
    ADD COLUMN pricing_method VARCHAR(20) NULL,
    ADD COLUMN snapshot_amount DECIMAL(15,2) NULL,
    ADD COLUMN snapshot_currency VARCHAR(3) NULL,
    ADD COLUMN price_version_id BIGINT NULL,
    ADD COLUMN quoted_quantity DECIMAL(12,2) NULL,
    ADD CONSTRAINT fk_req_service FOREIGN KEY (service_id) REFERENCES consultancy_services(id);

ALTER TABLE training_events
    ADD COLUMN fee_kind VARCHAR(10) NOT NULL DEFAULT 'FREE',
    ADD COLUMN fee_amount DECIMAL(15,2) NULL,
    ADD COLUMN fee_currency VARCHAR(3) NOT NULL DEFAULT 'TZS',
    ADD COLUMN registration_opens_at DATETIME(6) NULL,
    ADD COLUMN registration_closes_at DATETIME(6) NULL,
    ADD COLUMN objectives VARCHAR(1000) NULL,
    ADD COLUMN audience VARCHAR(500) NULL,
    ADD COLUMN prerequisites VARCHAR(500) NULL,
    ADD COLUMN contact_email VARCHAR(160) NULL;

ALTER TABLE training_registrations
    ADD COLUMN fee_kind VARCHAR(10) NOT NULL DEFAULT 'FREE',
    ADD COLUMN fee_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN fee_currency VARCHAR(3) NOT NULL DEFAULT 'TZS',
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED';

CREATE TABLE training_payments (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    control_number   VARCHAR(20)   NOT NULL,
    registration_id  BIGINT        NOT NULL,
    amount           DECIMAL(15,2) NOT NULL,
    currency         VARCHAR(3)    NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    gepg_reference   VARCHAR(50),
    paid_at          DATETIME(6),
    created_at       DATETIME(6)   NOT NULL,
    updated_at       DATETIME(6),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    CONSTRAINT uq_training_control_number UNIQUE (control_number),
    CONSTRAINT fk_training_payment_reg FOREIGN KEY (registration_id) REFERENCES training_registrations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_training_payment_reg ON training_payments(registration_id);
