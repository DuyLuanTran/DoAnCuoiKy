BEGIN;
DO $$ BEGIN
 IF current_database()<>'shoptech_marketplace' THEN RAISE EXCEPTION 'Sandbox database required'; END IF;
 IF NOT EXISTS(SELECT 1 FROM marketplace_migrations WHERE version='V003') THEN RAISE EXCEPTION 'Apply V003 first'; END IF;
END $$;
-- DDL START: also used in fresh private PostgreSQL test schemas.
CREATE TABLE product_favorites (
 id bigserial PRIMARY KEY, user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 product_id bigint NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(user_id,product_id)
);
CREATE INDEX ix_favorite_user_date ON product_favorites(user_id,created_at DESC);
CREATE TABLE product_reviews (
 id bigserial PRIMARY KEY, version bigint NOT NULL DEFAULT 0,
 user_id bigint NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
 product_id bigint NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
 order_detail_id bigint NOT NULL REFERENCES order_details(id) ON DELETE RESTRICT,
 rating integer NOT NULL CHECK(rating BETWEEN 1 AND 5),
 content varchar(2000) NOT NULL CHECK(char_length(trim(content)) BETWEEN 50 AND 2000),
 hidden boolean NOT NULL DEFAULT false, moderation_reason varchar(1000),
 seller_reply varchar(2000), replied_at timestamptz,
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(user_id,product_id)
);
CREATE INDEX ix_review_product_date ON product_reviews(product_id,hidden,created_at DESC);
CREATE TABLE product_review_media (
 review_id bigint NOT NULL REFERENCES product_reviews(id) ON DELETE CASCADE,
 position integer NOT NULL, url varchar(1000) NOT NULL, storage_id varchar(255) NOT NULL,
 kind varchar(10) NOT NULL CHECK(kind IN ('IMAGE','VIDEO')), PRIMARY KEY(review_id,position)
);
CREATE TABLE review_moderation_history (
 id bigserial PRIMARY KEY, review_id bigint NOT NULL REFERENCES product_reviews(id) ON DELETE RESTRICT,
 actor varchar(50) NOT NULL, hidden boolean NOT NULL, reason varchar(1000) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- DDL END
INSERT INTO marketplace_migrations(version) VALUES('V004');
COMMIT;
