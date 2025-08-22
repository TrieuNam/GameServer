CREATE TABLE IF NOT EXISTS user_account (
  id            VARCHAR(50)  NOT NULL PRIMARY KEY,
  username      VARCHAR(64)  NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  spid          VARCHAR(32)  NULL,
  external_id   VARCHAR(128) NULL,
  device        VARCHAR(32)  NULL,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_at TIMESTAMP    NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_spid_ext ON user_account (spid, external_id);