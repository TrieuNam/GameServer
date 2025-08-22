CREATE TABLE IF NOT EXISTS wallet_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id VARCHAR(64) NOT NULL,
  item_id BIGINT NOT NULL,
  balance BIGINT NOT NULL DEFAULT 0,
  updated_at BIGINT NOT NULL,
  ver BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_wallet_role_item UNIQUE (role_id, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wallet_ledger (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id VARCHAR(64) NOT NULL,
  item_id BIGINT NOT NULL,
  delta BIGINT NOT NULL,
  reason INT NULL,
  reason_type INT NULL,
  idem_key VARCHAR(100) NULL,
  created_at BIGINT NOT NULL,
  CONSTRAINT uk_wl_idem UNIQUE (idem_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_wl_role_time ON wallet_ledger(role_id, created_at);