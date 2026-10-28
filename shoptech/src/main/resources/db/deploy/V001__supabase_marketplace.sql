BEGIN;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '120s';
SET LOCAL search_path = public, pg_catalog;
DO $$ BEGIN
    IF current_database() <> 'postgres' THEN
        RAISE EXCEPTION 'This deployment is restricted to postgres';
    END IF;
    IF to_regclass('public.marketplace_migrations') IS NOT NULL OR to_regclass('public.shops') IS NOT NULL THEN
        RAISE EXCEPTION 'Marketplace schema already present; stop instead of reapplying';
    END IF;
    IF (SELECT count(*) FROM public.users u JOIN public.users_roles ur ON ur.user_id=u.id JOIN public.roles r ON r.id=ur.role_id WHERE r.name='ROLE_ADMIN' AND u.is_active AND NOT u.is_deleted) <> 1 THEN
        RAISE EXCEPTION 'Expected exactly one active Admin for the bootstrap shop';
    END IF;
END $$;
-- V001__marketplace_foundation.sql
DO $$ BEGIN
    IF current_database() <> 'postgres' THEN
        RAISE EXCEPTION 'This migration is restricted to postgres';
    END IF;
END $$;

CREATE TABLE marketplace_migrations (
    version varchar(30) PRIMARY KEY,
    applied_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO roles(name, description) VALUES ('ROLE_SELLER', 'Người bán quản lý shop của mình')
ON CONFLICT (name) DO NOTHING;

CREATE TABLE shops (
    id bigserial PRIMARY KEY,
    owner_id bigint NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    name varchar(100) NOT NULL,
    slug varchar(160) NOT NULL UNIQUE,
    description varchar(2000),
    email varchar(100) NOT NULL,
    phone varchar(20) NOT NULL,
    address varchar(500) NOT NULL,
    logo_url text,
    banner_url text,
    status varchar(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','ACTIVE','REJECTED','SUSPENDED')),
    commission_rate numeric(5,2) CHECK (commission_rate BETWEEN 0 AND 100),
    submitted_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at timestamptz,
    reviewed_by bigint REFERENCES users(id) ON DELETE RESTRICT,
    rejection_reason varchar(1000),
    suspension_reason varchar(1000),
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_shops_status_created ON shops(status, created_at DESC);
CREATE TABLE shop_review_history (
    id bigserial PRIMARY KEY,
    shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE RESTRICT,
    actor_id bigint NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    previous_status varchar(20) NOT NULL,
    new_status varchar(20) NOT NULL,
    reason varchar(1000),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_shop_history ON shop_review_history(shop_id, created_at DESC);
CREATE TABLE marketplace_settings (
    id integer PRIMARY KEY CHECK (id = 1),
    default_commission_rate numeric(5,2) NOT NULL CHECK (default_commission_rate BETWEEN 0 AND 100),
    updated_by bigint REFERENCES users(id) ON DELETE RESTRICT,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO marketplace_settings(id, default_commission_rate) VALUES (1, 0);

INSERT INTO shops(owner_id,name,slug,email,phone,address,description,status,reviewed_at,reviewed_by)
SELECT u.id,'ShopTech','shoptech',u.email,COALESCE(NULLIF(u.phone_number,''),'Chưa cập nhật'),
       COALESCE(NULLIF(u.address,''),'Chưa cập nhật'),'Cửa hàng ShopTech hiện có','ACTIVE',CURRENT_TIMESTAMP,u.id
FROM users u
WHERE u.is_active=true AND u.is_deleted=false AND EXISTS (
    SELECT 1 FROM users_roles ur JOIN roles r ON r.id=ur.role_id
    WHERE ur.user_id=u.id AND r.name='ROLE_ADMIN'
) ORDER BY u.id LIMIT 1;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM shops WHERE slug='shoptech') THEN
        RAISE EXCEPTION 'An active Admin is required to own the existing ShopTech catalog';
    END IF;
END $$;
INSERT INTO users_roles(user_id,role_id)
SELECT s.owner_id,r.id FROM shops s CROSS JOIN roles r WHERE s.slug='shoptech' AND r.name='ROLE_SELLER'
ON CONFLICT DO NOTHING;
INSERT INTO marketplace_migrations(version) VALUES ('V001');

-- V002__shop_owned_products.sql
DO $$ BEGIN
    IF current_database() <> 'postgres' THEN
        RAISE EXCEPTION 'Sandbox migration requires postgres';
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

-- V003__shop_orders.sql
DO $$ BEGIN
    IF current_database()<>'postgres' THEN RAISE EXCEPTION 'Sandbox database required'; END IF;
    IF NOT EXISTS(SELECT 1 FROM marketplace_migrations WHERE version='V002') THEN RAISE EXCEPTION 'Apply V002 first'; END IF;
    IF EXISTS(SELECT 1 FROM order_details d LEFT JOIN products p ON p.id=d.product_id WHERE p.id IS NULL) THEN
        RAISE EXCEPTION 'Cannot infer shop for historical product';
    END IF;
END $$;
CREATE TABLE shop_orders (
    id bigserial PRIMARY KEY,
    order_id bigint NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE RESTRICT,
    shop_name varchar(100) NOT NULL,
    subtotal numeric(15,2) NOT NULL CHECK(subtotal>=0),
    shipping_fee numeric(15,2) NOT NULL DEFAULT 0 CHECK(shipping_fee>=0),
    total numeric(15,2) NOT NULL CHECK(total>=0),
    commission_rate numeric(5,2) NOT NULL CHECK(commission_rate BETWEEN 0 AND 100),
    commission_amount numeric(15,2) NOT NULL CHECK(commission_amount>=0),
    seller_amount numeric(15,2) NOT NULL CHECK(seller_amount>=0),
    historical_import boolean NOT NULL DEFAULT false,
    status varchar(30) NOT NULL,
    payment_status varchar(30) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_shop_order UNIQUE(order_id,shop_id),
    CONSTRAINT ck_shop_order_total CHECK(total=subtotal+shipping_fee),
    CONSTRAINT ck_shop_order_settlement CHECK(total=commission_amount+seller_amount)
);
CREATE INDEX idx_shop_orders_scope ON shop_orders(shop_id,status,created_at);
CREATE TABLE shop_order_history (
    id bigserial PRIMARY KEY,
    shop_order_id bigint NOT NULL REFERENCES shop_orders(id) ON DELETE RESTRICT,
    actor_id bigint NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    previous_status varchar(30) NOT NULL,
    new_status varchar(30) NOT NULL,
    reason varchar(1000) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_shop_order_history ON shop_order_history(shop_order_id,id DESC);
ALTER TABLE order_details ADD COLUMN shop_order_id bigint REFERENCES shop_orders(id) ON DELETE RESTRICT;
-- All historical lines belong to bootstrap ShopTech. Do not invent historical fees.
DO $$ BEGIN
    IF EXISTS(SELECT 1 FROM order_details d JOIN products p ON p.id=d.product_id GROUP BY d.order_id HAVING count(DISTINCT p.shop_id)>1) THEN
        RAISE EXCEPTION 'Historical multi-shop orders require an explicit shipping allocation';
    END IF;
    IF EXISTS(SELECT 1 FROM orders o WHERE NOT EXISTS(SELECT 1 FROM order_details d WHERE d.order_id=o.id)) THEN
        RAISE EXCEPTION 'Historical order without lines';
    END IF;
END $$;
INSERT INTO shop_orders(order_id,shop_id,shop_name,subtotal,shipping_fee,total,commission_rate,commission_amount,seller_amount,historical_import,status,payment_status,created_at,updated_at)
SELECT o.id,s.id,s.name,o.subtotal_amount,o.shipping_fee,o.total_amount,0,0,o.total_amount,true,o.order_status,o.payment_status,o.created_at,COALESCE(o.updated_at,o.created_at)
FROM orders o JOIN (SELECT DISTINCT d.order_id,p.shop_id FROM order_details d JOIN products p ON p.id=d.product_id) x ON x.order_id=o.id JOIN shops s ON s.id=x.shop_id;
UPDATE order_details d SET shop_order_id=s.id FROM shop_orders s WHERE s.order_id=d.order_id;
ALTER TABLE order_details ALTER COLUMN shop_order_id SET NOT NULL;
INSERT INTO marketplace_migrations(version) VALUES('V003');

-- V004__reviews_and_favorites.sql
DO $$ BEGIN
 IF current_database()<>'postgres' THEN RAISE EXCEPTION 'Sandbox database required'; END IF;
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
