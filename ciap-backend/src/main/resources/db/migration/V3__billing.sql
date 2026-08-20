-- =============================================================
-- CIAP — Quotation, Contract & Billing (GePG) schema
-- =============================================================

CREATE TABLE quotations (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    quotation_no VARCHAR(30)  NOT NULL,
    request_id   BIGINT       NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    currency     VARCHAR(3)   NOT NULL DEFAULT 'TZS',
    subtotal     DECIMAL(15,2) NOT NULL DEFAULT 0,
    vat_rate     DECIMAL(5,4)  NOT NULL DEFAULT 0.18,
    vat_amount   DECIMAL(15,2) NOT NULL DEFAULT 0,
    total        DECIMAL(15,2) NOT NULL DEFAULT 0,
    valid_until  DATE,
    notes        VARCHAR(1000),
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    CONSTRAINT uq_quotation_no UNIQUE (quotation_no),
    CONSTRAINT fk_quotation_request FOREIGN KEY (request_id) REFERENCES consultancy_requests(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_quotation_request ON quotations(request_id);

CREATE TABLE quotation_items (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    quotation_id BIGINT        NOT NULL,
    description  VARCHAR(300)  NOT NULL,
    quantity     DECIMAL(12,2) NOT NULL,
    unit_price   DECIMAL(15,2) NOT NULL,
    line_total   DECIMAL(15,2) NOT NULL,
    CONSTRAINT fk_item_quotation FOREIGN KEY (quotation_id) REFERENCES quotations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_item_quotation ON quotation_items(quotation_id);

CREATE TABLE contracts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_no     VARCHAR(30)  NOT NULL,
    request_id      BIGINT       NOT NULL,
    quotation_id    BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    contract_value  DECIMAL(15,2) NOT NULL,
    currency        VARCHAR(3)   NOT NULL DEFAULT 'TZS',
    start_date      DATE,
    end_date        DATE,
    signed_by_client BOOLEAN     NOT NULL DEFAULT FALSE,
    signed_by_tirdo  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    CONSTRAINT uq_contract_no UNIQUE (contract_no),
    CONSTRAINT fk_contract_request FOREIGN KEY (request_id) REFERENCES consultancy_requests(id),
    CONSTRAINT fk_contract_quotation FOREIGN KEY (quotation_id) REFERENCES quotations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payments (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    control_number VARCHAR(20)  NOT NULL,
    request_id     BIGINT       NOT NULL,
    quotation_id   BIGINT,
    amount         DECIMAL(15,2) NOT NULL,
    currency       VARCHAR(3)   NOT NULL DEFAULT 'TZS',
    status         VARCHAR(20)  NOT NULL,
    gepg_reference VARCHAR(50),
    paid_at        DATETIME(6),
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6),
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    CONSTRAINT uq_payment_control_number UNIQUE (control_number),
    CONSTRAINT fk_payment_request FOREIGN KEY (request_id) REFERENCES consultancy_requests(id),
    CONSTRAINT fk_payment_quotation FOREIGN KEY (quotation_id) REFERENCES quotations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_payment_request ON payments(request_id);
