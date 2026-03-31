-- box-service consolidated schema (final: role_id BIGINT, all columns inline)

CREATE TABLE IF NOT EXISTS box_state (
    role_id            BIGINT      NOT NULL,
    box_level          INT         NOT NULL DEFAULT 1,
    box_buy_times      INT         NOT NULL DEFAULT 0,
    level_up_end_epoch BIGINT      NOT NULL DEFAULT 0,
    level_fetch_flag   INT         NOT NULL DEFAULT 0,
    open_box_total     INT         NOT NULL DEFAULT 0,
    last_open_is_five  TINYINT     NOT NULL DEFAULT 0,
    pending_json       TEXT        NULL,
    shi_zhuang_num     INT         NOT NULL DEFAULT 0,
    arena_item_num     INT         NOT NULL DEFAULT 0,
    daily_ymd          VARCHAR(16) NULL,
    last_open_epoch    BIGINT      NOT NULL DEFAULT 0,
    updated_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS luck_state (
    role_id           BIGINT NOT NULL,
    start_epoch       BIGINT NOT NULL DEFAULT 0,
    end_epoch         BIGINT NOT NULL DEFAULT 0,
    receive_bitmap    BIGINT NOT NULL DEFAULT 0,
    snapshot_open_cnt INT    NOT NULL DEFAULT 0,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS box_setting (
    role_id               BIGINT NOT NULL,
    equip_eqality         INT    NOT NULL DEFAULT 0,
    open_five_mark        INT    NOT NULL DEFAULT 0,
    equip_cap_mark        INT    NOT NULL DEFAULT 1,
    equip_sell_mark       INT    NOT NULL DEFAULT 0,
    condition_first1      INT    NOT NULL DEFAULT 0,
    condition_first2      INT    NOT NULL DEFAULT 0,
    condition_second1     INT    NOT NULL DEFAULT 0,
    condition_second2     INT    NOT NULL DEFAULT 0,
    condition_first_mark  INT    NOT NULL DEFAULT 0,
    condition_second_mark INT    NOT NULL DEFAULT 0,
    retain_mark           INT    NOT NULL DEFAULT 0,
    challenge_mark        INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
