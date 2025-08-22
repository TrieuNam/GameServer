CREATE TABLE role (
  role_id        VARCHAR(26) PRIMARY KEY,
  user_id        VARCHAR(36) NOT NULL,
  name           VARCHAR(64) NOT NULL,
  level          INT NOT NULL,
  exp            BIGINT NOT NULL, -- EXP trong cấp hiện tại
  hp             BIGINT NOT NULL,
  attack_value   BIGINT NOT NULL,
  defense_value  BIGINT NOT NULL,
  speed          INT    NOT NULL,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_name (user_id, name),
  KEY idx_user   (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Mở rộng nếu muốn key-value attr
CREATE TABLE role_attr (
  id       BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id  VARCHAR(26) NOT NULL,
  attr_key VARCHAR(64) NOT NULL,
  attr_val BIGINT      NOT NULL,
  UNIQUE KEY uk_role_attr (role_id, attr_key),
  CONSTRAINT fk_role_attr_role FOREIGN KEY (role_id) REFERENCES role(role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;