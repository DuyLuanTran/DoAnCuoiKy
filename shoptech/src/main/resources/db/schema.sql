-- ====================================================================
-- SHOPTECH DATABASE SCHEMA (POSTGRESQL / SUPABASE)
-- Cơ chế: Soft-delete / Thùng rác (Trash Bin) chuyên nghiệp
-- Hỗ trợ: Lưu trữ tạm thời, Khôi phục (Restore), Xoá vĩnh viễn (Force Delete)
-- ====================================================================

-- 1. EXTENSIONS & DỌN DẸP NẾU CẦN
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DROP VIEW IF EXISTS v_trash_products;
DROP VIEW IF EXISTS v_active_products;
DROP VIEW IF EXISTS v_trash_categories;
DROP VIEW IF EXISTS v_active_categories;
DROP VIEW IF EXISTS v_trash_users;
DROP VIEW IF EXISTS v_active_users;

DROP TABLE IF EXISTS chat_messages CASCADE;
DROP TABLE IF EXISTS order_details CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS cart_items CASCADE;
DROP TABLE IF EXISTS carts CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS password_reset_tokens CASCADE;
DROP TABLE IF EXISTS users_roles CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ====================================================================
-- MODULE 1 (NGƯỜI 1): AUTH & QUẢN LÝ NGƯỜI DÙNG
-- ====================================================================

-- Bảng Roles (ADMIN, USER)
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,       -- ROLE_ADMIN, ROLE_USER
    description VARCHAR(255)
);

-- Bảng Users (Hỗ trợ Khóa & Chuyển vào Thùng rác)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,         -- Mật khẩu mã hoá BCrypt
    full_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(15),
    address VARCHAR(255),
    avatar_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,       -- TRUE: Hoạt động | FALSE: Bị khóa
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,     -- TRUE: Nằm trong thùng rác
    deleted_at TIMESTAMP WITH TIME ZONE,           -- Thời điểm đưa vào thùng rác
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng liên kết nhiều - nhiều giữa Users và Roles
CREATE TABLE users_roles (
    user_id BIGINT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Bảng Quên mật khẩu (OTP / Token reset mật khẩu)
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(100) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ====================================================================
-- MODULE 2 (NGƯỜI 2): DANH MỤC & SẢN PHẨM (Có Thùng rác)
-- ====================================================================

-- Bảng Danh mục sản phẩm (Hỗ trợ Thùng rác)
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    icon_class VARCHAR(50),                 -- VD: 'bi bi-laptop', 'bi bi-phone'
    is_active BOOLEAN NOT NULL DEFAULT TRUE,       -- Ẩn/Hiện trên thanh điều hướng
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,     -- Nằm trong thùng rác
    deleted_at TIMESTAMP WITH TIME ZONE,           -- Ngày giờ xóa vào thùng rác
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Sản phẩm (Hỗ trợ Thùng rác & khôi phục)
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    sku VARCHAR(50) UNIQUE,
    description TEXT,
    specifications TEXT,                    -- Cấu hình kỹ thuật chi tiết
    price NUMERIC(15, 2) NOT NULL,          -- Giá niêm yết
    discount_price NUMERIC(15, 2),          -- Giá giảm (nếu có)
    stock_quantity INT NOT NULL DEFAULT 0,  -- Số lượng tồn kho
    main_image TEXT,                        -- Ảnh đại diện chính (Cloudinary)
    image_gallery TEXT[],                   -- Mảng URL ảnh chi tiết
    is_active BOOLEAN NOT NULL DEFAULT TRUE,       -- Đang kinh doanh
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,     -- TRUE: Đã chuyển vào thùng rác
    deleted_at TIMESTAMP WITH TIME ZONE,           -- Ngày giờ xoá vào thùng rác
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
);

-- ====================================================================
-- MODULE 3 (NGƯỜI 3): GIỎ HÀNG, ĐƠN HÀNG, THỐNG KÊ & CHATBOT
-- ====================================================================

-- Bảng Giỏ hàng (1 User - 1 Giỏ hàng)
CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Bảng Chi tiết giỏ hàng
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_cartitem_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cartitem_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Bảng Đơn hàng (Orders - Hỗ trợ lưu trữ / thùng rác nếu huỷ)
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_code VARCHAR(30) NOT NULL UNIQUE, -- VD: ORD-202609-0001
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    shipping_address TEXT NOT NULL,
    note TEXT,
    subtotal_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    shipping_fee NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(15, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL DEFAULT 'COD',   -- COD, VNPAY, BANKING
    payment_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID', -- UNPAID, PAID, REFUNDED
    order_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,      -- TRUE: Chuyển vào lưu trữ / thùng rác
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Bảng Chi tiết đơn hàng (OrderDetail - Snapshot giá & tên lúc mua)
CREATE TABLE order_details (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_image TEXT,
    price NUMERIC(15, 2) NOT NULL,          -- Snapshot giá bán tại thời điểm mua
    quantity INT NOT NULL CHECK (quantity > 0),
    subtotal NUMERIC(15, 2) NOT NULL,       -- price * quantity
    CONSTRAINT fk_detail_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_detail_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

-- Bảng Lịch sử Chatbot / WebSocket
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    session_id VARCHAR(100) NOT NULL,
    sender_type VARCHAR(20) NOT NULL,       -- 'USER', 'BOT', 'ADMIN'
    message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- ====================================================================
-- CÁC TIỆN ÍCH SQL (VIEWS): GIÚP TRUY VẤN SẢN PHẨM & THÙNG RÁC DỄ DÀNG
-- ====================================================================

-- 1. View xem sản phẩm đang bán (bỏ qua sản phẩm trong thùng rác)
CREATE OR REPLACE VIEW v_active_products AS
SELECT p.*, c.name AS category_name
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE p.is_deleted = FALSE AND p.is_active = TRUE AND c.is_deleted = FALSE;

-- 2. View xem thùng rác sản phẩm (Admin Trash Bin)
CREATE OR REPLACE VIEW v_trash_products AS
SELECT p.*, c.name AS category_name
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE p.is_deleted = TRUE
ORDER BY p.deleted_at DESC;

-- 3. View xem thùng rác người dùng (Admin Trash Bin)
CREATE OR REPLACE VIEW v_trash_users AS
SELECT * FROM users
WHERE is_deleted = TRUE
ORDER BY deleted_at DESC;

-- ====================================================================
-- INDEXES & TRIGGERS
-- ====================================================================
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_slug ON products(slug);
CREATE INDEX idx_products_trash ON products(is_deleted, is_active);
CREATE INDEX idx_categories_trash ON categories(is_deleted, is_active);
CREATE INDEX idx_users_trash ON users(is_deleted, is_active);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(order_status, is_deleted);
CREATE INDEX idx_orders_created ON orders(created_at);

-- Trigger cập nhật updated_at
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = CURRENT_TIMESTAMP;
   RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trg_update_users BEFORE UPDATE ON users FOR EACH ROW EXECUTE PROCEDURE update_timestamp();
CREATE TRIGGER trg_update_categories BEFORE UPDATE ON categories FOR EACH ROW EXECUTE PROCEDURE update_timestamp();
CREATE TRIGGER trg_update_products BEFORE UPDATE ON products FOR EACH ROW EXECUTE PROCEDURE update_timestamp();
CREATE TRIGGER trg_update_carts BEFORE UPDATE ON carts FOR EACH ROW EXECUTE PROCEDURE update_timestamp();
CREATE TRIGGER trg_update_orders BEFORE UPDATE ON orders FOR EACH ROW EXECUTE PROCEDURE update_timestamp();
