-- =============================================================
-- CIAP — Production hardening: DB-backed reference sequences
-- Replaces in-memory AtomicLong generators so reference numbers
-- survive restarts and are safe across multiple app instances.
-- =============================================================

CREATE TABLE reference_sequences (
    seq_key     VARCHAR(40) PRIMARY KEY,
    next_value  BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO reference_sequences (seq_key, next_value) VALUES
    ('REQUEST',   1),
    ('QUOTATION', 1),
    ('CONTRACT',  1),
    ('PROJECT',   1);
