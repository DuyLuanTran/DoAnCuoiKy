# Kiểm thử Người 3 trên Supabase chung — 22/10/2026

Ghi chú bàn giao Git: chỉ đưa báo cáo Markdown đã rà soát lên feature/cart-checkout. Toàn bộ ảnh, SQL result TXT, ZIP và evidence liên quan giữ local, không đưa lên GitHub. Báo cáo chỉ dùng tài khoản/dữ liệu mẫu đã được nhóm cho phép; không chứa mật khẩu, JWT hoặc cookie. Các ghi nhận không thao tác Git bên dưới mô tả phiên kiểm thử Supabase trước khi được cấp quyền commit/push.

Trạng thái: đã thực hiện các kịch bản được phép, hai đơn được giữ nguyên trên Supabase; Cart đã khôi phục số lượng ban đầu. Những mục chưa kiểm chứng được ghi NOT TESTED. Báo cáo này chỉ ghi lần chạy ứng dụng thật cổng 8080, không lấy kết quả H2 hoặc bộ PostgreSQL tạo schema.

## Xác minh môi trường và tài khoản

- JVM PID 17100: main class ShoptechApplication, user.dir là worktree hiện tại/shoptech. Không có datasource/spring.config/profiles.active trên command line. Không khởi động lại ứng dụng.
- Endpoint trong .env: aws-0-ap-southeast-1.pooler.supabase.com:6543, database postgres. Có 4 socket ESTABLISHED của JVM khớp IP DNS và port endpoint. JDBC dùng cấu hình gốc, transaction read-only, xác nhận current_database=postgres và transaction_read_only=on. Đây là đối chiếu process/cấu hình/socket/dữ liệu, không phải dump effective Spring Environment.
- Đăng nhập UI một lần thành công: nguyenvana (id 2, ROLE_USER); admin (id 1, ROLE_ADMIN và ROLE_USER). Cả hai active, không deleted; roles đối chiếu SELECT read-only users/users_roles/roles và code CustomUserDetailsService. Không sử dụng fixture person3-*.
- Giữ phiên USER ở localhost và ADMIN ở 127.0.0.1, cookie host-only tạo bởi AuthController giúp hai phiên không ghi đè nhau. Cùng JVM cổng 8080. Không xuất/lưu cookie, JWT, credential.
- Không sửa .env/source/branch, không chạy migration/schema.sql/data.sql/integration suite, không tạo/đổi tài khoản hoặc quyền.

## Dữ liệu gốc và thay đổi đã thực hiện

- Sản phẩm 6: Logitech MX Master 3S, SKU LOGI-MX3S-BLK; giá gốc 2.490.000, giá bán 2.090.000 VND; active, không deleted, tồn kho 45. Khớp dữ liệu seed đã khảo sát; người dùng cho phép sản phẩm này. Tồn kho đọc lại trực tiếp trước thao tác ghi và sau Cart vẫn 45.
- Cart nguyenvana ban đầu: CartItem id 2, sản phẩm 5 × 1; CartItem id 1, sản phẩm 6 × 1. Tổng 4.280.000 VND.
- Chỉ thao tác nghiệp vụ trên sản phẩm 6: thêm 1 (qty 2), sửa thành 3, thử 0/46 bị browser validation chặn, xóa dòng, thêm lại 1. Sau thao tác: sản phẩm 5 × 1 giữ nguyên CartItem id 2; sản phẩm 6 × 1 trở thành CartItem id 7. Số lượng/tổng tiền đã trở về gốc, ID dòng đã thay đổi và không được coi là hoàn tác toàn bộ.
- Checkpoint trước khi được phép checkout: chưa tạo đơn. Sau khi người dùng cho phép tạm xóa sản phẩm 5, đã đọc lại DB và Cart: id2/product5/qty1, id7/product6/qty1, stock6=45, giá và tổng tiền khớp. Chỉ xóa CartItem sản phẩm 5 bằng nút Xóa trên UI.
- Sau checkout/khôi phục: đã tạo đúng hai đơn ghi dưới đây; không thay đổi trạng thái đơn có sẵn, không sửa tồn kho trực tiếp. CartItem cuối: id9/product5/qty1 và id10/product6/qty1; tổng 4.280.000 VND. Không thu ID CartItem tạm thời cho đơn thứ hai trước khi checkout xóa dòng đó. ID dòng Cart thay đổi là tác động có lưu lại, không coi khôi phục số lượng là hoàn tác giao dịch.

