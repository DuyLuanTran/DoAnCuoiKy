# Bàn giao — Seller, đánh giá và yêu thích

Nhánh bàn giao: `fe/seller-rate-favorite`. Tài liệu này mô tả phạm vi, migration và khôi phục; hướng dẫn chạy sẽ bổ sung sau.

## Chức năng

- User đăng ký shop; admin duyệt, từ chối, đình chỉ hoặc mở lại shop và cấu hình chiết khấu mặc định/riêng từng shop.
- Seller quản lý sản phẩm, thùng rác, thông tin shop, đơn hàng và doanh thu của chính shop. Admin quản lý toàn sàn, giám sát sản phẩm và xem đơn/doanh thu toàn sàn.
- Đơn mua nhiều shop được tách thành đơn con, chốt giá và chiết khấu tại thời điểm đặt hàng; kiểm soát trạng thái và hoàn tồn kho.
- Trang chủ, danh sách sản phẩm, trang shop và header dùng chung; xếp hạng bán chạy dựa trên đơn con COMPLETED.
- Yêu thích lưu theo tài khoản. Khách đã mua từ đơn con COMPLETED có thể đánh giá 1–5 sao, bình luận 50–2000 ký tự, kèm ảnh/video. Seller trả lời; admin ẩn/hiện có lý do và lịch sử.
- Kiểm tra quyền ở backend, CSRF, phạm vi shop, kiểm tra tệp và phiên bản khi cập nhật đồng thời.

## Trạng thái database / migration

**Chưa cập nhật Supabase của nhóm. Code mới phụ thuộc schema mới; không triển khai code này với schema cũ.**

Các script được bàn giao tại `src/main/resources/db/migrations/`:

| Thứ tự | Script | Thay đổi |
| --- | --- | --- |
| 1 | V001__marketplace_foundation.sql | Shop, ROLE_SELLER, duyệt shop, cấu hình chiết khấu |
| 2 | V002__shop_owned_products.sql | Sản phẩm thuộc shop, SKU riêng từng shop, giám sát sản phẩm |
| 3 | V003__shop_orders.sql | Đơn con theo shop, lịch sử trạng thái, dữ liệu chiết khấu được chốt |
| 4 | V004__reviews_and_favorites.sql | Yêu thích, đánh giá, ảnh/video, lịch sử kiểm duyệt |

Đây là các migration đã kiểm thử trên bản sao riêng, có guard chỉ cho database `shoptech_marketplace`; **chưa phải migration triển khai Supabase**. Không xóa guard rồi chạy thẳng. Khi triển khai thật cần đối chiếu schema hiện tại, sao lưu, thử trên bản sao mới, lựa chọn chủ shop cho danh mục cũ và đối soát dữ liệu trước.

- V001 gán shop ShopTech ban đầu cho admin đang hoạt động đầu tiên và thêm ROLE_SELLER cho tài khoản đó. Cần chọn chủ shop phù hợp trước khi triển khai thật.
- Dữ liệu đơn cũ được giữ nguyên, chiết khấu lịch sử bằng 0. Một số đơn cũ trong bản sao có tổng chi tiết lệch tiền hàng của đơn; những đơn này bị chặn thao tác seller cho tới khi đối soát, không tự sửa số tiền.
- Không dùng `db/schema.sql` để nâng cấp DB đang có dữ liệu: file đó chứa DROP.
- Không nhập tài khoản demo, DB sandbox, file dump hoặc `.local/media` lên DB thật. Upload khi triển khai thật dùng Cloudinary của nhóm.

## Khôi phục

- Code: quay lại commit base của PR để đồng bộ với schema cũ; nếu PR đã merge dùng commit revert phù hợp của nhóm.
- Database: trước triển khai thật phải tạo và xác minh một bản sao lưu Supabase mới. Khi cần quay lại, dừng ghi dữ liệu, khôi phục bản sao lưu vào DB phục hồi để kiểm tra, rồi chuyển code và DB về cùng mốc.
- Revert Git không hoàn tác database. Nếu đã phát sinh đơn mới sau migration, cần giữ và đối soát dữ liệu đó trước khi khôi phục; không restore đè tùy tiện.
- Các dump của lần thử chỉ nằm trên máy thực hiện trong `.local/backups`, không có trên GitHub và không thay thế backup production.

## Kiểm thử đã có

- Bộ kiểm thử H2/unit và PostgreSQL riêng: 224 ca qua trước bổ sung kiểm tra dọn yêu thích khi xóa tài khoản.
- Lượt cuối cho chức năng mới: 31 ca qua (H2, PostgreSQL, kiểm tra media).
- Đã thử giao diện: gửi đánh giá kèm ảnh, yêu thích, seller trả lời và admin ẩn/hiện.
- Video có kiểm tra định dạng/giới hạn bằng test; lần thử upload trực tiếp dùng PNG. Cloudinary thật chưa được kiểm thử vì môi trường riêng không gọi dịch vụ thật.

Các tài liệu MARKETPLACE_STEP1–6 là ghi chép từng checkpoint, có thông tin lịch sử; trạng thái bàn giao hiện tại theo tài liệu này. Đây chưa phải toàn bộ yêu cầu của đề tài (OTP, nhiều địa chỉ, voucher, sản phẩm đã xem và các phần còn lại cần tiếp tục đối chiếu).

## Cấu hình bí mật

Đã bỏ giá trị JWT secret mặc định ghi cứng từ code cũ; ứng dụng lấy `JWT_SECRET` từ môi trường. Vì giá trị cũ đã nằm trong lịch sử Git, nhóm cần thay `JWT_SECRET` tại môi trường thật trước khi triển khai. Không đưa `.env` vào Git.
