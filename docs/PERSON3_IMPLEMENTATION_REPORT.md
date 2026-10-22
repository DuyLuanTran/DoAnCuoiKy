# BÁO CÁO TRIỂN KHAI NGƯỜI 3 — SHOPTECH

## Trạng thái hiện tại

- Giai đoạn: **Đối chiếu audit → sửa → regression H2 và kiểm thử nghiệp vụ Supabase thực tế đã thực hiện**; giới hạn NOT TESTED được ghi riêng.
- Branch bàn giao được người dùng chỉ định: `feature/cart-checkout`, đổi tên từ `feature/khoa-person3`, nền `dev` tại `35b048d`.
- Worktree ban đầu sạch, detached HEAD tại bản skeleton `6e05931`; đã chuyển đúng nền dev trước triển khai. Không thay đổi checkout của thành viên khác.
- Build: **PASS** Java 26.0.2.1, Maven Wrapper 3.9.16, source target 17.
- Test cuối trong lượt QA 22/10/2026: **61 PASS, 0 FAIL/ERROR; 31 PostgreSQL SKIPPED/NOT TESTED** (92 test được phát hiện). Mốc audit trước là 59 PASS; xem báo cáo demo cho minh chứng mới.
- Kịch bản QA H2: **52 PASS / 1 FAIL P3** (asset ajax-loader.gif thiếu). Xem [PERSON3_DEMO_TEST_REPORT.md](PERSON3_DEMO_TEST_REPORT.md). Supabase thật đã kiểm thử nghiệp vụ được phép, xem [PERSON3_SUPABASE_TEST_REPORT.md](PERSON3_SUPABASE_TEST_REPORT.md); bộ PostgreSQL tạo schema cô lập vẫn **NOT TESTED**.
- Blocker: không còn blocker triển khai. Cần nhóm xác minh trên PostgreSQL test riêng trước tích hợp môi trường dùng chung.
- Người dùng đã cho phép stage/commit/push phần Người 3 lên nhánh feature riêng. Không merge, tạo PR, force push hoặc thay đổi Supabase trong bước bàn giao Git.
- Source/test/build và ba báo cáo Markdown được chọn cho commit. Toàn bộ ảnh/video, JSON/TXT/XML/log QA và ZIP được giữ local, không đưa lên GitHub; các liên kết evidence trong báo cáo chỉ dùng cho bản local.
- Bước tiếp theo sau bàn giao: nhóm review feature và chạy PostgreSQL cô lập khi có môi trường; giữ nguyên các mục NOT TESTED.

### Kiểm tra trước bàn giao Git — 22/10/2026

- Remote dev xác minh trực tiếp bằng ls-remote là 35b048d087275991d468fabc26708463f4136021. Lịch sử Word có commit 28810376ff0cdd6b569188a990475cb44c360f40 là tổ tiên của dev, đã chứa bốn giá trị cấu hình nhạy cảm trùng .env hiện tại. Secret cũ đã tồn tại trên remote trước Người 3; không in giá trị, không sửa/xóa Word hoặc rewrite history.
- Quét các file Người 3 được chọn, báo cáo Markdown và untracked/evidence: không phát hiện secret runtime mới, JWT literal, private key hoặc GitHub token. Cấu hình test chứa giá trị giả H2 và signing key công khai chỉ dành cho test cô lập; cấu hình production dùng biến môi trường. Đây không phải kết luận repository/lịch sử hoàn toàn an toàn.
- Chạy mới Maven Wrapper compile test trước commit: BUILD SUCCESS, 61 PASS, 0 FAIL/ERROR, 31 PostgreSQL SKIPPED (92 tests). Biến kích hoạt PostgreSQL suite được bỏ trong subprocess; test dùng config H2 riêng, không kết nối Supabase.
- Secret cũ cần nhóm xử lý riêng: thu hồi/rotate credential liên quan và thống nhất phương án lịch sử Git; việc push nhánh feature không khắc phục nguy cơ đã tồn tại.

| Module | Trọng số | Kết quả |
|---|---|---|
| Cart | 0,75 | Backend/API/UI AJAX; unit, integration và browser PASS |
| Checkout & History | 0,75 | COD, transaction, locking, snapshot, idempotency; test/browser PASS |
| Admin Orders | 0,75 | Search/filter/paging, detail, state machine, conflict/restock; test/browser PASS |
| Revenue | 0,5 | Aggregate database, ngày/tháng, Chart.js; test/browser PASS |
| Order WebSocket | Bonus | JWT/Origin, riêng chủ đơn, after-commit; socket thật và browser PASS |
| Chatbot AI | 0,25 | Chưa triển khai — ngoài phạm vi |

Không tự xác nhận điểm chấm thực tế hoặc tính tương đương hoàn toàn giữa H2 và PostgreSQL.

## Ghi nhận từng giai đoạn

