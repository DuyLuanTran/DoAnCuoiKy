BEGIN;
DO $$ BEGIN
    IF current_database()<>'shoptech_marketplace' THEN RAISE EXCEPTION 'Sandbox database required'; END IF;
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
COMMIT;
