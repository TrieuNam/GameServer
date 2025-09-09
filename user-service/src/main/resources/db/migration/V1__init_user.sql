CREATE TABLE IF NOT EXISTS users (
                                     user_id   VARCHAR(36)  NOT NULL,
    username  VARCHAR(64)  NOT NULL,
    account   VARCHAR(64)  NOT NULL,
    pass_hash VARCHAR(100) NOT NULL,
    status    VARCHAR(16)  NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_account UNIQUE (account)
    ) ENGINE=InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;