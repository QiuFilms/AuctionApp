-- Czyszczenie starych tabel
DROP TABLE IF EXISTS auction_history CASCADE;
DROP TABLE IF EXISTS auctions CASCADE;
DROP TABLE IF EXISTS items_user CASCADE;
DROP TABLE IF EXISTS items CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Tabela: USERS
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    wallet REAL NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    creation_date TIMESTAMP WITHOUT TIME ZONE
);

-- Tabela: ITEMS z polem type
CREATE TABLE IF NOT EXISTS items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    rarity VARCHAR(255),
    type VARCHAR(50),
    min_level INT
);

-- Tabela pośrednia
-- Tabela pośrednia USER ↔ ITEM
CREATE TABLE IF NOT EXISTS items_user (
    unique_item_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    on_auction BOOLEAN NOT NULL DEFAULT FALSE,  -- ← PRZENIESIONE TUTAJ
    CONSTRAINT fk_iu_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_iu_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- Tabela: AUCTIONS
CREATE TABLE IF NOT EXISTS auctions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    item_id BIGINT,
    time_duration INT,
    creation_date TIMESTAMP WITHOUT TIME ZONE,
    end_date TIMESTAMP WITHOUT TIME ZONE,
    price REAL NOT NULL
);

-- Tabela: AUCTION_HISTORY
CREATE TABLE IF NOT EXISTS auction_history (
    id BIGSERIAL PRIMARY KEY,
    auction_id BIGINT,
    event_date TIMESTAMP WITHOUT TIME ZONE,
    event_type VARCHAR(255),
    user_id BIGINT
);

-- Klucze obce
ALTER TABLE auctions ADD CONSTRAINT fk_auctions_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE;
ALTER TABLE auction_history ADD CONSTRAINT fk_history_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE;
ALTER TABLE auction_history ADD CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE auctions ADD CONSTRAINT fk_auctions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;