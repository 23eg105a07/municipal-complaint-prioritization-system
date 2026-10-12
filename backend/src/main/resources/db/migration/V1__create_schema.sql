-- =============================================================================
-- V1: Core schema for Municipal Complaint Prioritization System
-- =============================================================================

-- 1. Departments
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    head_officer_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Users (CITIZEN, OFFICER, ADMIN)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(32) NOT NULL,
    phone VARCHAR(30),
    department_id BIGINT REFERENCES departments(id) ON DELETE SET NULL,
    zone_id INTEGER,
    available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_officer_attributes CHECK (
        role <> 'OFFICER' OR (department_id IS NOT NULL AND zone_id IS NOT NULL AND available IS NOT NULL)
    )
);

-- Circular FK for head of department
ALTER TABLE departments
    ADD CONSTRAINT fk_departments_head_officer
    FOREIGN KEY (head_officer_id) REFERENCES users(id) ON DELETE SET NULL;

-- 3. Complaints
CREATE TABLE complaints (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    citizen_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    department_id BIGINT NOT NULL REFERENCES departments(id) ON DELETE RESTRICT,
    category VARCHAR(50) NOT NULL,
    severity INTEGER NOT NULL CHECK (severity >= 1 AND severity <= 5),
    affected_count INTEGER NOT NULL DEFAULT 1 CHECK (affected_count >= 1),
    priority_score NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(32) NOT NULL DEFAULT 'REPORTED',
    zone_id INTEGER NOT NULL,
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    address VARCHAR(255),
    photo_url VARCHAR(500),
    is_escalated BOOLEAN NOT NULL DEFAULT FALSE,
    escalated_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_complaints_status ON complaints(status);
CREATE INDEX idx_complaints_priority ON complaints(priority_score DESC, created_at ASC, id ASC);
CREATE INDEX idx_complaints_zone ON complaints(zone_id);
CREATE INDEX idx_complaints_dept ON complaints(department_id);
CREATE INDEX idx_complaints_citizen ON complaints(citizen_id);

-- 4. Complaint Upvotes
CREATE TABLE complaint_upvotes (
    id BIGSERIAL PRIMARY KEY,
    complaint_id BIGINT NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    citizen_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_complaint_citizen_upvote UNIQUE (complaint_id, citizen_id)
);

CREATE INDEX idx_upvotes_complaint ON complaint_upvotes(complaint_id);

-- 5. Assignments
CREATE TABLE assignments (
    id BIGSERIAL PRIMARY KEY,
    complaint_id BIGINT NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    officer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    assigned_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_assignments_complaint_active ON assignments(complaint_id, active);
CREATE INDEX idx_assignments_officer_active ON assignments(officer_id, active);

-- 6. Status Transition Logs
CREATE TABLE complaint_status_logs (
    id BIGSERIAL PRIMARY KEY,
    complaint_id BIGINT NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    changed_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    remarks TEXT,
    proof_photo_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_status_logs_complaint ON complaint_status_logs(complaint_id);

-- 7. Citizen Feedback
CREATE TABLE citizen_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    complaint_id BIGINT NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    citizen_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comments TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_complaint_feedback UNIQUE (complaint_id)
);
