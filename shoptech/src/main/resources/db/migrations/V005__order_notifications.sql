BEGIN;
DO $$ BEGIN
    IF current_database()<>'shoptech_marketplace' THEN RAISE EXCEPTION 'Sandbox database required'; END IF;
    IF NOT EXISTS(SELECT 1 FROM marketplace_migrations WHERE version='V004') THEN RAISE EXCEPTION 'Apply V004 first'; END IF;
END $$;
CREATE TABLE order_notifications (
    id bigserial PRIMARY KEY,
    recipient_id bigint NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    order_id bigint NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    shop_order_id bigint REFERENCES shop_orders(id) ON DELETE RESTRICT,
    audience varchar(20) NOT NULL CHECK (audience IN ('BUYER','SELLER')),
    message varchar(255) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at timestamptz
);
CREATE INDEX idx_order_notifications_inbox ON order_notifications(recipient_id,created_at DESC,id DESC);
CREATE INDEX idx_order_notifications_unread ON order_notifications(recipient_id) WHERE read_at IS NULL;
INSERT INTO marketplace_migrations(version) VALUES('V005');
COMMIT;
