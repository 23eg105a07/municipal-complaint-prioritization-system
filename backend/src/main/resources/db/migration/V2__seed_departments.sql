-- =============================================================================
-- V2: Seed initial municipal departments
-- =============================================================================

INSERT INTO departments (code, name, description)
VALUES
    ('ROADS', 'Roads & Infrastructure', 'City roads, potholes, pavements, bridges, and traffic signage'),
    ('WATER', 'Water Supply & Sanitation', 'Municipal water pipelines, leaks, sewage, and drainage systems'),
    ('ELECTRICAL', 'Electricity & Public Lighting', 'Streetlights, power line faults, and municipal electrical grid');