| Giai đoạn | Điều kiện đạt và bằng chứng |
|---|---|
| 0 Audit | Đọc source dev, schema/data/guide, auth/catalog/layout; compile nền PASS. Wrapper lỗi Target[0] khi .m2 không phải symlink, đã sửa và chạy lại thành công. |
| 1 Cart | 13 unit Cart và 5 integration ban đầu PASS: owner, validation, CSRF, IDOR, đồng thời, API, render. |
| 2 Checkout | Compile + 25 test PASS ở thời điểm nghiệm thu: snapshot, đổi giá/kho, rollback, double-submit, hai người tranh tồn kho 1, history. |
| 3 Admin | 16 integration PASS ở thời điểm nghiệm thu: ADMIN/USER, tìm kiếm/paging, detail, terminal, stale update và hủy đồng thời. |
| 4 Revenue | 19 integration PASS ở thời điểm nghiệm thu: sum/count, ngày/tháng, biên giờ Việt Nam, empty, quyền, render/API. |
| 5 Notification | 24 test PASS ở thời điểm nghiệm thu, gồm 5 test qua WebSocket/HTTP server thật và H2 riêng. |
| 6 Final | Clean compile + toàn bộ 46 test PASS; JavaScript syntax, browser E2E, responsive, diff và rà soát secret hoàn tất. |

Số test từng giai đoạn là lượt chạy thực tế lúc đó, không cộng dồn thành tổng cuối.

## Quyết định kiến trúc và dữ liệu

- Tái sử dụng JWT cookie/Principal/SecurityContext, User/Product/Category, repository và layout hiện có; không tạo đăng nhập hoặc catalog mới.
- Cart mutation khóa hàng User trước tìm/tạo Cart, kể cả khi chưa có giỏ. Dùng UNIQUE carts.user_id và UNIQUE cart_items(cart_id, product_id) trong schema đã có. Không trừ tồn kho khi thêm giỏ.
- Giá phía server dùng discountPrice khi khác null, theo quy tắc UI hiện có; BigDecimal cho tiền. Quantity nhận dạng decimal và validate phần thập phân bằng 0 trước intValueExact, tránh JSON 1.5 bị ép thành 1.
- Checkout khóa User rồi các Product theo ID tăng dần; refresh Product dưới pessimistic lock vì Cart graph có thể đã nạp giá/kho cũ. Snapshot, trừ kho, lưu đơn và xóa giỏ cùng transaction; không gọi dịch vụ ngoài.
- UUID 128 bit được encode base64url đầy đủ vào order_code dài 26, dùng UNIQUE sẵn có làm idempotency bền vững. Retry cùng key trả đơn đã tạo của owner. Frontend giữ key qua lỗi mạng/reload, tách theo username; không dùng biến frontend làm bảo đảm duy nhất. Key đã dùng cho đơn xóa mềm sẽ conflict, không tạo bản sao.
- COD, phí giao hàng 0 VND hiển thị rõ; không giả lập online payment. COD COMPLETED được Admin xác nhận đã thu tiền và ghi PAID.
- State machine PENDING → CONFIRMED → SHIPPING → COMPLETED; PENDING/CONFIRMED có thể CANCELLED nếu COD UNPAID. Terminal không chỉnh sửa. Không tạo chức năng xóa đơn.
- Admin update khóa Order, so expectedStatus để trả 409 khi stale. State machine không có chu trình nên không cần cột version. Hủy khóa Product theo ID tăng dần và hoàn kho trong cùng transaction, đúng một lần; không khóa ngược User.
- Revenue chỉ COMPLETED và is_deleted=false; không bắt buộc PAID để tương thích dữ liệu cũ. Đếm đơn hoàn tất trong kỳ, không phải tất cả đơn. Aggregate theo ngày tại DB, nhóm tháng từ tối đa 366 aggregate ngày; không join detail.
- Múi giờ Asia/Ho_Chi_Minh, khoảng [đầu ngày từ, đầu ngày sau ngày đến). Do schema không có completed_at, ghi nhận theo created_at và nêu rõ trên UI.
- WebSocket gốc **/ws/order-status**, thay đề xuất STOMP. Server tự gắn phiên với Principal; không nhận destination/userId từ client. Client gửi text/SUBSCRIBE/SEND bị đóng 1008. Không dùng /user/queue/order-status vì không dùng STOMP.
- Handshake yêu cầu JWT cookie hợp lệ, Principal khớp và Origin đúng APP_PUBLIC_BASE_URL; không token query/wildcard. Kiểm tra hạn JWT trước khi gửi. CSRF HTTP giữ nguyên, socket chỉ server gửi dữ liệu và không thực hiện mutation từ client.
- Event OrderStatusChanged trong transaction, listener AFTER_COMMIT; DTO chỉ id/code/status; lỗi transport được cô lập. Toast/status update và reconnect backoff 1–30 giây; reload lấy DB.
- Product.image_gallery đổi khai báo text[] thành text array, cùng kiểu trên PostgreSQL và tương thích H2. Không sửa SQL schema.
- Thay checkout mẫu bằng dữ liệu thật, giữ Electro CSS. Trang admin tổng quan dùng admin layout cũ, bỏ số liệu/biểu đồ demo và liên kết sang Revenue duy nhất. Trang index mẫu dẫn chọn sản phẩm sang catalog thật, bỏ giỏ demo.

