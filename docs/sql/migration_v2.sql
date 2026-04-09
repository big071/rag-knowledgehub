USE rag_knowledgehub;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='sys_user' AND column_name='enabled') = 0,
'ALTER TABLE sys_user ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER nickname',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='kb_document' AND column_name='source_hash') = 0,
'ALTER TABLE kb_document ADD COLUMN source_hash VARCHAR(64) NULL AFTER storage_path',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='kb_document' AND column_name='review_status') = 0,
'ALTER TABLE kb_document ADD COLUMN review_status VARCHAR(20) DEFAULT ''PENDING'' AFTER parse_status',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='kb_document' AND column_name='reviewed_by') = 0,
'ALTER TABLE kb_document ADD COLUMN reviewed_by BIGINT NULL AFTER review_status',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='kb_document' AND column_name='reviewed_at') = 0,
'ALTER TABLE kb_document ADD COLUMN reviewed_at DATETIME NULL AFTER reviewed_by',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='kb_document' AND column_name='review_comment') = 0,
'ALTER TABLE kb_document ADD COLUMN review_comment VARCHAR(500) NULL AFTER reviewed_at',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='kb_document' AND column_name='version_no') = 0,
'ALTER TABLE kb_document ADD COLUMN version_no INT NOT NULL DEFAULT 1 AFTER review_comment',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='kb_document' AND column_name='parent_document_id') = 0,
'ALTER TABLE kb_document ADD COLUMN parent_document_id BIGINT NULL AFTER version_no',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='kb_document' AND column_name='latest') = 0,
'ALTER TABLE kb_document ADD COLUMN latest TINYINT(1) NOT NULL DEFAULT 1 AFTER parent_document_id',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='kb_document' AND column_name='sensitive_hit') = 0,
'ALTER TABLE kb_document ADD COLUMN sensitive_hit TINYINT(1) NOT NULL DEFAULT 0 AFTER latest',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='kb_document' AND column_name='sensitive_tip') = 0,
'ALTER TABLE kb_document ADD COLUMN sensitive_tip VARCHAR(500) NULL AFTER sensitive_hit',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name='kb_document' AND index_name='idx_doc_review') = 0,
'ALTER TABLE kb_document ADD INDEX idx_doc_review (review_status)',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name='kb_document' AND index_name='idx_doc_latest') = 0,
'ALTER TABLE kb_document ADD INDEX idx_doc_latest (latest)',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='document_chunk' AND column_name='content_hash') = 0,
'ALTER TABLE document_chunk ADD COLUMN content_hash VARCHAR(64) NOT NULL DEFAULT '''' AFTER content',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='document_chunk' AND column_name='vector_json') = 0,
'ALTER TABLE document_chunk ADD COLUMN vector_json LONGTEXT NULL AFTER content_hash',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name='document_chunk' AND index_name='idx_chunk_hash') = 0,
'ALTER TABLE document_chunk ADD INDEX idx_chunk_hash (content_hash)',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='qa_record' AND column_name='summary') = 0,
'ALTER TABLE qa_record ADD COLUMN summary VARCHAR(500) NULL AFTER answer',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='qa_record' AND column_name='conversation_id') = 0,
'ALTER TABLE qa_record ADD COLUMN conversation_id VARCHAR(64) NULL AFTER summary',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name='qa_record' AND index_name='idx_qa_conv') = 0,
'ALTER TABLE qa_record ADD INDEX idx_qa_conv (conversation_id)',
'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS document_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS document_tag_rel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doc_tag (document_id, tag_id),
    INDEX idx_rel_tag (tag_id),
    CONSTRAINT fk_rel_doc FOREIGN KEY (document_id) REFERENCES kb_document(id) ON DELETE CASCADE,
    CONSTRAINT fk_rel_tag FOREIGN KEY (tag_id) REFERENCES document_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(64) NOT NULL UNIQUE,
    config_value VARCHAR(500) NOT NULL,
    description VARCHAR(255),
    updated_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS operation_log (
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
