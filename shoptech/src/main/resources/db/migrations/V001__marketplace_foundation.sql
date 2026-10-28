-- Run only on the local sandbox. Never run schema.sql against the source database.
BEGIN;
DO $$ BEGIN
    IF current_database() <> 'shoptech_marketplace' THEN
        RAISE EXCEPTION 'This migration is restricted to shoptech_marketplace';
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
-- No business commission percentage is assumed: Admin chooses the value in the sandbox.
INSERT INTO marketplace_settings(id, default_commission_rate) VALUES (1, 0);

-- The existing single-store catalog is assigned to the first active Admin in the clone.
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
COMMIT;
