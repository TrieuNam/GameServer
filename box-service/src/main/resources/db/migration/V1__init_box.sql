CREATE TABLE box_state (
  role_id           VARCHAR(64) PRIMARY KEY,
  box_level         INT NOT NULL DEFAULT 1,
  box_buy_times     INT NOT NULL DEFAULT 0,
  level_up_end_epoch BIGINT NOT NULL DEFAULT 0,
  level_fetch_flag  INT NOT NULL DEFAULT 0,
  open_box_total    INT NOT NULL DEFAULT 0,
  last_open_is_five TINYINT NOT NULL DEFAULT 0,
  pending_json      TEXT NULL,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE luck_state (
  role_id           VARCHAR(64) PRIMARY KEY,
  start_epoch       BIGINT NOT NULL DEFAULT 0,
  end_epoch         BIGINT NOT NULL DEFAULT 0,
  receive_bitmap    BIGINT NOT NULL DEFAULT 0,
  snapshot_open_cnt INT NOT NULL DEFAULT 0,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;