## Hai đơn tạo trong phiên này và tác động còn tồn tại

| Order ID / code | Nghiệp vụ qua UI | Trạng thái cuối | Tổng tiền | Tồn kho sản phẩm 6 |
|---|---|---|---:|---|
| 6 / ORD-Vj9ZtXtSRG-jZ4qUpm-7dw | Checkout COD qty1, PENDING → CONFIRMED → SHIPPING → COMPLETED | COMPLETED / PAID | 2.090.000 VND | 45 → 44 khi checkout; giữ 44 khi hoàn tất |
| 7 / ORD-yf46bII8S_-ndRizDgNjqA | Checkout COD qty1, PENDING → CONFIRMED → CANCELLED | CANCELLED / UNPAID | 2.090.000 VND | 44 → 43 khi checkout; 43 → 44 khi hủy |

- Cả hai user_id=2 (nguyenvana), một OrderDetail duy nhất product_id=6, qty1, price/subtotal=2.090.000. Phí giao hàng 0. Người nhận/địa chỉ giả, ghi chú KHONG GIAO HANG. Không chỉnh sửa hoặc xóa đơn để dọn dữ liệu.
- Ngày 22/10/2026 và toàn tháng 10/2026: Revenue cuối 2.090.000 VND, 1 đơn COMPLETED. Hủy đơn7 không tăng doanh thu. Khoảng 01/09–22/10: từ 10.920.000/1 đơn lên 13.010.000/2 đơn. SELECT read-only GROUP BY ngày Việt Nam khớp UI.
- Sản phẩm 6 giảm ròng một chiếc: 45 → 44. Sản phẩm 5 không được checkout; cuối phiên đọc stock5=50. Không có snapshot SQL stock5 trước phiên nên không gán PASS cho đối chiếu trước/sau stock5; luồng UI chỉ xóa/thêm Cart của sản phẩm này.

## Kết quả kiểm thử thực tế

