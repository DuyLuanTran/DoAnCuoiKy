# Bước 6 — Yêu thích và đánh giá sản phẩm

## Phạm vi đã thực hiện

- Nút trái tim trên thẻ sản phẩm; trang `/favorites` phân trang 12 sản phẩm, giữ dữ liệu theo tài khoản trong database. Header dùng chung có đường dẫn Yêu thích.
- Trang chi tiết sản phẩm có đánh giá 1–5 sao, bình luận 50–2000 ký tự, tối đa 3 ảnh (5 MB/ảnh), 1 video MP4/WebM (20 MB), tổng 25 MB. Có xem trước tệp trước khi gửi.
- Chỉ người mua có đơn con của shop `COMPLETED` được đánh giá; đơn tổng có thể vẫn `PENDING`. Đơn trả hàng/hoàn tiền, đơn bị xóa và dữ liệu lịch sử chưa đối soát không tạo quyền đánh giá mới. Chủ shop không tự đánh giá sản phẩm mình. Mỗi user/product một đánh giá.
- Điểm sao trung bình và số bài được tính từ đánh giá đang hiển thị; danh sách đánh giá phân trang 5 bài. Không tạo sao hoặc lượt đánh giá giả.
- Seller: `/seller/reviews`, chỉ đọc và trả lời đánh giá của shop mình. Có kiểm tra phiên bản để tránh ghi đè thay đổi từ trang cũ.
- Admin: `/admin/reviews`, ẩn/hiện lại với lý do bắt buộc, lưu `review_moderation_history`. Admin không sửa nội dung hoặc điểm của khách.
- Nội dung dùng `th:text`/DOM textContent; tệp được kiểm tra MIME, chữ ký định dạng, dung lượng, số lượng; tên lưu UUID. Nếu transaction không commit, các tệp vừa upload được dọn. Profile isolated chỉ lưu `.local/media`; không gọi upload Cloudinary thật.
- Yêu thích không làm sản phẩm đang ẩn/bị chặn/shop ngừng bán xuất hiện công khai. Bỏ thích có thể thực hiện ngay cả khi sản phẩm ngừng bán. Xóa hẳn tài khoản chưa có đơn hàng tự dọn các mục yêu thích của tài khoản đó.

## Database và khôi phục

Migration: `src/main/resources/db/migrations/V004__reviews_and_favorites.sql`.
Bốn bảng mới: `product_favorites`, `product_reviews`, `product_review_media`, `review_moderation_history`.
Chỉ áp dụng vào PostgreSQL Docker `shoptech_marketplace` tại `127.0.0.1:55432`; migration có guard từ chối database khác. Chưa triển khai vào database gốc/Supabase.

Snapshot trước bước này: `.local/backups/step6-before-community.dump`. Giữ các snapshot bước trước. Không restore đè khi web đang chạy; cần dừng preview và chọn đúng database local. Restore snapshot sẽ đưa toàn bộ dữ liệu sandbox về trước bước 6, bao gồm bỏ dữ liệu thử tạo sau snapshot. Code và schema phải được đưa về cùng mốc; không chạy code mới với schema thiếu V004.

Code vẫn chưa commit/push. Không dùng V004 trực tiếp cho production; cần chuẩn bị bản migration triển khai và sao lưu production riêng khi được yêu cầu.

## Cách xem và thử

1. Mở `http://localhost:18081/products` hoặc trang chi tiết một sản phẩm. Bấm trái tim, vào Yêu thích ở header, thử bỏ thích rồi tải lại trang.
2. Tài khoản người mua mẫu `sandbox-other` đã có đơn con COMPLETED của An Khang Tech. Bài đánh giá thử kèm ảnh trên `Laptop An Khang Demo` đã được gửi bằng giao diện, không chèn trực tiếp bằng SQL.
3. Bài thử có nội dung ghi rõ database riêng; seller `sandbox-user` đã phản hồi bằng giao diện. Mỗi tài khoản chỉ gửi một lần cho mỗi sản phẩm, nên form của bài này sẽ không hiện lại với `sandbox-other`.
4. Để thử gửi bài mới: dùng tài khoản mua hàng khác trong sandbox, mua sản phẩm của An Khang Tech bằng COD; vào seller lần lượt xác nhận/lấy hàng/giao hàng/hoàn tất. Quay về trang chi tiết sản phẩm bằng tài khoản người mua để đánh giá.
5. Đăng nhập `sandbox-user` để vào `/seller/reviews`; đăng nhập `sandbox-admin` tại `127.0.0.1:18081` để vào `/admin/reviews`. Hai hostname localhost và 127.0.0.1 có cookie riêng, tiện thử song song.
6. Admin đã thử ẩn và hiện lại bài mẫu. Hiện bài đang hiển thị để bạn xem; điểm và bài biến mất khỏi trang công khai khi ẩn, xuất hiện lại khi mở.

Mật khẩu demo giữ nguyên từ các bước trước; không thay mật khẩu tài khoản gốc.

## Xác nhận

- Compile thành công.
- Bộ H2/unit và PostgreSQL riêng: 224 ca qua ở `.local/test-step6-full.log` trước bổ sung test dọn yêu thích khi xóa tài khoản.
- Media validation/local storage: 5 ca qua ở `.local/test-community-media.log`.
- Lần kiểm thử cuối cho phần này (H2 + PostgreSQL + media) ghi ở `.local/test-community-final.log`.
- Trên web đã thử thật: mua hàng đủ điều kiện, gửi sao/bình luận/ảnh, thêm yêu thích, trang yêu thích, seller trả lời, admin ẩn và hiện lại.
- Ảnh minh chứng: `.local/evidence/reviews-step6.png`.
- Upload video được hỗ trợ và kiểm tra định dạng/giới hạn bằng test; lần thử trực tiếp trên trình duyệt dùng ảnh PNG. Upload Cloudinary thật chưa chạy vì đang dùng môi trường riêng.

Các yêu cầu khác như OTP, nhiều địa chỉ, sản phẩm đã xem, voucher và vận chuyển cần đối chiếu ở bước tiếp theo; bước này không đồng nghĩa đã hoàn thành toàn bộ đề bài.
