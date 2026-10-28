# Thông báo đơn hàng

**Supabase của nhóm đã được cập nhật V005 ngày 28/10/2026.** Đã xác nhận trước khi chạy: database `postgres`, V004 có mặt, V005 và bảng mới chưa tồn tại. Script tạo bảng và chỉ mục trong một transaction; xác nhận V005 sau khi chạy. Không chạy lại script trên database này.

Với database thử tên `shoptech_marketplace`, dùng `src/main/resources/db/migrations/V005__order_notifications.sql` có kiểm tra tên database và phiên bản trước đó. Bản triển khai Supabase là `src/main/resources/db/deploy/V005__order_notifications.sql`. Không đổi file `.env`; cần khởi động lại ứng dụng để nạp code mới.

Thông báo mới được lưu khi checkout, khi seller đổi trạng thái đơn và khi người mua gửi yêu cầu trả hàng. Chuông ở header mua sắm và trang quản lý tải 30 thông báo mỗi lần, có nút xem thêm; nhấn vào thông báo sẽ đánh dấu đã đọc và mở đúng đơn. Lịch sử cũ không được tạo thông báo hồi tố. Người mua xem lịch sử trạng thái theo shop trong trang chi tiết đơn.

Mã trạng thái vẫn giữ nguyên tiếng Anh; câu thông báo hiển thị tiếng Việt. Thanh toán vẫn là COD theo luồng hiện tại.