### Database và môi trường chung

Bảng dùng: users, products, categories, carts, cart_items, orders, order_details; roles/users_roles cho fixture/auth hiện có. Không dùng chat_messages.

**Không kết nối/ghi Supabase, không chạy schema.sql/data.sql, không migration, không đổi .env hoặc cấu hình cá nhân.** Chưa xác minh schema live. Không cần migration theo schema trong repository. Test chỉ nạp application-test.properties, không import application.properties/.env; dùng H2 và halt_on_error để không bỏ qua lỗi tạo schema.

Nếu nhóm tự khởi động source thường với Supabase chỉ đọc, phải đặt SPRING_JPA_HIBERNATE_DDL_AUTO=none và xác nhận cấu hình thực tế; báo cáo này không ghi nhận đã chạy ứng dụng với Supabase.

## File thay đổi

Các đường dẫn dưới đây tính từ root repository; package Java là shoptech/src/main/java/com/example/shoptech.

| Nhóm | File mới và mục đích |
|---|---|
| Entity | entity/Cart.java, CartItem.java, Order.java, OrderDetail.java, OrderStatus.java — ánh xạ bảng và trạng thái |
| Repository | repository/CartRepository.java, CartItemRepository.java, OrderRepository.java — owner, paging, khóa đơn, aggregate |
| DTO | dto/CartDto.java, OrderDto.java, OrderStatusNotification.java — validation và response không expose Entity |
| Service | service/CartService.java, OrderService.java, AdminOrderService.java, RevenueService.java, OrderNotificationService.java, OrderStatusChanged.java |
| Web | controller/CartController.java, OrderController.java, AdminOrderController.java, RevenueController.java, CommerceExceptionHandler.java; config/WebSocketConfig.java |
| User UI | resources/templates/user/cart.html, orders.html, order-detail.html; resources/templates/fragments/commerce.html |
| Admin UI | resources/templates/admin/orders.html, order-detail.html, revenue.html |
| JS dùng chung | resources/static/user/js/commerce.js, checkout.js, order-notifications.js; resources/static/admin/js/orders.js, revenue.js |
| Test | shoptech/src/test/java/com/example/shoptech/CommerceIntegrationTest.java, WebSocketNotificationTest.java, Person3TestApplication.java; service/CartServiceTest.java, OrderNotificationServiceTest.java; PostgresCommerceIntegrationTest.java; shoptech/src/test/resources/application-test.properties |
| Báo cáo | docs/PERSON3_IMPLEMENTATION_REPORT.md |

File tracked đã chỉnh:

- shoptech/mvnw.cmd: sửa kiểm tra symlink null, không đổi bản Maven.
- shoptech/src/main/resources/application.properties: mặc định ddl-auto=none, giữ sql.init.mode=never; không đổi thông tin kết nối.
- shoptech/src/main/resources/static/user/js/main.js: guard price inputs/slider vắng mặt, sửa TypeError trên catalog/detail.
- shoptech/pom.xml: H2 test scope và spring-websocket thực dùng; không đổi phiên bản Spring Boot/Lombok.
- **Có thể conflict Người 1/2:** config/SecurityConfig.java (endpoint và API 401), entity/Product.java (ARRAY alias), repository/UserRepository.java và ProductRepository.java (lock query).
- **Có thể conflict UI nhóm:** resources/templates/admin/index.html, fragments/admin-layout.html; user/checkout.html, home.html, account.html, index.html, product.html, store.html.
- shoptech/src/test/java/com/example/shoptech/ShoptechApplicationTests.java: context test cố định cấu hình H2, tránh database chung.

Không đổi AuthController, JWT service/filter, Cloudinary/SMTP service hoặc CRUD sản phẩm/danh mục/người dùng.

## API và Security

| Method | Endpoint | Quyền / công dụng |
|---|---|---|
| GET | /cart, /api/cart | Đăng nhập; giỏ của Principal |
| POST | /api/cart/items | Đăng nhập + CSRF; thêm/gộp sản phẩm, 201 |
| PATCH | /api/cart/items/{id} | Đăng nhập + CSRF; sửa số lượng thuộc giỏ mình |
| DELETE | /api/cart/items/{id} | Đăng nhập + CSRF; xóa dòng thuộc giỏ mình |
| GET | /checkout | Đăng nhập; form COD |
| POST | /api/orders | Đăng nhập + CSRF; checkout hoặc trả đơn cũ với cùng requestKey |
| GET | /orders, /orders/{id} | Đăng nhập; history phân trang / detail có owner filter |
| GET | /admin/orders, /admin/orders/{id} | ADMIN; search/status/page/detail |
| PATCH | /admin/orders/{id}/status | ADMIN + CSRF; expectedStatus và status, conflict 409 |
| GET | /admin/revenue, /admin/revenue/data | ADMIN; from/to/group DAY hoặc MONTH |
| WS GET upgrade | /ws/order-status | JWT cookie + đúng Origin; chỉ nhận thông báo của Principal |

