-- ===================================================================
-- Kafka Tabanlı Toplantı Özetleyici - Database Initialization
-- ===================================================================

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS toplanti_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE toplanti_db;

-- ===================================================================
-- Users Table
-- ===================================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    display_name VARCHAR(255),
    avatar_url VARCHAR(500),
    platform VARCHAR(50),
    platform_user_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_platform (platform, platform_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- Meetings Table
-- ===================================================================
CREATE TABLE IF NOT EXISTS meetings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_id VARCHAR(255) UNIQUE,
    title VARCHAR(500),
    description TEXT,
    platform VARCHAR(50) NOT NULL,
    channel_id VARCHAR(255),
    guild_id VARCHAR(255),
    host_user_id BIGINT,
    status VARCHAR(50) DEFAULT 'PENDING',
    scheduled_start TIMESTAMP NULL,
    scheduled_end TIMESTAMP NULL,
    actual_start TIMESTAMP NULL,
    actual_end TIMESTAMP NULL,
    duration_seconds INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (host_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_status (status),
    INDEX idx_platform (platform),
    INDEX idx_channel (channel_id),
    INDEX idx_scheduled_start (scheduled_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- Meeting Participants Table
-- ===================================================================
CREATE TABLE IF NOT EXISTS meeting_participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    user_id BIGINT,
    participant_name VARCHAR(255),
    platform_user_id VARCHAR(255),
    joined_at TIMESTAMP NULL,
    left_at TIMESTAMP NULL,
    duration_seconds INT,
    is_host BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_meeting (meeting_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- Audio Messages Table
-- ===================================================================
CREATE TABLE IF NOT EXISTS audio_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT,
    voice_session_id VARCHAR(255),
    platform VARCHAR(50) NOT NULL,
    channel_id VARCHAR(255),
    author VARCHAR(255),
    author_id VARCHAR(255),
    audio_url VARCHAR(1000),
    audio_file_path VARCHAR(1000),
    duration_seconds DECIMAL(10,2),
    file_size_bytes BIGINT,
    mime_type VARCHAR(100),
    transcription TEXT,
    transcription_status VARCHAR(50) DEFAULT 'PENDING',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE SET NULL,
    INDEX idx_meeting (meeting_id),
    INDEX idx_voice_session (voice_session_id),
    INDEX idx_channel (channel_id),
    INDEX idx_status (transcription_status),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- Transcriptions Table
-- ===================================================================
CREATE TABLE IF NOT EXISTS transcriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audio_message_id BIGINT,
    meeting_id BIGINT,
    full_text TEXT,
    language VARCHAR(10) DEFAULT 'tr',
    confidence_score DECIMAL(5,4),
    word_count INT,
    processing_time_ms BIGINT,
    ai_model VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (audio_message_id) REFERENCES audio_messages(id) ON DELETE CASCADE,
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE SET NULL,
    INDEX idx_audio_message (audio_message_id),
    INDEX idx_meeting (meeting_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- Transcription Segments Table
-- ===================================================================
CREATE TABLE IF NOT EXISTS transcription_segments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transcription_id BIGINT NOT NULL,
    speaker_name VARCHAR(255),
    speaker_id VARCHAR(255),
    text TEXT NOT NULL,
    start_time_ms BIGINT,
    end_time_ms BIGINT,
    confidence_score DECIMAL(5,4),
    sequence_order INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transcription_id) REFERENCES transcriptions(id) ON DELETE CASCADE,
    INDEX idx_transcription (transcription_id),
    INDEX idx_speaker (speaker_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- Tasks (Action Items) Table
-- ===================================================================
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(255) UNIQUE,
    meeting_id BIGINT NOT NULL,
    transcription_id BIGINT,
    channel_id VARCHAR(255),
    platform VARCHAR(50),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    assignee VARCHAR(255),
    assigned_to_name VARCHAR(255),
    assignee_id VARCHAR(255),
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    status VARCHAR(50) DEFAULT 'PENDING',
    due_date TIMESTAMP NULL,
    source_text TEXT,
    confidence_score DECIMAL(5,4),
    assignment_reason VARCHAR(500),
    processed_time TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
    FOREIGN KEY (transcription_id) REFERENCES transcriptions(id) ON DELETE SET NULL,
    INDEX idx_meeting (meeting_id),
    INDEX idx_transcription (transcription_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- Meeting Summaries Table
-- ===================================================================
CREATE TABLE IF NOT EXISTS meeting_summaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    channel_id VARCHAR(255),
    platform VARCHAR(50),
    title VARCHAR(500),
    summary TEXT,
    key_points TEXT,
    decisions TEXT,
    participants TEXT,
    duration_minutes BIGINT,
    meeting_date TIMESTAMP NULL,
    processed_time TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
    INDEX idx_meeting (meeting_id),
    INDEX idx_platform (platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- Voice Sessions Table
-- ===================================================================
CREATE TABLE IF NOT EXISTS voice_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) UNIQUE NOT NULL,
    meeting_id BIGINT,
    platform VARCHAR(50) NOT NULL,
    channel_id VARCHAR(255),
    guild_id VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    total_audio_count INT DEFAULT 0,
    total_duration_seconds INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE SET NULL,
    INDEX idx_session_id (session_id),
    INDEX idx_meeting (meeting_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- Kafka Outbox Table (for reliable event publishing)
-- ===================================================================
CREATE TABLE IF NOT EXISTS kafka_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    topic VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    retry_count INT DEFAULT 0,
    error_message TEXT,
    INDEX idx_status (status),
    INDEX idx_topic (topic),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- Insert Default Data
-- ===================================================================

-- Insert system user
INSERT IGNORE INTO users (id, username, email, display_name, is_active) VALUES
(1, 'system', 'system@toplanti.local', 'System', TRUE);

-- ===================================================================
-- DEMO KULLANICILAR
-- ===================================================================
-- Password: "password123" için bcrypt hash
-- Online tool: https://bcrypt-generator.com/ (rounds: 10)
INSERT IGNORE INTO users (id, username, email, password_hash, display_name, is_active) VALUES
(2, 'user', 'user@demo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Demo User', TRUE),
(3, 'manager', 'manager@demo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Demo Manager', TRUE),
(4, 'admin', 'admin@demo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Demo Admin', TRUE);

COMMIT;

