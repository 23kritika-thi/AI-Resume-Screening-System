-- =============================================================================
-- AI-Based Resume Screening and Candidate Ranking System
-- Database Schema Definition
-- Target DBMS: MySQL 8.0+
-- =============================================================================

CREATE DATABASE IF NOT EXISTS resume_screening_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE resume_screening_db;

-- 1. Recruiter Accounts Table
CREATE TABLE IF NOT EXISTS recruiter_accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_recruiter_username (username),
    INDEX idx_recruiter_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Jobs Table
CREATE TABLE IF NOT EXISTS jobs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    recruiter_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    department VARCHAR(100),
    description TEXT NOT NULL,
    required_skills TEXT,
    preferred_skills TEXT,
    min_experience_years INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_jobs_recruiter FOREIGN KEY (recruiter_id) 
        REFERENCES recruiter_accounts(id) ON DELETE CASCADE,
    INDEX idx_jobs_status (status),
    INDEX idx_jobs_recruiter (recruiter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Candidates Table
CREATE TABLE IF NOT EXISTS candidates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(30),
    linkedin_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_candidates_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Resumes Table
CREATE TABLE IF NOT EXISTS resumes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    candidate_id INT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    extracted_text LONGTEXT,
    file_hash VARCHAR(64),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resumes_candidate FOREIGN KEY (candidate_id) 
        REFERENCES candidates(id) ON DELETE CASCADE,
    INDEX idx_resumes_candidate (candidate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Screening Results Table
CREATE TABLE IF NOT EXISTS screening_results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    job_id INT NOT NULL,
    candidate_id INT NOT NULL,
    resume_id INT NOT NULL,
    match_score DECIMAL(5,2) NOT NULL,
    required_skills_score DECIMAL(5,2) NOT NULL DEFAULT 0,
    preferred_skills_score DECIMAL(5,2) NOT NULL DEFAULT 0,
    experience_score DECIMAL(5,2) NOT NULL DEFAULT 0,
    text_similarity_score DECIMAL(5,2) NOT NULL DEFAULT 0,
    matched_skills TEXT,
    missing_skills TEXT,
    ranking INT,
    recommendation VARCHAR(50),
    screened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_screening_job FOREIGN KEY (job_id) 
        REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_screening_candidate FOREIGN KEY (candidate_id) 
        REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_screening_resume FOREIGN KEY (resume_id) 
        REFERENCES resumes(id) ON DELETE CASCADE,
    INDEX idx_screening_job_score (job_id, match_score DESC),
    INDEX idx_screening_candidate (candidate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Recruiter Notes Table
CREATE TABLE IF NOT EXISTS recruiter_notes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    screening_id INT NOT NULL,
    recruiter_id INT NOT NULL,
    note_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notes_screening FOREIGN KEY (screening_id) 
        REFERENCES screening_results(id) ON DELETE CASCADE,
    CONSTRAINT fk_notes_recruiter FOREIGN KEY (recruiter_id) 
        REFERENCES recruiter_accounts(id) ON DELETE CASCADE,
    INDEX idx_notes_screening (screening_id),
    INDEX idx_notes_recruiter (recruiter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