| Mục | Kết quả | Bằng chứng/giới hạn |
|---|---|---|
| USER và ADMIN đăng nhập tài khoản nhóm | PASS | UI vào /user/home và /admin; DB roles đúng |
| USER truy cập /admin/orders | PASS | UI trả Forbidden 403; user-admin-forbidden.png |
| Thêm sản phẩm 6 | PASS | Qty 1→2; dòng 4.180.000, tổng 6.370.000; cart-added.png |
| Sửa số lượng | PASS | Qty 3, dòng 6.270.000, tổng 8.460.000; cart-quantity3.png |
| Validation qty 0 | PASS (UI) | Native min=1 chặn, chưa kiểm tra backend invalid API; cart-invalid-zero.png |
| Validation qty 46 | PASS (UI) | Native max=45 chặn; chưa kiểm tra backend invalid API |
| Xóa và thêm lại sản phẩm 6 | PASS | Xóa còn tổng 2.190.000; thêm lại tổng 4.280.000; DB checkpoint khớp |
| Cart không giảm tồn kho | PASS | SELECT trước/sau: sản phẩm 6 stock=45 |
| Checkout hiển thị giỏ hiện tại | PASS (render) | Cả sản phẩm 5 và 6; checkout-existing-cart.png; chưa submit |
| Order History tải trong phiên USER | PASS (render) | 3 đơn seed của nguyenvana; order-history-existing.png; chưa có đơn mới |
| Admin Orders danh sách | PASS (render) | 5 đơn seed, admin-orders-baseline.png |
| Admin tìm chuỗi không tồn tại | PASS | PERSON3-NO-SUCH-ORDER-20261022 trả Không có đơn hàng phù hợp |
| Admin lọc PENDING | PASS | 1 dòng PENDING, admin-filter-pending.png; không sửa đơn |
| Phân trang Admin nhiều trang | NOT TESTED | Hiện 5 đơn, một trang; không tạo đơn thừa chỉ để ép phân trang |
| Revenue ngày khoảng rỗng | PASS | 23/09–22/10: 0 VND, 0 đơn |
| Revenue tháng khoảng rỗng | PASS | Hai bucket 2026-09/10 đều 0 |
| Revenue ngày có dữ liệu | PASS | 01/09–22/10: 10.920.000 VND, 1 đơn; dòng 05/09 có count=1 |
| Revenue tháng có dữ liệu | PASS | 2026-09: 10.920.000 VND, 1 đơn; 2026-10: 0 |
| Đối chứng Revenue với PostgreSQL | PASS | SELECT read-only count=1, sum(total_amount)=10920000.00 cùng khoảng Vietnam; db-readonly-checkpoint.txt |
| Alias/projection orderCount với dữ liệu PostgreSQL | PASS trong khoảng đã thử | UI hiển thị count=1 thực tế qua repository; không chỉ H2. Không suy rộng ra mọi timezone/boundary |
| Checkout COD và Order Detail đơn mới | PASS | Hai đơn6/7, mỗi đơn chỉ product6×1, tổng 2.090.000; Cart trống sau checkout, stock giảm đúng1; ảnh Checkout và db-after-checkout/db-before-cancel |
| PENDING → CONFIRMED → SHIPPING → COMPLETED | PASS | Đơn6 qua từng bước UI ADMIN; DB cuối COMPLETED/PAID; UI không còn form sửa đơn kết thúc |
| COD PAID trong Admin và User đọc mới | PASS | Admin/DB PAID khi COMPLETED; mở lại detail USER xác nhận PAID, order6-final-paid.png |
| Hủy sau CONFIRMED và hoàn kho | PASS | Đơn7 CANCELLED/UNPAID; DB stock43→44; không tăng Revenue |
| Hủy trực tiếp từ PENDING, hủy lặp/concurrency | NOT TESTED | Chỉ tạo thêm một đơn theo phạm vi cho phép, chọn nhánh CONFIRMED→CANCELLED; không gửi yêu cầu hủy lặp |
| WebSocket USER nhận trạng thái không reload | PASS | Giữ USER /orders/6 trong cả ba lần ADMIN đổi; status và toast CONFIRMED/SHIPPING/COMPLETED tự xuất hiện. Đơn7 nhận CONFIRMED/CANCELLED tương tự. Không điều hướng/reload USER giữa các bước đổi trạng thái |
| WebSocket cách ly tài khoản USER khác | NOT TESTED | Không có phiên USER khác đã xác thực để đối chứng. ADMIN ở host127.0.0.1 khác Origin không phải bằng chứng cách ly người nhận |
| History chứa hai đơn kiểm thử | PASS | order-history-test-orders.png hiển thị đúng code, tổng và trạng thái cuối của6/7 |
| Revenue sau đơn6 và sau hủy đơn7 | PASS | Ngày22/10 và nhóm tháng10 đều 2.090.000/1 đơn; PostgreSQL read-only đối chiếu cùng kết quả |
| Khôi phục Cart bằng nghiệp vụ UI | PASS | Product5×1 và6×1, tổng4.280.000; CartItem cuối9/10; db-final.txt và cart-final-restored.png |
| Backend API validation, concurrency/idempotency | NOT TESTED | Không chạy automation ghi hàng loạt trên DB chung |

## Bằng chứng và giới hạn