API mới trả DTO/ProblemDetail, không tin userId/price/total do client gửi. Anonymous /api trả 401, trang HTML chuyển login; USER gọi admin bị 403. Validation 400, không thấy/không sở hữu 404, nghiệp vụ stale/stock 409. Không dùng GET để mutation, không tắt CSRF/Security.

## Kiểm thử cuối

Lệnh tại shoptech, JAVA_HOME chỉ đặt cho phiên terminal:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26.0.2.1'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean compile test
```

| Suite | PASS | FAIL/ERROR/SKIP |
|---|---:|---:|
| CartServiceTest | 13 | 0 |
| CommerceIntegrationTest | 30 | 0 |
| WebSocketNotificationTest | 7 | 0 |
| OrderNotificationServiceTest | 3 | 0 |
| JwtServiceTest hiện có | 3 | 0 |
| ShoptechApplicationTests | 3 | 0 |
| PostgresCommerceIntegrationTest | 0 | 31 SKIPPED / NOT TESTED |
| Tổng | **59** | **0 fail/error, 31 skip** |

Log cuối: shoptech/target/audit-final-verification.log; JUnit XML: shoptech/target/surefire-reports. Không stage log build.

- PASS: concurrent first-cart merge, owner/IDOR, validation số lượng kể cả fractional, tồn kho/soft-delete/inactive, server discount.
- PASS: checkout thành công/empty, validation, snapshot giá thay đổi, trừ kho/xóa giỏ, rollback khi dòng sau lỗi; double-submit đồng thời một đơn; hai User tranh kho 1 chỉ một thành công.
- PASS: Admin quyền, tìm kiếm/paging/detail, invalid transitions/terminal, stale conflict, hủy đồng thời hoàn kho một lần.
- PASS: Revenue sum/count, ngày/tháng, biên múi giờ đổi ngày/tháng, empty/invalid range, loại pending/cancelled/deleted, USER 403, không join detail.
- PASS: WebSocket JWT/Origin, riêng owner, chặn client chọn đích, rollback không gửi, disconnect/reconnect, JWT hết hạn không nhận và transport lỗi không phá Order.
- PASS regression MockMvc: homepage, catalog/detail/search/filter/page request, login/logout JWT cookie thật, USER/ADMIN, các trang Admin User/Category/Product. Chỉ xác minh render/quyền với các module CRUD cũ, không tuyên bố kiểm thử toàn bộ thao tác CRUD/upload/mail.
- PASS: node --check cho 5 JS mới và main.js đã sửa; git diff --check; không có nội dung staged.

### Browser E2E đã thực hiện

Server test localhost:18080, H2 person3-browser tạm, fixture giả. Đã dừng server và đóng tab test.

1. Login USER; thêm qua catalog AJAX, badge 1; sửa lên 2, tổng 28.000.000 VND.
2. Checkout COD, phí 0, đơn PENDING snapshot 2 sản phẩm; giỏ về 0.
3. Login ADMIN; mở list/detail; đổi CONFIRMED → SHIPPING → COMPLETED; payment PAID, form terminal biến mất.
4. Tab User đang mở nhận toast và cập nhật CONFIRMED ngay; tải history lại thấy COMPLETED từ DB.
5. Revenue ngày/tháng hiển thị 28.000.000 VND, 1 đơn; Chart.js render, không có console error trên trang Revenue. Kiểm tra responsive tại 390×844.
6. Thêm rồi xóa CartItem bằng UI, empty state; phát hiện Electro ghi đè hidden của nút checkout, sửa CSS và browser xác nhận nút đã ẩn khi giỏ trống.

Ảnh bằng chứng: ngoài repository, trong thư mục visualization của chat, tên shoptech-revenue-test.jpg. Chỉ chứa dữ liệu giả.

### NOT TESTED / giới hạn còn lại

- PostgreSQL test riêng, schema/trigger live Supabase, concurrency trên PostgreSQL thật; H2 không thay thế xác minh đặc thù PostgreSQL.
- Gửi mail SMTP, upload Cloudinary, toàn bộ thao tác CRUD cũ; không gọi dịch vụ ngoài trong test.
- Hạ tầng reverse proxy/TLS nhiều instance. Socket registry nằm trong bộ nhớ một instance; không thêm broker ngoài theo yêu cầu. Notification không có lưu hàng đợi offline; DB là nguồn chuẩn khi reload.
- Chỉ kết nối socket đã xác thực nhận dữ liệu đến hạn JWT; có kiểm tra expiry và trạng thái tài khoản trước gửi. Socket của tài khoản bị khóa/xóa được đóng khi có notification tiếp theo; không có scheduler đóng idle socket ngay tại thời điểm khóa.
- Revenue phân bổ ngày tạo đơn, không thể báo cáo theo ngày hoàn thành chính xác nếu chưa có completed_at.

## Hướng dẫn test thủ công an toàn

1. Dùng launcher test **Person3TestApplication** trong src/test: luôn ép datasource H2 trong bộ nhớ, bind loopback, không đọc .env, không dùng Supabase. Dữ liệu sẽ mất khi dừng ứng dụng.
2. Tự đặt biến môi trường PERSON3_TEST_PASSWORD trong terminal cho phiên test, tối thiểu 12 ký tự; không lưu vào source/báo cáo. Tên fixture USER/ADMIN xem trong launcher test; báo cáo không ghi thông tin đăng nhập.
3. Chạy tại shoptech:

```powershell
.\mvnw.cmd test-compile spring-boot:test-run '-Dspring-boot.test-run.main-class=com.example.shoptech.Person3TestApplication'
```

4. Mở http://localhost:18080/products. Login USER test, thêm sản phẩm; thêm lại để gộp, sửa/xóa dòng; thử quantity 0 và vượt kho. Xác nhận thông báo, badge và tổng tiền.
5. Checkout COD bằng thông tin giả; thử submit lặp; xem /orders và detail. Kiểm tra sản phẩm giảm kho, giỏ rỗng, snapshot không đổi khi giá sản phẩm được sửa trên DB test.
6. Dùng trình duyệt/profile khác cho ADMIN để giữ phiên User riêng. Vào /admin/orders, tìm theo mã/tên khách, lọc trạng thái, xem detail. Tiến hành từng trạng thái hợp lệ; thử hủy đơn PENDING/CONFIRMED COD chưa trả tiền. Kiểm tra hoàn kho một lần và terminal không sửa.
7. Mở cùng đơn trên hai tab Admin; tab thứ hai dùng dữ liệu cũ phải nhận conflict yêu cầu tải lại.
8. Giữ tab User detail mở, Admin đổi trạng thái; User nhận toast/status tức thì. Tài khoản khác không nhận. Ngắt kết nối rồi tải lại, trạng thái phải theo DB.
9. Mở /admin/revenue, lọc ngày/tháng, đối chiếu tổng đơn COMPLETED và thử kỳ rỗng. USER truy cập admin/revenue phải 403.
10. Kiểm tra lại homepage/catalog/search/category/page/login/logout và các trang Admin cũ. Chỉ thử ghi trên H2 hoặc PostgreSQL test riêng được nhóm cấp; không dùng tài khoản/stock Supabase thật.
11. Dừng launcher bằng Ctrl+C, xóa biến mật khẩu test khỏi phiên terminal. Nếu cần PostgreSQL, chuẩn bị môi trường cô lập riêng, không chạy schema/data lên Supabase chung.

## Điểm mở rộng tương lai

Order Notification có DTO/event/service riêng, đường /ws/order-status riêng; WebSocketConfig có thể đăng ký handler/endpoint khác. Module Chatbot sau này tự thêm xác thực/handler/service riêng, không cần sửa Cart, Order hoặc Revenue để phục vụ chat. Không tạo AI provider, chat handler/widget, dependency AI hoặc API giả.

## Git và secret cuối cùng

- Đã chạy git status --short, git diff --stat, git diff --check và kiểm tra staged diff rỗng. git diff --stat chỉ tính 17 tracked files; 43 file mới chưa stage được liệt kê riêng ở status.
- Rà soát file thay đổi theo các từ khóa credential/key/JDBC: chỉ có tên biến môi trường, code auth có sẵn và giá trị giả trong test. Không thêm secret thật vào source/báo cáo; giữ nguyên cấu hình môi trường cá nhân.
- Không git add, commit, push, pull, rebase, merge, reset, PR hoặc thao tác remote.
- Không ghi đè thay đổi chưa commit của nhóm (worktree sạch trước triển khai).
- Không thay đổi database Supabase chung; không Chatbox/Chatbot.

## Đối chiếu toàn bộ SHOPTECH_PERSON3_AUDIT.md — kết quả mới nhất

Đã đọc toàn bộ audit từ tệp người dùng cung cấp và kiểm tra lại từng nhận định trên worktree hiện tại. Phần “chỉ đọc” trong báo cáo mô tả phạm vi của người audit trước đó; yêu cầu hiện tại của người dùng cho phép sửa lỗi đã xác minh. Không đổi branch và không ghi đè các thay đổi đã có.

| Nhận định audit | Phân loại sau xác minh | Hành động / bằng chứng |
|---|---|---|
| P1 Revenue alias orderCount trên PostgreSQL | **Rủi ro cần kiểm thử PostgreSQL; chưa tái hiện lỗi mapping** | Test thật qua Hibernate Tuple có alias chính xác lowercase ordercount vẫn map getter getOrderCount() thành 1; repository projection không null. Giữ SQL đang hoạt động, không sửa theo suy đoán. Phép thử H2 không chứng minh driver/PostgreSQL thật. |
| P1 thiếu PostgreSQL transaction/concurrency | **Rủi ro cần kiểm thử** | Không có psql/postgres/pg_ctl/docker local trong môi trường kiểm tra, không có URL test được cấp. Thêm bộ test opt-in dùng lại 30 commerce tests + 1 test schema/constraint/ARRAY trên PostgreSQL. Cả 31 NOT TESTED. Không tự cài DB/Docker hoặc dùng Supabase để thử. |
| P2 trang chủ template mẫu | **Nhận định đúng, sửa phần thuộc Người 3** | Nối footer Giỏ hàng, Lịch sử đơn, Đơn hàng, Tài khoản đến endpoint thật. Không biến đổi toàn bộ catalog/trang chủ của Người 2; sản phẩm/giá mẫu, wishlist/compare/newsletter ngoài phạm vi vẫn còn, không claim homepage đã hoàn toàn dùng DB. |
| P2 socket vẫn nhận sau khi khóa/xóa User | **Lỗi đã xác nhận và sửa** | Hai test socket thật trước sửa nhận CONFIRMED dù User vừa khóa hoặc soft-delete (FAIL tái hiện). Sau sửa, không nhận dữ liệu và socket đóng 1008 (PASS). |
| P2 ddl-auto=update ở cấu hình thường | **Cấu hình không an toàn đã xác nhận và sửa** | Default đổi none, giữ sql.init.mode=never. Test ConfigData đọc cấu hình thật không nạp .env, xác nhận none/never mà không tạo datasource. Worktree không có .env và terminal không có override DDL tại thời điểm kiểm tra. Không khởi động với Supabase. |
| P3 doanh thu theo created_at của COMPLETED | **Giới hạn đúng thiết kế, không phải bug mới** | Không có completed_at trong schema; giữ rule và nhãn UI, không thêm cột. |
| P3 raw WebSocket khác STOMP | **Cảnh báo không áp dụng như lỗi chức năng** | /ws/order-status độc lập, server-only, JWT/Origin/owner đã test. Giữ kiến trúc, không thêm chatbot hoặc broker. |
| P3 offline/single instance | **Giới hạn còn phù hợp** | Không có offline queue/multi-instance registry; reload lấy DB. Không tự mở rộng hạ tầng. |
| P3 SMTP/Cloudinary/toàn bộ CRUD cũ | **NOT TESTED cho dịch vụ ngoài và full CRUD** | Chạy regression render/quyền/login/logout/catalog/search/filter, giữ nguyên service liên quan; không gọi API ngoài. |
| Các lớp/nghiệp vụ tồn tại, không chatbot, 46 test cũ | **Xác nhận hiện trạng/historical evidence** | Source các module có thật. Bộ test hiện tại chạy lại 59 PASS, 31 PostgreSQL chưa chạy; không dùng XML cũ để tự kết luận lần chạy mới. |

### Sửa lỗi khác tìm thấy khi đối chiếu

- Nút “Tiếp tục mua sắm” ở User Home/Account trỏ /, trong khi AuthController chuyển User đã đăng nhập từ / về trang Home: luồng mua sắm bị lặp. Chỉ đổi href sang /products, giữ controller xác thực và thiết kế cũ. MockMvc render + browser xác nhận từ login → Home → CTA → catalog đúng.
- main.js Electro gọi addEventListener trên price-min/price-max không tồn tại ở catalog/detail. Browser tái hiện TypeError tại main.js:131. Thêm guard phần tử/slider tồn tại; node syntax PASS, browser mới không còn console error ở catalog/detail; thêm giỏ từ detail rồi mở Cart vẫn PASS. Không thay nghiệp vụ lọc/catalog.
- Notification revalidation dùng một query exists cho mỗi owner/event, không phải mỗi socket. Repository mở transaction read-only REQUIRES_NEW ngắn, tránh persistence context cũ từ Order và kết thúc trước I/O socket. DB read lỗi thì bỏ gửi, không ném lỗi ngược nghiệp vụ Order (unit test PASS).
- Không sửa transaction/locking/state machine đang hoạt động khi chưa có lỗi tái hiện. Bổ sung test thay vì viết lại service.

### Kiểm thử bổ sung sau audit

- PASS: alias lowercase qua Hibernate Tuple và actual repository projection (H2); không dùng Map giả làm bằng chứng PostgreSQL.
- PASS: replay key cũ sau khi thêm giỏ mới không tiêu thụ giỏ mới hoặc trừ kho lần hai; cùng key từ owner khác bị unique conflict và rollback stock/cart.
- PASS: hai key checkout khác nhau đồng thời trên cùng giỏ chỉ tạo một Order; checkout refresh đúng giá/kho khi Product đã nằm trong persistence context từ trước.
- PASS: hủy từ CONFIRMED, chặn hủy PAID; lỗi hoàn kho ở dòng sau rollback cả dòng đã hoàn trước đó và giữ trạng thái Order.
- PASS: socket đang mở sau khóa/soft-delete bị chặn, lỗi kiểm tra quyền không phá Order; các test JWT/Origin/owner/rollback/reconnect cũ vẫn PASS.
- PASS: default DDL/SQL init an toàn, guard test PostgreSQL từ chối host Supabase, database khác, URL có credential/options.
- PASS: link mua sắm Home/Account bằng HTML render, browser catalog/detail/Cart; giữ Electro/Admin và không chỉnh auth service.
- Build cuối: .\mvnw.cmd clean compile test → BUILD SUCCESS; 90 detected = 59 PASS + 31 PostgreSQL SKIPPED, 0 failure/error. Không dùng skipTests. Tất cả test thực thi dùng H2, socket test dùng HTTP server thật.
- Đã chạy node --check cho 6 JS liên quan; git diff --check PASS. Không staged. Server browser H2 đã dừng, tab test đã đóng. Ảnh mới: shoptech-audit-cart.jpg trong visualization của chat, chỉ dữ liệu giả.

### Cách chạy PostgreSQL test cô lập khi môi trường được cấp

Bộ PostgresCommerceIntegrationTest đã compile nhưng **chưa chạy trên PostgreSQL**. Cần một PostgreSQL local thử nghiệm có database riêng tên shoptech_person3_test, role test có quyền tạo schema trong database đó. Không sử dụng database chung hoặc endpoint Supabase.

1. Tự đặt PERSON3_POSTGRES_TEST_URL, PERSON3_POSTGRES_TEST_USER, PERSON3_POSTGRES_TEST_PASSWORD cho phiên terminal, không ghi giá trị bí mật vào file/source/báo cáo.
2. URL chỉ cho phép JDBC PostgreSQL loopback (localhost/127.0.0.1/IPv6 loopback), database shoptech_person3_test; không userinfo hoặc query options. Guard chạy trước mọi kết nối. Thiếu URL thì JUnit đánh dấu 31 test SKIPPED, không có fallback về cấu hình Supabase.
3. Chạy tại shoptech:

```powershell
.\mvnw.cmd '-Dtest=PostgresCommerceIntegrationTest' test
```

4. Bộ test tạo schema ngẫu nhiên person3_<UUID>, nạp schema.sql của repo sau khi loại toàn bộ DROP TABLE/VIEW và CREATE EXTENSION; không chạy data.sql. Hibernate dùng validate, không update/create. Các fixture/test chỉ ở schema đó. Cleanup chỉ DROP đúng schema ngẫu nhiên do bộ test tạo sau khi kiểm tra lại target; không drop database hoặc schema public.
5. Bao gồm alias repository, timezone, thật sự có unique constraint, PostgreSQL ARRAY, checkout/rollback/idempotency, concurrent stock, Admin cancel/conflict và regression dùng cùng bộ test gốc. Cần xem kết quả PostgreSQL thực tế, bao gồm schema validation với các Entity hiện có, trước khi kết luận tương thích.
6. Nếu PostgreSQL chưa có hoặc startup/validation chưa chạy được, giữ trạng thái NOT TESTED; không đổi sang Supabase để lấy kết quả PASS.

Kết luận phạm vi an toàn: đã khắc phục các lỗi/cấu hình xác nhận được, giữ nghiệp vụ đã test tốt, chuẩn bị bộ kiểm thử PostgreSQL và ghi rõ giới hạn. Chưa tự xác nhận production-ready hoặc tích hợp dev. Không add/commit/push/merge/PR, không thay đổi Supabase, không Chatbox/Chatbot.


## Lượt QA minh chứng 22/10/2026

- Chi tiết 53 case và artefact: [PERSON3_DEMO_TEST_REPORT.md](PERSON3_DEMO_TEST_REPORT.md). Browser/API/H2: 48 PASS + 1 FAIL; W06/W07/X01/X02 lấy từ JUnit thực thi mới, thêm 4 PASS. Không cộng các số macro-case vào tổng JUnit.
- Chạy mới clean compile test: 61 PASS, 31 PostgreSQL SKIPPED, 0 failure/error. Hai test mới: expired signed JWT bị chặn tại handshake thật và snapshot QA không tồn tại trong application thường.
- FAIL còn lại: G04/P3, /user/css/ajax-loader.gif trả 404, slick-theme.css:5 tham chiếu file không có. Không sửa CSS/asset chung trong vai trò QA. Các negative HTTP400/401/403/404/409 khác có chủ ý.
- Person3TestApplication thêm fixture đối chứng và snapshot JDBC chỉ đọc, chỉ trên H2 test; QA runner ở src/test/qa/person3-demo.cjs. Không thay code production trong lượt QA.
- Ảnh/video2fps và JSON/assertion/JUnit đã lưu docs/person3-demo-evidence/verified-run. Mật khẩu chỉ trong RAM/environment phiên, không lưu credential/JWT vào artefact. Các tab/process test đã dừng, cleanup.json xác nhận port18080 trống.
- Không dùng PASS cũ, không đổi branch/Git stage/commit/push/merge/PR và không chạm Supabase.

## Kiểm tra trước phiên Supabase chung — 22/10/2026

- Giai đoạn hiện tại: khảo sát chỉ đọc, chờ người dùng tự đăng nhập USER tại tab http://localhost:8080/login. Chưa dùng thông tin xác thực đã cung cấp; không lưu credential, JWT hoặc session cookie.
- HTTP GET localhost:8080 trả 200; /products hiển thị 9 sản phẩm, trang chi tiết truy xuất thành công. Source ProductCatalogController truy vấn service/repository cho danh sách/chi tiết. Kết nối truy vấn dữ liệu ứng dụng hoạt động; danh tính database runtime/Supabase chính xác chưa được xác minh độc lập, không suy ra từ HTTP 200.
- Sản phẩm mẫu đề xuất: ID 6, Chuột không dây công thái học Logitech MX Master 3S, SKU LOGI-MX3S-BLK. Giá gốc 2.490.000 VND, giá bán 2.090.000 VND, tồn kho trước thao tác ghi 45 (DOM input quantity max=45). ID lấy từ data-add-product=6 trên trang chi tiết; tên/SKU/slug/giá/tồn kho khớp db/data.sql của nhóm. Đây là bằng chứng sản phẩm có trong seed, không chứng minh chưa từng được nhóm sử dụng. Phải đọc lại tồn kho ngay trước checkout.
- Cấu hình source: ddl-auto=none, sql.init.mode=never. Không sửa/đọc xuất nội dung .env; chưa xác minh override runtime. Không chạy integration test trên database chung.
- Build/test phiên Supabase: NOT TESTED; cart/checkout/admin/revenue/WebSocket trên database chung: NOT TESTED. Chưa tạo mã đơn, chưa thực hiện thao tác ghi nghiệp vụ hoặc thay đổi tồn kho.
- Chỉ cập nhật báo cáo trong bước này; không sửa source/branch, không stage/commit/push/merge. Bước tiếp theo: người dùng tự đăng nhập, xác minh quyền và dữ liệu runtime an toàn; sau đó thực hiện kịch bản trên sản phẩm mẫu được xác định và ghi lại mã đơn/mọi thay đổi.

### Kết quả kiểm thử Supabase thực tế sau checkpoint — 22/10/2026

- Báo cáo hiện hành: [PERSON3_SUPABASE_TEST_REPORT.md](PERSON3_SUPABASE_TEST_REPORT.md), ảnh và SELECT read-only tại docs/person3-supabase-evidence. Đã xác thực tài khoản nhóm nguyenvana/admin và khớp endpoint runtime với .env; không dùng fixture H2.
- Đơn6 ORD-Vj9ZtXtSRG-jZ4qUpm-7dw: COD product6×1, COMPLETED/PAID, stock45→44. Đơn7 ORD-yf46bII8S_-ndRizDgNjqA: CONFIRMED→CANCELLED/UNPAID, stock44→43→44. Cả hai đơn giữ nguyên, không xóa để dọn dữ liệu.
- PASS thực tế: Cart mutation/validation UI, checkout/snapshot/history/detail, Admin state flow, COD PAID, hủy sau CONFIRMED/hoàn kho, WebSocket owner nhận status/toast không reload, Revenue ngày22/10 và toàn tháng10 =2.090.000/1 đơn, khớp PostgreSQL SELECT read-only. Projection orderCount trả count1 thật trên PostgreSQL.
- Cart cuối product5×1 và6×1, tổng4.280.000; CartItem ID9/10 thay checkpoint2/7. Kho cuối product6=44. Khôi phục Cart không hoàn tác các giao dịch đã tạo.
- NOT TESTED: WebSocket với USER khác, phân trang nhiều trang, hủy trực tiếp PENDING/lặp/concurrency và backend invalid API trong phiên shared DB. Không lấy PASS H2 thay thế; không chạy31 PostgreSQL schema test, không sửa source/.env/branch hoặc Git ghi. Không còn blocker cho kịch bản đã thực hiện.
