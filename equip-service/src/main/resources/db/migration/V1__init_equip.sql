-- equip-service consolidated schema (final: BIGINT role_id)

CREATE TABLE IF NOT EXISTS equip_slot (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_id    BIGINT NOT NULL,
  equip_type INT    NOT NULL,
  item_id    INT    NOT NULL,
  hp         INT    DEFAULT 0,
  attack     INT    DEFAULT 0,
  defend     INT    DEFAULT 0,
  speed      INT    DEFAULT 0,
  attr_type1  INT   DEFAULT 0,
  attr_value1 INT   DEFAULT 0,
  attr_type2  INT   DEFAULT 0,
  attr_value2 INT   DEFAULT 0,
  version    BIGINT NOT NULL DEFAULT 0,
  updated_at BIGINT NOT NULL,
  UNIQUE KEY uk_role_type (role_id, equip_type),
  KEY idx_role (role_id)
);

CREATE TABLE IF NOT EXISTS equip_fumo (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_id    BIGINT NOT NULL,
  equip_type INT    NOT NULL,
  level      INT    NOT NULL,
  exp        INT    NOT NULL,
  end_time   BIGINT NOT NULL,
  version    BIGINT NOT NULL DEFAULT 0,
  updated_at BIGINT NOT NULL,
  UNIQUE KEY uk_role_type (role_id, equip_type)
);