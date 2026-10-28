BEGIN;
DO $$ BEGIN
    IF current_database() <> 'shoptech_marketplace' THEN
        RAISE EXCEPTION 'Sandbox migration requires shoptech_marketplace';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM marketplace_migrations WHERE version='V001') THEN
        RAISE EXCEPTION 'V001 must be applied first';
    END IF;
END $$;
ALTER TABLE products ADD COLUMN shop_id bigint REFERENCES shops(id) ON DELETE RESTRICT;
UPDATE products SET shop_id=(SELECT id FROM shops WHERE slug='shoptech');
ALTER TABLE products ALTER COLUMN shop_id SET NOT NULL;
ALTER TABLE products DROP CONSTRAINT products_sku_key;
ALTER TABLE products ADD CONSTRAINT uq_products_shop_sku UNIQUE(shop_id,sku);
ALTER TABLE products ADD COLUMN is_blocked boolean NOT NULL DEFAULT false;
ALTER TABLE products ADD COLUMN moderation_reason varchar(1000);
ALTER TABLE products ADD COLUMN moderated_by bigint REFERENCES users(id) ON DELETE RESTRICT;
ALTER TABLE products ADD COLUMN moderated_at timestamptz;
CREATE INDEX idx_products_shop_visible ON products(shop_id,is_deleted,is_active,is_blocked);
CREATE TABLE product_moderation_history (
    id bigserial PRIMARY KEY,
    product_id bigint NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    actor_id bigint NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    blocked boolean NOT NULL,
    reason varchar(1000) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_product_moderation_history ON product_moderation_history(product_id,created_at DESC);
INSERT INTO marketplace_migrations(version) VALUES ('V002');
COMMIT;
