-- ====================================================================
-- SHOPTECH MOCK DATA (DỮ LIỆU MẪU - CÓ DỮ LIỆU THÙNG RÁC ĐỂ TEST)
-- Tương thích: Supabase SQL Editor & PostgreSQL
-- Ghi chú mật khẩu: admin: "admin123", user: "123456"
-- ====================================================================

-- 1. INSERT ROLES
INSERT INTO roles (id, name, description) VALUES
(1, 'ROLE_ADMIN', 'Quản trị viên toàn quyền hệ thống'),
(2, 'ROLE_USER', 'Khách hàng mua sắm thành viên')
ON CONFLICT (id) DO NOTHING;

SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));

-- 2. INSERT USERS (Có user bình thường, user bị khoá, và user trong thùng rác)
INSERT INTO users (id, username, email, password, full_name, phone_number, address, avatar_url, is_active, is_deleted, deleted_at, created_at) VALUES
-- User 1: Admin
(1, 'admin', 'admin@shoptech.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5V78GQR/q5pGjP6rEa42nK8i7O5eG', 'Quản Trị Viên ShopTech', '0901234567', 'Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150', TRUE, FALSE, NULL, '2026-08-01 08:00:00+07'),

-- User 2: Khách hàng mua sắm tích cực
(2, 'nguyenvana', 'vana@gmail.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Nguyễn Văn An', '0912345678', '123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150', TRUE, FALSE, NULL, '2026-08-10 09:30:00+07'),

-- User 3: Khách hàng nữ
(3, 'tranthib', 'thib@gmail.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Trần Thị Bình', '0987654321', '45 Cầu Giấy, Phường Quan Hoa, Quận Cầu Giấy, Hà Nội', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150', TRUE, FALSE, NULL, '2026-08-15 14:15:00+07'),

-- User 4: Tài khoản bị khoá (is_active = FALSE để test mở khoá)
(4, 'lequangc', 'quangc@gmail.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Lê Quang Cường (Bị khóa)', '0933445566', '789 Trần Hưng Đạo, Quận 5, TP. Hồ Chí Minh', NULL, FALSE, FALSE, NULL, '2026-08-20 16:45:00+07'),

-- User 5: TÀI KHOẢN ĐÃ CHUYỂN VÀO THÙNG RÁC (để test tính năng Khôi phục / Xoá vĩnh viễn)
(5, 'phamthid', 'thid@gmail.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Phạm Thị Dung (Trong thùng rác)', '0977889900', '12 Lê Duẩn, Đà Nẵng', NULL, FALSE, TRUE, '2026-09-12 11:20:00+07', '2026-08-25 10:00:00+07')
ON CONFLICT (id) DO NOTHING;

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

-- 3. INSERT USERS_ROLES
INSERT INTO users_roles (user_id, role_id) VALUES
(1, 1), -- Admin có ROLE_ADMIN
(1, 2), -- Admin có ROLE_USER
(2, 2),
(3, 2),
(4, 2),
(5, 2)
ON CONFLICT DO NOTHING;

-- 4. INSERT CATEGORIES (Có danh mục đang hoạt động và danh mục trong thùng rác)
INSERT INTO categories (id, name, slug, description, icon_class, is_active, is_deleted, deleted_at) VALUES
(1, 'Laptop & Máy Tính', 'laptop-may-tinh', 'Laptop gaming, laptop văn phòng, workstation chính hãng hiệu năng đỉnh cao', 'bi bi-laptop', TRUE, FALSE, NULL),
(2, 'Điện Thoại & Tablet', 'dien-thoai-tablet', 'Smartphone flagship, iPhone, Samsung, Xiaomi bảo hành chính hãng', 'bi bi-phone', TRUE, FALSE, NULL),
(3, 'Bàn Phím & Chuột', 'ban-phim-chuot', 'Bàn phím cơ Custom, chuột gaming không dây, phụ kiện góc setup', 'bi bi-keyboard', TRUE, FALSE, NULL),
(4, 'Màn Hình Đồ Hoạ', 'man-hinh-do-hoa', 'Màn hình 2K, 4K sắc nét cho Designer, Coder và Game thủ', 'bi bi-display', TRUE, FALSE, NULL),
(5, 'Tai Nghe & Âm Thanh', 'tai-nghe-am-thanh', 'Tai nghe chống ồn chủ động ANC, loa Bluetooth chuẩn Hi-Res Audio', 'bi bi-headphones', TRUE, FALSE, NULL),
-- Danh mục 6: ĐÃ CHUYỂN VÀO THÙNG RÁC
(6, 'Máy In & Máy Scan Cũ', 'may-in-may-scan-cu', 'Danh mục thiết bị văn phòng tạm thời ngừng hỗ trợ', 'bi bi-printer', FALSE, TRUE, '2026-09-10 09:00:00+07')
ON CONFLICT (id) DO NOTHING;

SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));

