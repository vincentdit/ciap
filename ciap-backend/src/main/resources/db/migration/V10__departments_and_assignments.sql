-- Department routing and Head of Department assignment.
-- Existing requests are attached by service line. They stay NO_HOD until a head is appointed.

INSERT INTO roles (name, description) VALUES
    ('HOD', 'Head of Department — reviews and assigns work in one department');

CREATE TABLE departments (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(120) NOT NULL,
    code         VARCHAR(40)  NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    hod_user_id  BIGINT       NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NULL,
    created_by   VARCHAR(100) NULL,
    updated_by   VARCHAR(100) NULL,
    CONSTRAINT uq_departments_code UNIQUE (code),
    CONSTRAINT uq_departments_hod UNIQUE (hod_user_id),
    CONSTRAINT fk_departments_hod FOREIGN KEY (hod_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE department_members (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id     BIGINT  NOT NULL,
    user_id           BIGINT  NOT NULL,
    can_receive_work  BOOLEAN NOT NULL DEFAULT TRUE,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NULL,
    created_by        VARCHAR(100) NULL,
    updated_by        VARCHAR(100) NULL,
    CONSTRAINT uq_department_member UNIQUE (department_id, user_id),
    CONSTRAINT fk_member_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE service_routes (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    consultancy_type  VARCHAR(30) NOT NULL,
    department_id     BIGINT      NOT NULL,
    active            BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NULL,
    created_by        VARCHAR(100) NULL,
    updated_by        VARCHAR(100) NULL,
    CONSTRAINT uq_service_route_type UNIQUE (consultancy_type),
    CONSTRAINT fk_service_route_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO departments (name, code, active, created_at) VALUES
    ('Industrial Research', 'INDUSTRIAL', TRUE, CURRENT_TIMESTAMP(6)),
    ('Engineering', 'ENGINEERING', TRUE, CURRENT_TIMESTAMP(6)),
    ('ICT', 'ICT', TRUE, CURRENT_TIMESTAMP(6)),
    ('Environmental', 'ENVIRONMENTAL', TRUE, CURRENT_TIMESTAMP(6));

INSERT INTO service_routes (consultancy_type, department_id, active, created_at)
SELECT 'INDUSTRIAL', id, TRUE, CURRENT_TIMESTAMP(6) FROM departments WHERE code = 'INDUSTRIAL';
INSERT INTO service_routes (consultancy_type, department_id, active, created_at)
SELECT 'ENGINEERING', id, TRUE, CURRENT_TIMESTAMP(6) FROM departments WHERE code = 'ENGINEERING';
INSERT INTO service_routes (consultancy_type, department_id, active, created_at)
SELECT 'ICT', id, TRUE, CURRENT_TIMESTAMP(6) FROM departments WHERE code = 'ICT';
INSERT INTO service_routes (consultancy_type, department_id, active, created_at)
SELECT 'ENVIRONMENTAL', id, TRUE, CURRENT_TIMESTAMP(6) FROM departments WHERE code = 'ENVIRONMENTAL';

ALTER TABLE consultancy_requests
    ADD COLUMN department_id BIGINT NULL,
    ADD COLUMN routing_status VARCHAR(20) NOT NULL DEFAULT 'UNMAPPED',
    ADD COLUMN assignment_deadline DATE NULL,
    ADD CONSTRAINT fk_req_department FOREIGN KEY (department_id) REFERENCES departments(id);

CREATE INDEX idx_req_department ON consultancy_requests(department_id);
CREATE INDEX idx_req_routing ON consultancy_requests(routing_status);

UPDATE consultancy_requests r
JOIN service_routes s ON s.consultancy_type = r.consultancy_type AND s.active = TRUE
JOIN departments d ON d.id = s.department_id AND d.active = TRUE
SET r.department_id = d.id,
    r.routing_status = 'NO_HOD';

CREATE TABLE request_assignments (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id              BIGINT       NOT NULL,
    department_id           BIGINT       NULL,
    consultant_id           BIGINT       NOT NULL,
    assigned_by             VARCHAR(150) NOT NULL,
    note                    VARCHAR(500) NULL,
    deadline                DATE         NULL,
    previous_consultant_id  BIGINT       NULL,
    reason                  VARCHAR(500) NULL,
    created_at              DATETIME(6)  NOT NULL,
    CONSTRAINT fk_assignment_request FOREIGN KEY (request_id) REFERENCES consultancy_requests(id),
    CONSTRAINT fk_assignment_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_assignment_consultant FOREIGN KEY (consultant_id) REFERENCES consultants(id),
    CONSTRAINT fk_assignment_previous FOREIGN KEY (previous_consultant_id) REFERENCES consultants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_assignment_request ON request_assignments(request_id);
