-- Seed the five system roles (Module 1). The default ADMIN account is
-- provisioned in Java on first startup using the real password encoder.
INSERT INTO roles (name, description) VALUES
    ('ADMIN',   'System administrator — full access'),
    ('MANAGER', 'Consultancy manager — requests, assignment, projects'),
    ('EXPERT',  'Consultant/expert — execution and reporting'),
    ('FINANCE', 'Finance officer — billing, payments, revenue'),
    ('CLIENT',  'External client — submits and tracks requests');
