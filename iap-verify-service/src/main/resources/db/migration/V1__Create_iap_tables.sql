-- iap-verify-service consolidated schema (final: user_id VARCHAR(64))

CREATE TABLE purchases (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id             VARCHAR(64) NOT NULL,
    platform            VARCHAR(20) NOT NULL COMMENT 'GOOGLE, APPLE',
    product_id          VARCHAR(100) NOT NULL,
    purchase_token      VARCHAR(500) NOT NULL UNIQUE,
    order_id            VARCHAR(100),
    package_name        VARCHAR(100),
    purchase_time       DATETIME,
    purchase_state      VARCHAR(20)  COMMENT 'PURCHASED, PENDING, REFUNDED, CANCELLED',
    verification_status VARCHAR(20) NOT NULL COMMENT 'PENDING, VERIFIED, FAILED, FRAUD',
    verification_time   DATETIME,
    consumption_status  VARCHAR(20) NOT NULL COMMENT 'UNCONSUMED, CONSUMED, EXPIRED',
    consumed_time       DATETIME,
    amount              BIGINT       COMMENT 'Price in cents',
    currency            VARCHAR(10),
    raw_response        TEXT,
    error_message       TEXT,
    fraud_score         INT          COMMENT '0-100',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id            (user_id),
    INDEX idx_platform           (platform),
    INDEX idx_verification_status (verification_status),
    INDEX idx_consumption_status  (consumption_status),
    INDEX idx_fraud_score         (fraud_score),
    INDEX idx_created_at          (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IAP Purchase records';

CREATE TABLE refund_requests (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_id      BIGINT NOT NULL,
    user_id          VARCHAR(64) NOT NULL,
    reason           TEXT,
    status           VARCHAR(20) NOT NULL COMMENT 'PENDING, APPROVED, REJECTED, PROCESSED',
    refund_amount    BIGINT,
    processed_by     BIGINT COMMENT 'GM ID',
    processed_at     DATETIME,
    resolution_notes TEXT,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_purchase_id (purchase_id),
    INDEX idx_user_id     (user_id),
    INDEX idx_status      (status),
    INDEX idx_created_at  (created_at),
    FOREIGN KEY (purchase_id) REFERENCES purchases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Refund requests';
