> Checkpoint tiếp theo: [Trang chủ marketplace và menu Seller](MARKETPLACE_STEP5.md).

# Checkpoint 4A — Trang shop và lọc sản phẩm theo shop

## Đã hoàn tất
- Trang công khai /shops/{slug}, dùng lại user/store.html và lưới Electro: tên shop, thời gian tham gia, số sản phẩm đang được hiển thị, giới thiệu có thể mở rộng.
- PublicShop DTO chỉ chứa id/name/slug/description/joinedAt. Không đưa email/điện thoại/địa chỉ hồ sơ, thông tin chủ shop, chiết khấu hay lý do xét duyệt ra storefront.
- Sản phẩm chỉ xuất hiện khi shop ACTIVE, chủ shop active/chưa xóa, sản phẩm active/chưa xóa/không bị khóa và danh mục active/chưa xóa.
- Trang /products có bộ lọc Cửa hàng; tên shop trên thẻ sản phẩm và trang chi tiết đều bấm sang shop được.
- Tìm kiếm theo tên, danh mục, khoảng giá theo giá đang bán, còn hàng, giảm giá, sắp xếp và phân trang được kết hợp với shop.
- Đường dẫn /shops/{slug} cố định shop từ slug, bỏ qua shopId giả trong query. Header tìm kiếm, menu danh mục, sidebar và phân trang giữ phạm vi hiện tại. Footer và logo vẫn dẫn về phạm vi toàn sàn theo thiết kế chung.
- Xóa bộ lọc trong shop chỉ xóa các lựa chọn trong shop. Xóa bộ lọc trên /products bỏ cả shop. Nút Mua sắm toàn sàn luôn về /products.
- Header đếm sản phẩm của shop là số sản phẩm công khai không phụ thuộc bộ lọc; kết quả tìm kiếm bên dưới có số riêng. Shop trống không thêm dữ liệu/đánh giá giả.
- Seller dashboard có nút Xem trang shop khi ACTIVE.
- Giữ ảnh đang có trong database, thêm biểu tượng shop từ Font Awesome sẵn có, không đổi bộ template hoặc thêm thư viện.
- CSS riêng marketplace.css cho thông tin shop, tên dài, bộ lọc shop. Có quy tắc màn hình nhỏ; kiểm tra trình duyệt trực tiếp ở kích thước desktop hiện có, chưa có lượt kiểm tra trên thiết bị điện thoại thật.

## Dữ liệu và môi trường
- Không cần migration hoặc thay đổi cấu trúc DB ở checkpoint này.
- Bản thử cổng 18081 tiếp tục dùng 127.0.0.1:55432/shoptech_marketplace. Không cập nhật Supabase gốc.
- Thử giao diện ở bước này chỉ đọc sản phẩm/shop; không tạo đơn hay sửa tồn kho/dữ liệu shop.
- Hai shop công khai hiện có: /shops/shoptech (9 sản phẩm) và /shops/an-khang-tech-8 (1 sản phẩm demo).

## Xác nhận
- .local/test-step4-public.log: 24 tests PASS (12 H2 + 12 PostgreSQL).
- .local/test-step4-full.log: 172 tests, 0 failures/errors/skips, BUILD SUCCESS. Bao gồm 111 kiểm thử thông thường và 61 kiểm thử PostgreSQL trong schema riêng của shoptech_person3_test.
- Phủ: truy cập guest, bảo vệ shop ngừng hoạt động/chủ shop khóa hoặc xóa, không lộ dữ liệu riêng, chống thay shopId trong route, truy vấn count/data nhất quán, bộ lọc kết hợp, giá sale, wildcard literal, escape nội dung shop, phân trang giữ query, lỗi dữ liệu đầu vào, header các trang mua hàng.
- Trình duyệt: chọn An Khang Tech từ dropdown chỉ thấy 1 sản phẩm; bấm Xem shop đúng trang, thử keyword không có kết quả, xóa lọc vẫn ở shop; nhập giá 10–20 triệu và tích còn hàng/giảm giá vẫn thấy laptop; đổi giá giảm dần giữ các giá/checkbox; Mua sắm toàn sàn về đủ 10 sản phẩm, lọc ShopTech còn 9; seller mở Xem trang shop thành công.
- Ảnh: .local/evidence/shop-page-step4.png và shop-filter-step4.png.

## Xem
- http://localhost:18081/shops/an-khang-tech-8
- http://localhost:18081/shops/shoptech
- http://localhost:18081/products?shopId=1

Mốc kế tiếp sau khi xem UI: trang chủ nhiều shop, sản phẩm mới/bán chạy từ dữ liệu đơn hàng. Trang chủ, xếp hạng bán trên 10, nhóm giỏ hàng theo shop chưa nằm trong checkpoint 4A.
