> Checkpoint tiếp theo: [Trang shop và bộ lọc theo shop](MARKETPLACE_STEP4.md).

# Checkpoint 3 — Đơn hàng và doanh thu theo shop

## Phạm vi hoàn tất
- Seller: danh sách/tìm kiếm/lọc/phân trang đơn của shop, chi tiết chỉ chứa hàng của shop, lịch sử cập nhật trạng thái.
- Luồng: PENDING → CONFIRMED → PICKED_UP → SHIPPING → COMPLETED. Hủy từ PENDING/CONFIRMED; sau hoàn tất có RETURN_REQUESTED → REFUNDED, bắt buộc lý do.
- Hủy/hoàn tiền chỉ hoàn kho phần hàng của shop đó; khóa đơn cha và khóa sản phẩm theo ID tăng dần để chống xử lý lặp, xung đột trả 409.
- Khi shop SUSPENDED vẫn được xử lý đơn cũ; việc bán hàng mới vẫn bị chặn.
- Một checkout lưu một đơn cha + một shop_order mỗi shop. Tiền hàng, phí giao, tên shop và chiết khấu được chốt tại checkout, không đổi theo cấu hình/giá sau này.
- Chiết khấu = subtotal × tỷ lệ / 100, làm tròn HALF_UP 2 chữ số; còn lại = tổng đơn shop − chiết khấu. Hiện phí giao đơn mới vẫn 0 như luồng COD cũ.
- Admin xem tổng hợp các đơn và chiết khấu; không được cập nhật trạng thái thay Seller. PATCH cũ /admin/orders/{id}/status trả 403.
- Thống kê từng shop và toàn sàn dùng COMPLETED, không tính đơn bị xóa/CANCELLED/RETURN_REQUESTED/REFUNDED. Gom ngày/tháng theo ngày tạo đơn tại Asia/Ho_Chi_Minh; đếm đơn shop, không đếm lặp theo dòng hàng.
- Khách xem trạng thái mỗi shop. Trạng thái cha đại diện phần hàng còn xử lý chậm nhất (ưu tiên RETURN_REQUESTED khi có); thanh toán có PARTIALLY_PAID. Giá trị đặt ban đầu trong lịch sử được giữ nguyên; tổng hiện tại trong chi tiết loại CANCELLED/REFUNDED.
- Menu đơn hàng/biểu đồ dùng icon fa-clipboard-list/fa-chart-line, cùng span/font và trạng thái active như các mục cũ. Giao diện quản trị dùng SB Admin2, không nhúng CSS storefront vào chi tiết đơn.

## Database riêng và bảo toàn dữ liệu
- Chỉ migration V003 trên 127.0.0.1:55432/shoptech_marketplace. Supabase gốc không được cập nhật.
- Backup trước bước này: .local/backups/step2-before-orders.dump (cùng ảnh local tại .local/media).
- V003 có kiểm tra tên DB và phiên bản V002, transaction toàn bộ; thêm shop_orders, shop_order_history, order_details.shop_order_id.
- 10 đơn / 17 dòng lịch sử chuyển nguyên vẹn sang ShopTech, giữ nguyên số tiền, trạng thái và ngày. Chiết khấu lịch sử là 0% vì chưa có căn cứ áp phí hồi tố.
- Phát hiện sẵn 5 đơn ID 1–5 có tổng dòng hàng gấp đôi subtotal đơn. Lượt chuyển đầu tiên đã rollback khi kiểm tra phát hiện; migration cuối đánh dấu historical_import và không sửa dữ liệu tiền/chi tiết đó.
- Đơn lịch sử lệch tiền được chặn cập nhật trạng thái/hoàn kho cho tới khi đối soát. Trang Seller/Admin hiện cảnh báo dữ liệu nhập cũ.
- Không chạy schema.sql hoặc migration này trên Supabase. Quay về checkpoint 2 cần checkout/snapshot code tương ứng và phục hồi dump vào một DB local trống riêng; không phục hồi đè khi server đang chạy.

## Kiểm tra
- .local/test-step3-postgres-full.log: 148 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS. Gồm 99 kiểm thử thông thường + 49 PostgreSQL (37 commerce, 12 seller orders) trong schema riêng trên shoptech_person3_test.
- Sau khi cố định thứ tự lựa chọn trạng thái (ưu tiên tiến bước thay vì chọn hủy), chạy lại 24 kiểm thử Seller trên H2 và PostgreSQL: .local/test-step3-seller-final.log, tất cả đạt.
- Kiểm tra nhiều shop, tiền/chiết khấu chốt, retry checkout, ngăn truy cập chéo, CSRF, Admin chỉ đọc, chuyển trạng thái, hủy/hoàn kho một lần, hoàn tiền, thống kê theo shop và múi giờ, dữ liệu cũ lệch tiền, thông báo sau commit.
- Trình duyệt trên DB preview: sandbox-other mua chuột ShopTech + Laptop An Khang Demo, tạo đơn cha 11, hai đơn shop S11/S12. Seller sandbox-user chỉ thấy S12. Chuyển S12 tới COMPLETED; thống kê riêng 14.990.000 VND, phí 749.500, còn lại 14.240.500. Khách thấy S11 PENDING và S12 COMPLETED/PAID, cha PARTIALLY_PAID.
- Dữ liệu demo để lại cho người dùng tự xem. Không giao hàng/gửi mail/chuyển tiền thật. Chỉ tồn kho trong bản sao bị trừ.

## Xem bản thử
- Seller: http://localhost:18081/seller/orders và /seller/revenue (sandbox-user).
- Admin: http://127.0.0.1:18081/admin/orders và /admin/revenue (sandbox-admin).
- Dùng tài khoản thử đã tạo ở bước 1. Cổng 18081 chạy bằng scripts/Start-MarketplaceSandbox.ps1; không dùng lệnh khởi động production mặc định.

## Giới hạn và bước sau
- REFUNDED ghi nhận hoàn tiền thủ công cho COD; chưa tích hợp cổng thanh toán, chuyển tiền seller, hay xác minh giao dịch hoàn tiền bên ngoài. Seller phải ghi nhận sau khi đã xử lý thực tế.
- Chưa có yêu cầu trả hàng từ khách, thời hạn trả hàng, khiếu nại/đối soát, nhà vận chuyển hoặc Shipper. Trạng thái hiện do Seller cập nhật.
- Bước tiếp theo sau khi duyệt UI: storefront marketplace / trang shop / lọc theo shop; rồi các chức năng còn lại như địa chỉ, đánh giá, yêu thích, OTP, voucher và thanh toán trực tuyến theo kế hoạch.
