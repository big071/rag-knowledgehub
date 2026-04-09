CREATE DATABASE IF NOT EXISTS rag_knowledgehub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE rag_knowledgehub;

DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS document_tag_rel;
DROP TABLE IF EXISTS document_tag;
DROP TABLE IF EXISTS qa_record;
DROP TABLE IF EXISTS document_chunk;
DROP TABLE IF EXISTS kb_document;
DROP TABLE IF EXISTS knowledge_base;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    nickname VARCHAR(64) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(500),
    doc_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_kb_user (user_id),
    CONSTRAINT fk_kb_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE kb_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    knowledge_base_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT DEFAULT 0,
    storage_path VARCHAR(255) NOT NULL,
    source_hash VARCHAR(64),
    parse_status VARCHAR(20) DEFAULT 'PROCESSING',
    review_status VARCHAR(20) DEFAULT 'PENDING',
    reviewed_by BIGINT,
    reviewed_at DATETIME,
    review_comment VARCHAR(500),
    version_no INT NOT NULL DEFAULT 1,
    parent_document_id BIGINT,
    latest TINYINT(1) NOT NULL DEFAULT 1,
    sensitive_hit TINYINT(1) NOT NULL DEFAULT 0,
    sensitive_tip VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_doc_kb (knowledge_base_id),
    INDEX idx_doc_user (user_id),
    INDEX idx_doc_review (review_status),
    INDEX idx_doc_latest (latest),
    CONSTRAINT fk_doc_kb FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE document_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content LONGTEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    vector_json LONGTEXT,
    es_doc_id VARCHAR(128) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chunk_doc (document_id),
    INDEX idx_chunk_kb (knowledge_base_id),
    INDEX idx_chunk_hash (content_hash),
    CONSTRAINT fk_chunk_doc FOREIGN KEY (document_id) REFERENCES kb_document(id) ON DELETE CASCADE,
    CONSTRAINT fk_chunk_kb FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE qa_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    answer LONGTEXT NOT NULL,
    summary VARCHAR(500),
    conversation_id VARCHAR(64),
    source_json LONGTEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_qa_user (user_id),
    INDEX idx_qa_kb (knowledge_base_id),
    INDEX idx_qa_conv (conversation_id),
    CONSTRAINT fk_qa_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_qa_kb FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE document_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE document_tag_rel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doc_tag (document_id, tag_id),
    INDEX idx_rel_tag (tag_id),
    CONSTRAINT fk_rel_doc FOREIGN KEY (document_id) REFERENCES kb_document(id) ON DELETE CASCADE,
    CONSTRAINT fk_rel_tag FOREIGN KEY (tag_id) REFERENCES document_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(64) NOT NULL UNIQUE,
    config_value VARCHAR(500) NOT NULL,
    description VARCHAR(255),
    updated_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(64),
    role VARCHAR(20),
    action VARCHAR(120) NOT NULL,
    method VARCHAR(10),
    request_uri VARCHAR(255),
    ip VARCHAR(64),
    success TINYINT(1) NOT NULL DEFAULT 1,
    detail VARCHAR(1000),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_user (user_id),
    INDEX idx_log_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
