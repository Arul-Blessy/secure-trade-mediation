CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    reputation    NUMERIC(3,1) NOT NULL DEFAULT 10.0,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wallets (
    user_id         BIGINT PRIMARY KEY REFERENCES users(id),
    regular_balance NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    escrow_balance  NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    penalties_total NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    version         BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE products (
    id         BIGSERIAL PRIMARY KEY,
    seller_id  BIGINT        NOT NULL REFERENCES users(id),
    name       VARCHAR(150)  NOT NULL,
    price      NUMERIC(12,2) NOT NULL,
    image_url  VARCHAR(500),
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active     BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE orders (
    id                  BIGSERIAL PRIMARY KEY,
    buyer_id            BIGINT        NOT NULL REFERENCES users(id),
    seller_id           BIGINT        NOT NULL REFERENCES users(id),
    product_id          BIGINT        NOT NULL REFERENCES products(id),
    price               NUMERIC(12,2) NOT NULL,
    payment_mode        VARCHAR(10)   NOT NULL,
    buyer_locked_amount NUMERIC(12,2) NOT NULL,
    seller_deposit      NUMERIC(12,2) NOT NULL,
    status              VARCHAR(30)   NOT NULL,
    seller_agreed       BOOLEAN       NOT NULL DEFAULT FALSE,
    agent_notified      BOOLEAN       NOT NULL DEFAULT FALSE,
    return_requested    BOOLEAN       NOT NULL DEFAULT FALSE,
    return_decision     VARCHAR(20),
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wallet_transactions (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT        NOT NULL REFERENCES users(id),
    order_id   BIGINT        REFERENCES orders(id),
    bucket     VARCHAR(10)   NOT NULL,
    amount     NUMERIC(12,2) NOT NULL,
    reason     VARCHAR(60)   NOT NULL,
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);