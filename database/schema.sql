-- ProManage schema
-- Run this script in PostgreSQL before starting the application.

CREATE SEQUENCE IF NOT EXISTS projects_id_seq START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS projects (
    id SERIAL PRIMARY KEY,
    project_code VARCHAR(20) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    deadline INT NOT NULL CHECK (deadline > -3650),
    revenue INT NOT NULL CHECK (revenue > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_week INT
);

CREATE TABLE IF NOT EXISTS weekly_revenue_history (
    week_no SERIAL PRIMARY KEY,
    total_revenue INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