- Thư mục: person3-supabase-evidence; ảnh Cart, Checkout, History, Admin Orders và Revenue chụp trực tiếp từ phiên đang đăng nhập. Không chụp credential hoặc xuất cookie/JWT.
- Đọc trực tiếp /api/cart bằng browser bị ERR_BLOCKED_BY_CLIENT; không coi đây là lỗi API. UI Cart gọi API và SELECT DB xác nhận mutation, nhưng chưa thu raw HTTP status/body riêng trong phiên này.
- Read-only probe ban đầu dùng sai tên bảng user_roles (42P01), sau đó sửa theo entity thành users_roles. Probe aggregate ban đầu dùng status (42703), sau đó sửa thành order_status. Các lỗi thuộc công cụ kiểm tra, transaction read-only; không quy thành lỗi sản phẩm, không sửa source.
- Cấu hình source ddl-auto=none/sql.init.mode=never; chưa dump effective Spring config hay toàn bộ process environment. Không thử DDL để xác minh.
- Trong lần thêm probe OrderDetail, công cụ kiểm tra dùng nhầm unit_price/total_price (42703); đã sửa SELECT theo entity thành price/subtotal trước khi tiếp tục. Không phải lỗi ứng dụng, không có SQL ghi hoặc sửa source.
- WebSocket chỉ cập nhật trạng thái/toast theo payload hiện có; payment status trên trang USER đang mở giữ snapshot UNPAID đến khi đọc lại trang. Sau khi đã chứng minh thông báo không reload, mở lại detail USER thấy PAID và khớp DB; không gán PASS cho cập nhật payment status realtime.
- Không thu raw WebSocket frames/HTTP headers để tránh xuất credential hoặc cookie; bằng chứng realtime là các thay đổi DOM/toast thực tế và chuỗi thao tác UI. Không suy rộng thành kiểm chứng mọi trường hợp mất kết nối/JWT expiry/isolation.

## Bằng chứng lần tiếp tục và kiểm tra cuối

- Checkout: checkout-cod-product6.png, checkout-cancel-product6.png; đơn mới: order6-pending.png, order7-pending.png, order6-final-paid.png.
- Admin: admin-search-order6.png, admin-order6-completed.png, admin-order7-cancelled.png.
- WebSocket: websocket-order6-confirmed.png, websocket-order6-shipping.png, websocket-order6-completed.png, websocket-order7-cancelled.png. Ảnh CONFIRMED chứa toast thực tế.
- Revenue: revenue-day-final.png (22/10), revenue-month-final.png (01–31/10), chụp sau khi nút Xem thống kê enabled trở lại và dữ liệu đã tải xong.
- DB read-only: db-after-checkout.txt, db-after-completed.txt, db-before-cancel.txt, db-after-cancel.txt, db-final.txt. Mọi kết nối probe setReadOnly(true), autocommit=false, cuối rollback; chỉ SELECT.
- Đã xem ảnh WebSocket/Revenue để kiểm tra bằng chứng thực tế. Ảnh có viewport hẹp theo panel hiện tại; Cart cuối và detail PAID lưu fullPage. Không thay đổi thiết kế/source.
- git diff --check thành công; chỉ có cảnh báo LF/CRLF của thay đổi source tồn tại từ trước. git diff --cached --name-only rỗng; branch vẫn feature/khoa-person3. Không thực hiện thao tác Git ghi.
- Không chạy compile/test suite trong phiên shared DB này; phạm vi là browser nghiệp vụ thật và SELECT read-only. 31 bài PostgreSQL tạo/xóa schema tuyệt đối không chạy trên Supabase. Không dùng kết quả H2 thay thế.
- Hai tab xác thực được giữ để người dùng kiểm tra: USER Cart localhost:8080/cart và ADMIN Revenue 127.0.0.1:8080/admin/revenue; không xuất session/cookie. Không dừng ứng dụng của người dùng.

Không còn blocker đối với các kịch bản đã chạy. Các mục NOT TESTED vẫn giữ nguyên giới hạn nêu trên, đặc biệt cách ly WebSocket với USER thứ hai và phân trang nhiều trang. Không tạo thêm đơn hoặc xin thêm quyền chỉ để biến các mục này thành PASS. Nhóm có thể đối chiếu hai mã đơn và stock44; các giao dịch vẫn tồn tại dù Cart đã khôi phục.