-- 5. INSERT PRODUCTS (Sản phẩm đang bán và sản phẩm trong thùng rác)
INSERT INTO products (id, category_id, name, slug, sku, description, specifications, price, discount_price, stock_quantity, main_image, is_active, is_deleted, deleted_at) VALUES
(1, 1, 'MacBook Pro 14 M3 Pro (18GB/512GB SSD)', 'macbook-pro-14-m3-pro-18gb-512gb', 'MBP-M3P-14',
 'MacBook Pro 14 inch trang bị chip M3 Pro mang lại hiệu năng phi thường cho các luồng công việc nặng. Màn hình Liquid Retina XDR tuyệt đẹp với thời lượng pin lên đến 18 giờ.',
 'CPU: Apple M3 Pro 11-core | GPU: 14-core | RAM: 18GB Unified | Ổ cứng: 512GB SSD | Màn hình: 14.2 inch 120Hz ProMotion Liquid Retina XDR',
 49990000, 46990000, 15, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800', TRUE, FALSE, NULL),

(2, 1, 'Laptop Asus ROG Zephyrus G16 (Core Ultra 9 / RTX 4080)', 'laptop-asus-rog-zephyrus-g16-rtx4080', 'ASUS-G16-4080',
 'Laptop gaming mỏng nhẹ hàng đầu với màn hình OLED 2.5K 240Hz, vỏ nhôm CNC nguyên khối siêu sang trọng cùng cấu hình đỉnh cao cân mọi tác vụ AI và render 3D.',
 'CPU: Intel Core Ultra 9 185H | GPU: NVIDIA GeForce RTX 4080 12GB | RAM: 32GB LPDDR5X | Ổ cứng: 1TB PCIe 4.0 | Màn hình: 16 inch OLED 2.5K 240Hz',
 68990000, 64990000, 8, 'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=800', TRUE, FALSE, NULL),

(3, 2, 'iPhone 16 Pro Max 256GB - Titan Tự Nhiên', 'iphone-16-pro-max-256gb-titan-tu-nhien', 'IP16PM-256-NAT',
 'Siêu phẩm flagship của Apple với khung viền Titan Grade 5 siêu nhẹ bền bỉ, vi xử lý A18 Pro 3nm mạnh mẽ nhất, nút Camera Control thế hệ mới và zoom quang học 5x.',
 'Chip: Apple A18 Pro | RAM: 8GB | Bộ nhớ: 256GB | Màn hình: 6.9 inch Super Retina XDR OLED 120Hz | Camera sau: 48MP + 48MP UltraWide + 12MP Tele 5x',
 34990000, 32990000, 30, 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800', TRUE, FALSE, NULL),

(4, 2, 'Samsung Galaxy S24 Ultra 5G 12GB/512GB', 'samsung-galaxy-s24-ultra-512gb', 'S24U-512-GRY',
 'Khám phá kỷ nguyên Galaxy AI toàn năng với bút S-Pen tích hợp, khung viền Titanium sang trọng, camera chụp đêm Nightography 200MP vượt bậc và màn hình phẳng viền siêu mỏng.',
 'Chip: Snapdragon 8 Gen 3 for Galaxy | RAM: 12GB | Bộ nhớ: 512GB | Màn hình: 6.8 inch Dynamic AMOLED 2X 120Hz | Pin: 5000 mAh sạc nhanh 45W',
 33990000, 29990000, 20, 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800', TRUE, FALSE, NULL),

(5, 3, 'Bàn phím cơ không dây Keychron K8 Pro QMK/VIA Gateron Red', 'ban-phim-co-keychron-k8-pro-gateron-red', 'KC-K8P-RED',
 'Bàn phím cơ Tenkeyless hỗ trợ QMK/VIA tùy biến toàn diện phím bấm và macro, kết nối đa thiết bị Bluetooth 5.1 & Type-C, switch Gateron Pro êm ái hỗ trợ Hot-swap.',
 'Layout: TKL 87 phím | Switch: Gateron G Pro Red (Hot-swap 3/5 pin) | Kết nối: Bluetooth 5.1 & Dây Type-C | Keycap: OSA Profile PBT Double-shot | Pin: 4000 mAh',
 2450000, 2190000, 50, 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800', TRUE, FALSE, NULL),

(6, 3, 'Chuột không dây công thái học Logitech MX Master 3S', 'chuot-khong-day-logitech-mx-master-3s', 'LOGI-MX3S-BLK',
 'Biểu tượng công nghệ cho dân lập trình và sáng tạo nội dung. Cảm biến 8000 DPI lướt trên mọi bề mặt kể cả mặt kính, nút bấm Quiet Clicks siêu êm và cuộn MagSpeed 1000 dòng/giây.',
 'Cảm biến: Darkfield 8000 DPI | Bấm phím: Yên tĩnh Quiet Clicks | Cuộn chuột: MagSpeed điện từ | Kết nối: Logi Bolt & Bluetooth | Thời lượng pin: Lên đến 70 ngày',
 2490000, 2090000, 45, 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=800', TRUE, FALSE, NULL),

(7, 4, 'Màn hình Dell UltraSharp U2724D 27 inch 2K IPS Black 120Hz', 'man-hinh-dell-ultrasharp-u2724d-2k', 'DELL-U2724D',
 'Màn hình đồ họa cao cấp công nghệ IPS Black cho tỷ lệ tương phản 2000:1, chuẩn màu 98% DCI-P3, tần số quét 120Hz mượt mà và cảm biến ánh sáng tự động điều chỉnh nhiệt độ màu.',
 'Kích thước: 27 inch | Tấm nền: IPS Black | Độ phân giải: 2K QHD (2560 x 1440) | Tần số quét: 120Hz | Cổng: DisplayPort 1.4, HDMI 2.1, USB-C 90W PD',
 11500000, 10890000, 12, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800', TRUE, FALSE, NULL),

(8, 5, 'Tai nghe không dây chống ồn Sony WH-1000XM5', 'tai-nghe-chong-on-sony-wh-1000xm5', 'SONY-WH1000XM5',
 'Công nghệ chống ồn hàng đầu thế giới với bộ xử lý tích hợp V1 và QN1. Đàm thoại rõ nét với 8 micro định dạng chùm sóng, hỗ trợ âm thanh Hi-Res Audio Wireless LDAC.',
 'Driver: 30mm sợi carbon | Thời lượng pin: 30 giờ (bật ANC) | Trọng lượng: 250g | Kết nối: Bluetooth 5.2 đa điểm, Jack 3.5mm | Tính năng: Speak-to-Chat',
 8490000, 7290000, 25, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800', TRUE, FALSE, NULL),

-- SẢN PHẨM 9: ĐÃ CHUYỂN VÀO THÙNG RÁC (để test tính năng Thùng rác sản phẩm của Người 2)
(9, 2, 'iPhone 11 64GB - Hàng Cũ (Ngừng kinh doanh)', 'iphone-11-64gb-hang-cu', 'IP11-64-OLD',
 'Sản phẩm đã hoàn tất vòng đời kinh doanh, được admin chuyển vào thùng rác để lưu trữ.',
 'Chip: Apple A13 Bionic | Màn hình: 6.1 inch Liquid Retina IPS LCD | Bộ nhớ: 64GB',
 7500000, 6900000, 0, 'https://images.unsplash.com/photo-1574755393849-623942496936?w=800', FALSE, TRUE, '2026-09-14 15:30:00+07')
ON CONFLICT (id) DO NOTHING;

SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));

-- 6. INSERT CARTS & CART ITEMS
INSERT INTO carts (id, user_id) VALUES
(1, 2),
(2, 3)
ON CONFLICT (id) DO NOTHING;

SELECT setval('carts_id_seq', (SELECT MAX(id) FROM carts));

INSERT INTO cart_items (cart_id, product_id, quantity) VALUES
(1, 6, 1),
(1, 5, 1),
(2, 8, 1)
ON CONFLICT DO NOTHING;

-- 7. INSERT ORDERS
INSERT INTO orders (id, order_code, user_id, recipient_name, recipient_phone, shipping_address, note, subtotal_amount, shipping_fee, total_amount, payment_method, payment_status, order_status, is_deleted, deleted_at, created_at) VALUES
-- Đơn 1: Hoàn thành (Tháng 8/2026)
(1, 'ORD-202608-0001', 2, 'Nguyễn Văn An', '0912345678', '123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. HCM', 'Giao giờ hành chính', 32990000, 0, 32990000, 'BANKING', 'PAID', 'COMPLETED', FALSE, NULL, '2026-08-12 10:20:00+07'),

-- Đơn 2: Hoàn thành (Tháng 9/2026)
(2, 'ORD-202609-0002', 3, 'Trần Thị Bình', '0987654321', '45 Cầu Giấy, Quan Hoa, Cầu Giấy, Hà Nội', 'Gọi trước khi giao', 10890000, 30000, 10920000, 'COD', 'PAID', 'COMPLETED', FALSE, NULL, '2026-09-05 14:40:00+07'),

-- Đơn 3: Đang giao (SHIPPING)
(3, 'ORD-202609-0003', 2, 'Nguyễn Văn An', '0912345678', '123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. HCM', NULL, 46990000, 0, 46990000, 'VNPAY', 'PAID', 'SHIPPING', FALSE, NULL, '2026-09-14 09:15:00+07'),

-- Đơn 4: Chờ xử lý (PENDING)
(4, 'ORD-202609-0004', 3, 'Trần Thị Bình', '0987654321', '45 Cầu Giấy, Quan Hoa, Cầu Giấy, Hà Nội', 'Hàng dễ vỡ, bọc xốp kĩ giúp mình', 4280000, 30000, 4310000, 'COD', 'UNPAID', 'PENDING', FALSE, NULL, '2026-09-16 19:30:00+07'),

-- Đơn 5: Đã huỷ (CANCELLED)
(5, 'ORD-202609-0005', 2, 'Nguyễn Văn An', '0912345678', '123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. HCM', 'Đổi ý không mua nữa', 7290000, 30000, 7320000, 'COD', 'UNPAID', 'CANCELLED', FALSE, NULL, '2026-09-10 11:00:00+07')
ON CONFLICT (id) DO NOTHING;

SELECT setval('orders_id_seq', (SELECT MAX(id) FROM orders));

-- 8. INSERT ORDER_DETAILS (Snapshot giá & tên)
INSERT INTO order_details (order_id, product_id, product_name, product_image, price, quantity, subtotal) VALUES
(1, 3, 'iPhone 16 Pro Max 256GB - Titan Tự Nhiên', 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800', 32990000, 1, 32990000),
(2, 7, 'Màn hình Dell UltraSharp U2724D 27 inch 2K IPS Black 120Hz', 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800', 10890000, 1, 10890000),
(3, 1, 'MacBook Pro 14 M3 Pro (18GB/512GB SSD)', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800', 46990000, 1, 46990000),
(4, 5, 'Bàn phím cơ không dây Keychron K8 Pro QMK/VIA Gateron Red', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800', 2190000, 1, 2190000),
(4, 6, 'Chuột không dây công thái học Logitech MX Master 3S', 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=800', 2090000, 1, 2090000),
(5, 8, 'Tai nghe không dây chống ồn Sony WH-1000XM5', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800', 7290000, 1, 7290000)
ON CONFLICT DO NOTHING;

-- 9. INSERT CHAT_MESSAGES
INSERT INTO chat_messages (user_id, session_id, sender_type, message, created_at) VALUES
(2, 'sess_user2_001', 'USER', 'Shop ơi, MacBook Pro M3 Pro này có hỗ trợ xuất 2 màn hình ngoài không shop?', '2026-09-17 08:30:00+07'),
(NULL, 'sess_user2_001', 'BOT', 'Dạ chào anh An! Bản chip Apple M3 Pro hỗ trợ xuất tối đa lên đến 2 màn hình ngoài độ phân giải cao qua cổng Thunderbolt 4 và HDMI ạ. Anh cần tư vấn thêm về cáp chuyển đổi không ạ?', '2026-09-17 08:30:05+07')
ON CONFLICT DO NOTHING;
