# Báo cáo kiểm thử/demo Người 3 — ShopTech

Ghi chú bàn giao Git: báo cáo Markdown được lưu trên nhánh feature/cart-checkout; các ảnh/video/JSON/XML/log evidence được giữ local và chủ động loại khỏi commit. Các liên kết evidence bên dưới cần bản local để mở. Những ghi nhận tên nhánh và quyền Git trong phiên QA là lịch sử tại thời điểm chạy.

**Phiên QA: 22/10/2026, Asia/Saigon. Tất cả kết quả bên dưới được chạy trong lượt yêu cầu này; không lấy PASS cũ thay cho test mới.**

## 1. Kết quả và môi trường

- 53 kịch bản QA: **52 PASS, 1 FAIL (P3 tài nguyên CSS thiếu)**. Trong đó browser/API/H2: 48 PASS + 1 FAIL; 4 case an toàn transaction/WebSocket lấy từ JUnit mới: PASS.
- Maven `clean compile test`: **BUILD SUCCESS; 61 PASS, 0 failure/error, 31 PostgreSQL SKIPPED/NOT TESTED** (92 tests được phát hiện). Tăng 2 test thực thi so với mốc 59: expired signed JWT handshake và endpoint snapshot không nạp trong application thường.
- Các bộ số trên có giao nhau về nghiệp vụ; không cộng 53 kịch bản với 61 JUnit thành số chức năng độc lập.
- Branch giữ nguyên `feature/khoa-person3`; source đầy đủ và thay đổi hiện có được bảo toàn. Không stage/commit/push/pull/merge/rebase/PR.
- Launcher `Person3TestApplication`, bind loopback cổng 18080. Log startup và JDBC runtime snapshot cùng xác nhận `jdbc:h2:mem:person3-browser`. Đã xác minh trước mutation; không nạp `.env` hoặc kết nối Supabase.
- Mật khẩu fixture ngẫu nhiên chỉ nằm trong RAM/environment của tiến trình test, không trong source/báo cáo/ảnh/HTTP log. Không lưu cookie, Authorization, CSRF hoặc JWT vào evidence.
- Playwright/Chrome/FFmpeg có sẵn; dùng 5 context tách cookie: USER, ADMIN, USER đối chứng, ADMIN thứ hai, anonymous. Không dùng tab chung cookie để giả lập hai tài khoản.
- Snapshot JDBC read-only `/admin/person3-test/snapshot` chỉ được đăng ký bởi test launcher, role ADMIN và kiểm tra lại URL H2. Unit test mới xác nhận bean này không tồn tại trong app thường. Không có endpoint QA trong production source.
- Server H2, Chrome headless và FFmpeg đã dừng; port 18080 không còn listener. [Bằng chứng cleanup](person3-demo-evidence/verified-run/cleanup.json).

| Module | PASS | FAIL | NOT TESTED |
|---|---:|---:|---:|
| Cart | 10 | 0 | 0 |
| Order | 9 | 0 | 0 |
| Admin | 11 | 0 | 0 |
| Revenue | 8 | 0 | 0 |
| WebSocket | 7 | 0 | 0 |
| Regression | 5 | 1 | 0 |
| Bổ sung | 2 | 0 | 0 |
| PostgreSQL suite riêng (test JUnit, không phải 53 macro-case trên) | 0 | 0 | 31 |

## 2. Xem minh chứng thực

- [Video USER → ADMIN → notification → Revenue](person3-demo-evidence/verified-run/person3-live-demo.mp4) — 13.000000 giây, 677,993 byte. Là các khung hình browser thật ở 2 fps truyền vào FFmpeg, chuyển góc nhìn USER/ADMIN; không phải video liên tục 30 fps. Tạm dừng ghi khi login và đổi viewport. Video có cả validation/kỳ thống kê rỗng nên số 0 trong một số khung là kịch bản âm; đối chiếu R03/R04 để xem doanh thu 28 triệu.
- [Contact sheet trích từ chính video](person3-demo-evidence/verified-run/video-contact-sheet.jpg); [Thông số ghi hình](person3-demo-evidence/verified-run/video-metadata.json); [FFprobe xác minh file](person3-demo-evidence/verified-run/video-probe.json).
- [Kết quả 53 case có assertion](person3-demo-evidence/verified-run/all-scenario-results.json); [HTTP response và snapshot JDBC](person3-demo-evidence/verified-run/http-and-db.json); [WebSocket frame theo từng context](person3-demo-evidence/verified-run/websocket-frames.json).
- [H2 trước mutation](person3-demo-evidence/verified-run/datasource-initial.json); [H2 sau các test](person3-demo-evidence/verified-run/final-db.json); [Console/pageerror](person3-demo-evidence/verified-run/console.json); [URL HTTP lỗi thực tế](person3-demo-evidence/verified-run/browser-response-errors.json).
- [Log Maven mới](person3-demo-evidence/verified-run/maven-clean-compile-test.log); [JUnit từng test](person3-demo-evidence/verified-run/junit-summary.json). Các XML trong thư mục `junit/` được trích nguyên testcase attributes/status từ báo cáo Surefire, bỏ properties/system-out để tránh ghi thông tin môi trường; SHA-256 bản XML nguồn nằm trong JSON summary.
- Script tái chạy: `shoptech/src/test/qa/person3-demo.cjs`. Dùng `node shoptech/src/test/qa/person3-demo.cjs` tại root; script từ chối server đã chiếm cổng 18080, tự sinh mật khẩu, xác minh H2, checkpoint và cleanup. Exit code 1 hiện là đúng vì G04 asset thiếu chưa sửa. Cần Playwright/Chrome/FFmpeg sẵn có.

### Ảnh các bước chính

- [Cart tăng/giảm và tổng tiền](person3-demo-evidence/verified-run/C05-cart.png)
- [Checkout trước submit](person3-demo-evidence/verified-run/O01-checkout.png)
- [Đơn COD vừa tạo](person3-demo-evidence/verified-run/O01-order-created.png)
- [Lịch sử đơn](person3-demo-evidence/verified-run/O08-history.png)
- [Admin Orders](person3-demo-evidence/verified-run/A01-admin-list.png)
- [Lọc trạng thái](person3-demo-evidence/verified-run/A03-pagination-filter.png)
- [COMPLETED/PAID và terminal](person3-demo-evidence/verified-run/A05-completed.png)
- [Revenue chưa hoàn tất](person3-demo-evidence/verified-run/R01-revenue-empty.png)
- [Chart theo ngày](person3-demo-evidence/verified-run/R03-daily-chart.png)
- [Chart theo tháng](person3-demo-evidence/verified-run/R04-monthly-chart.png)
- [USER nhận notification không reload](person3-demo-evidence/verified-run/W03-live-notification.png)
- [Cart 390×844](person3-demo-evidence/verified-run/G05-cart-mobile.png)
- [Revenue 390×844](person3-demo-evidence/verified-run/G05-revenue-mobile.png)

## 3. Bảng đầy đủ các kịch bản

Dữ liệu: sản phẩm fixture có giá niêm yết 15.000.000 VND, giá KM 14.000.000 VND, tồn kho đầu 5. Thông tin người nhận/số điện thoại/địa chỉ đều giả. Mỗi PASS là assertion đã thực thi; không chỉ nhìn giao diện.

| ID | Bước thực hiện | Đầu vào | Mong đợi | Thực tế | Trạng thái | Minh chứng |
|---|---|---|---|---|---|---|
| C01 | Mở giỏ rỗng; so UI/API/H2; Mở giỏ rỗng; so UI/API/H2 | Fixture mới | quantity=0; DB không CartItem | {"quantity":0,"screenshot":"C01-cart-empty.png"} | PASS | [Ảnh](person3-demo-evidence/verified-run/C01-cart-empty.png), [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| C02 | Click thêm sản phẩm ở catalog; Click thêm sản phẩm ở catalog | 1 × giá KM 14.000.000 | Badge1, total14M, kho vẫn5 | {"quantity":1,"total":14000000,"stock":5} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| C03 | Thêm lại cùng sản phẩm; Thêm lại cùng sản phẩm | +1 | Một dòng quantity2, total28M | {"items":1,"quantity":2,"total":28000000} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| C04 | Tăng số lượng qua UI; Tăng số lượng qua UI | 3 | quantity=3, tổng=42000000 | {"quantity":3,"total":42000000,"screenshot":"C04-cart.png"} | PASS | [Ảnh](person3-demo-evidence/verified-run/C04-cart.png), [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| C05 | Giảm số lượng qua UI; Giảm số lượng qua UI | 2 | quantity=2, tổng=28000000 | {"quantity":2,"total":28000000,"screenshot":"C05-cart.png"} | PASS | [Ảnh](person3-demo-evidence/verified-run/C05-cart.png), [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| C06 | Nhập vượt kho rồi PATCH API; Nhập vượt kho rồi PATCH API | 6 > stock5 | UI invalid; 409; DB giữ2 | {"http":409,"dbQuantity":2} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| C07 | Nhập 0/âm/thập phân và gọi API; Nhập 0/âm/thập phân và gọi API | 0,-1,1.5 | UI invalid; cả ba HTTP400 | {"http":[400,400,400],"quantity":2} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| C08 | Click xóa dòng; Click xóa dòng | CartItem hiện có | UI/API/DB trống | {"dbCartItems":0} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| C09 | Reload giỏ; Reload giỏ | Sau xóa | Vẫn trống; checkout ẩn | {"quantity":0,"checkoutVisible":false} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| C10 | Anonymous mở giỏ và gọi API; Anonymous mở giỏ và gọi API | Không login | Redirect login, GET401, POST403 | {"redirect":"/login","get":401,"post":403} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| O01 | Double-click đặt COD qua browser; Double-click đặt COD qua browser | 2 × 14M | Một Order PENDING, một request UI | {"orderId":1,"postCount":1,"screenshot":"O01-order-created.png"} | PASS | [Ảnh](person3-demo-evidence/verified-run/O01-order-created.png), [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| O02 | Đọc snapshot giá/tổng/trạng thái H2; Đọc snapshot giá/tổng/trạng thái H2 | Order1, 2 sản phẩm | price14M,total28M,fee0,COD/PENDING/UNPAID | {"price":14000000,"quantity":2,"total":28000000,"fee":0,"status":"PENDING","payment":"UNPAID"} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| O03 | Kiểm tra kho/giỏ H2 sau checkout; Kiểm tra kho/giỏ H2 sau checkout | stock trước5,mua2 | stock3, CartItem0 | {"stock":3,"cartItems":0} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| O04 | Validation form và API; Validation form và API | Tên rỗng, phone abcdefgh, địa chỉ rỗng | Form invalid và HTTP400 cho từng trường | {"formValid":false,"http":[400,400,400]} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| O05 | POST checkout giỏ rỗng; POST checkout giỏ rỗng | Payload COD hợp lệ | 409; không Order | {"http":409,"orders":0} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| O06 | Replay hai POST cùng key đồng thời; Replay hai POST cùng key đồng thời | Payload từ request UI | Cùng ID; không trừ kho thêm | {"http":[200,200],"sameId":1,"stock":3} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| O07 | Reload và browser Back về checkout; Reload và browser Back về checkout | Sau đơn thành công | Một Order, stock3, checkout disabled | {"orders":1,"stock":3,"checkoutDisabled":true} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| O08 | Mở history và click detail; Mở history và click detail | Order1 | Mã đúng, status PENDING | {"code":"ORD-rfp_JwUaSpKPc-VyipmjBQ","screenshot":"O08-history.png"} | PASS | [Ảnh](person3-demo-evidence/verified-run/O08-history.png), [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| O09 | Thử IDOR và Order không tồn tại; Thử IDOR và Order không tồn tại | Other User và ID99999999 | 404, không lộ total | {"idor":404,"missing":404} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| A01 | Mở danh sách đơn; Mở danh sách đơn | Order1 | Có đúng đơn | {"code":"ORD-rfp_JwUaSpKPc-VyipmjBQ","screenshot":"A01-admin-list.png"} | PASS | [Ảnh](person3-demo-evidence/verified-run/A01-admin-list.png), [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| A02 | Tìm mã đơn và tên khách; Tìm mã đơn và tên khách | Mã đơn; Khách hàng thử nghiệm | Cả hai tìm thấy Order1 | {"matches":2} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| A03 | Lọc và phân trang backend; Lọc và phân trang backend | 12 orders, trang10 dòng; lọc PENDING | Trang1=10, trang2=2, pending1 | {"page1":10,"page2":2,"pending":1,"screenshot":"A03-pagination-filter.png"} | PASS | [Ảnh](person3-demo-evidence/verified-run/A03-pagination-filter.png), [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| A04 | Mở chi tiết snapshot; Mở chi tiết snapshot | Click mã đơn | Hiển thị 28M và form trạng thái | {"total":28000000,"screenshot":"A04-admin-detail.png"} | PASS | [Ảnh](person3-demo-evidence/verified-run/A04-admin-detail.png), [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| A05 | Hoàn tất state machine qua UI; Hoàn tất state machine qua UI | PENDING→CONFIRMED→SHIPPING→COMPLETED | DB/UI COMPLETED | {"states":["PENDING","CONFIRMED","SHIPPING","COMPLETED"],"screenshot":"A05-completed.png"} | PASS | [Ảnh](person3-demo-evidence/verified-run/A05-completed.png), [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| A06 | COD chuyển PAID; COD chuyển PAID | Order COMPLETED | PAID | {"payment":"PAID"} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| A07 | Chặn chỉnh terminal; Chặn chỉnh terminal | COMPLETED→PENDING | Không form; API409 | {"form":0,"http":409} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| A08 | Hủy PENDING và CONFIRMED; Hủy PENDING và CONFIRMED | Hai đơn quantity1 | CANCELLED và kho về3 | {"cancelled":[2,3],"stock":3} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| A09 | Hủy lặp không hoàn kho lần hai; Hủy lặp không hoàn kho lần hai | Expected PENDING trên đơn CANCELLED | 409; kho3 | {"http":409,"stock":3} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| A10 | Hai ADMIN cập nhật stale đồng thời; Hai ADMIN cập nhật stale đồng thời | Context admin/admin2 cùng expected PENDING | Một200 một409; stock khớp | {"http":[409,200],"winner":"CANCELLED","stockAfterCleanup":3} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| A11 | USER không truy cập quản trị; USER không truy cập quản trị | GET list, PATCH status | 403 cả hai | {"list":403,"patch":403} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| R01 | Mở thống kê khi chưa COMPLETED; Mở thống kê khi chưa COMPLETED | Một PENDING | Revenue0, count0 | {"revenue":0,"screenshot":"R01-revenue-empty.png"} | PASS | [Ảnh](person3-demo-evidence/verified-run/R01-revenue-empty.png), [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| R02 | Doanh thu sau 1 COMPLETED; Doanh thu sau 1 COMPLETED | 12 orders, chỉ một hoàn tất | UI/H2 revenue28M, count1 | {"ui":28000000,"db":28000000,"completed":1} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| R03 | Nhóm ngày và Chart.js thật; Nhóm ngày và Chart.js thật | DAY, khoảng UI | API/UI/DB28M; canvas/dataset render | {"width":966,"points":30,"total":28000000,"screenshot":"R03-daily-chart.png"} | PASS | [Ảnh](person3-demo-evidence/verified-run/R03-daily-chart.png), [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| R04 | Nhóm tháng qua UI; Nhóm tháng qua UI | MONTH | Nhãn YYYY-MM; total28M | {"total":28000000,"labels":["2026-09","2026-10"],"screenshot":"R04-monthly-chart.png"} | PASS | [Ảnh](person3-demo-evidence/verified-run/R04-monthly-chart.png), [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| R05 | Kỳ không dữ liệu; Kỳ không dữ liệu | 2000-01-01..2000-01-02 | Revenue0, completed0 | {"revenue":0,"completed":0} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| R06 | Không tính PENDING/CANCELLED; Không tính PENDING/CANCELLED | Completed1, cancelled10, pending1 | Chỉ 28M | {"counts":{"COMPLETED":1,"CANCELLED":10,"PENDING":1},"revenue":28000000} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| R07 | Khoảng ngày đảo ngược; Khoảng ngày đảo ngược | 2026-10-22..2026-10-01 | API400, UI lỗi, nút enabled | {"http":400,"message":"Chọn khoảng ngày hợp lệ, tối đa 366 ngày."} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| R08 | Chặn USER xem thống kê; Chặn USER xem thống kê | GET page/data | 403 cả hai | {"page":403,"data":403} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| W01 | Chờ kết nối WS của hai USER; Chờ kết nối WS của hai USER | JWT cookie, context riêng | /ws/order-status OPEN | {"endpoint":"/ws/order-status","readyState":1} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| W02 | ADMIN click CONFIRMED; ADMIN click CONFIRMED | PENDING→CONFIRMED | DB CONFIRMED | {"status":"CONFIRMED"} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| W03 | USER nhận toast/status không reload; USER nhận toast/status không reload | Tab detail đang mở | CONFIRMED; 0 navigation | {"status":"CONFIRMED","navigations":0,"screenshot":"W03-live-notification.png"} | PASS | [Ảnh](person3-demo-evidence/verified-run/W03-live-notification.png), [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| W04 | Kiểm tra USER khác không nhận frame; Kiểm tra USER khác không nhận frame | Context Other online | 0 frame Other, owner có CONFIRMED | {"otherNewFrames":0,"ownerReceived":true} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| W05 | Đóng socket, reconnect, nhận SHIPPING và reload; Đóng socket, reconnect, nhận SHIPPING và reload | Client close chủ động; Admin SHIPPING | Socket mới OPEN; trạng thái DB đồng bộ | {"reconnected":true,"statusAfterReload":"SHIPPING"} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| W06 | JWT hết hạn và tài khoản khóa/xóa; Chạy JUnit mới trong lượt QA này; assertion repository/H2/socket thật (expiry listener bổ sung unit) | JWT ký hợp lệ đã hết hạn; session của User vừa khóa/xóa | Handshake bị chặn; không phát thông tin; đóng socket khi có event | {"junitTests":["expiredJwtDoesNotReceiveData","rejectsExpiredSignedJwtAtRealHandshake","lockedOrDeletedAccountCannotReceiveOnExistingSocket(boolean)[1]","lockedOrDeletedAccountCannotReceiveOnExistingSocket(boolean)[2]"]} | PASS | [JUnit](person3-demo-evidence/verified-run/junit-summary.json) |
| W07 | Rollback không phát notification; Chạy JUnit mới trong lượt QA này; assertion repository/H2/socket thật (expiry listener bổ sung unit) | Admin transition trong transaction rollback | Không frame thành công; DB vẫn PENDING | {"junitTests":["rollbackDoesNotNotifyAndDisconnectDoesNotBreakOrder"]} | PASS | [JUnit](person3-demo-evidence/verified-run/junit-summary.json) |
| G01 | Login/logout USER và ADMIN; Login/logout USER và ADMIN | POST logout có CSRF; mở trang protected; login lại | Logout về login, login thành công | {"user":true,"admin":true} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| G02 | Catalog/detail/giá KM; Catalog/detail/giá KM | Click link chi tiết thật | Giá14M, nút giỏ enabled | {"salePrice":14000000,"listPrice":15000000} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| G03 | Home/Account → Products; Home/Account → Products | Click CTA ở từng trang | Đều đến /products | {"routes":2,"correct":true} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| G04 | Console và lỗi HTTP; Console và lỗi HTTP | Theo dõi pageerror; tải lại catalog/detail | Không JS exception hoặc HTTP bất thường ngoài negative test | [{"role":"user","status":404,"path":"/user/css/ajax-loader.gif"}]<br><br>1 !== 0<br> | FAIL | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| G05 | Desktop và responsive390×844; Desktop và responsive390×844 | Cart/Revenue, viewport thực | Không body overflow | {"widths":[{"viewport":390,"width":390},{"viewport":390,"width":390}],"screenshots":["G05-cart-desktop.png","G05-cart-mobile.png","G05-revenue-mobile.png"]} | PASS | [Ảnh](person3-demo-evidence/verified-run/G05-cart-desktop.png), [Ảnh](person3-demo-evidence/verified-run/G05-cart-mobile.png), [Ảnh](person3-demo-evidence/verified-run/G05-revenue-mobile.png), [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| G06 | Phân quyền endpoint Người3/QA; Phân quyền endpoint Người3/QA | USER snapshot, anonymous Order/Revenue | 403,401,302 | {"snapshot":403,"order":401,"revenue":302} | PASS | [Assertion browser/API/H2](person3-demo-evidence/verified-run/cases.json) |
| X01 | Rollback checkout và hoàn kho; Chạy JUnit mới trong lượt QA này; assertion repository/H2/socket thật (expiry listener bổ sung unit) | Dòng sau không khả dụng/hoàn kho lỗi | Không Order/stock/cart thay đổi một phần | {"junitTests":["rollbackRestoresEarlierProductWhenLaterLineFails","cancellationRollsBackEarlierRestockWhenLaterLineFails"]} | PASS | [JUnit](person3-demo-evidence/verified-run/junit-summary.json) |
| X02 | Hai User tranh tồn kho 1; Chạy JUnit mới trong lượt QA này; assertion repository/H2/socket thật (expiry listener bổ sung unit) | Hai checkout đồng thời, stock1 | Chỉ một thành công, không tồn kho âm | {"junitTests":["twoBuyersCompeteForLastStock"]} | PASS | [JUnit](person3-demo-evidence/verified-run/junit-summary.json) |

## 4. FAIL và các quan sát

### P3 — G04: thiếu ảnh loading của Slick

- Thực tế: browser trả **404 `/user/css/ajax-loader.gif`**, ghi nhận hai request trong console/network của USER ở catalog/detail. `shoptech/src/main/resources/static/user/css/slick-theme.css:5` dùng `url('./ajax-loader.gif')`, nhưng không tìm thấy file tương ứng trong static/user.
- Tái hiện: chạy launcher H2 → login USER → catalog → chi tiết sản phẩm → xem Network/Console. [HTTP evidence](person3-demo-evidence/verified-run/browser-response-errors.json) và [Console evidence](person3-demo-evidence/verified-run/console.json).
- Ảnh hưởng: indicator loading của slider thiếu; không làm sai Cart/Order/Revenue. Không có JS exception và không có HTTP5xx trong lượt QA. G04 vẫn được ghi **FAIL**, không che lỗi asset bằng tiêu chí chỉ kiểm tra 5xx.
- Không tự sửa CSS/asset thuộc phần giao diện chung/Người 2 trong lượt QA này. Đề xuất nhóm bổ sung asset đúng giấy phép hoặc thống nhất bỏ tham chiếu; không cần viết lại slider.
- HTTP400 `/admin/revenue/data` là kết quả mong đợi của R07 (from > to), không phải bug. Các 400/401/403/404/409 chủ động từ API test cũng là negative-case mong đợi, được lưu riêng trong HTTP log.
- Không phát hiện lỗi Register/Auth mới; chỉ login/logout được thực thi. Không tự sửa module Register/Auth.
- Lượt automation REPL ban đầu bị timeout và có selector/role-label của bộ ghi minh chứng chưa đúng. Đã thay bằng runner có checkpoint, chạy lại toàn bộ 49 browser cases; chỉ kết quả `verified-run/` được tính trong báo cáo. File ở thư mục cha là minh chứng thử nghiệm đầu, không dùng để thay PASS lượt cuối.

## 5. Đối chiếu backend và frontend

- Cart: UI quantity 1→2→3→2, API totals 14M→28M→42M→28M và JDBC quantity đồng nhất. Add không giảm kho; invalid input không đổi DB; delete/reload giỏ trống, checkout ẩn.
- COD: double-click tạo đúng một POST từ browser; replay hai POST cùng key đồng thời trả cùng Order ID. Snapshot 2×14M, fee0, total28M, PENDING/UNPAID; kho5→3, giỏ rỗng. Back/reload không tạo thêm Order.
- Admin: hai context ADMIN thực hiện stale PATCH đồng thời, đúng một200 và một409; DB stock khớp trạng thái thắng. Hủy từ PENDING/CONFIRMED và retry chỉ hoàn kho một lần. Dataset12 đơn kiểm chứng page1=10/page2=2 và filter PENDING=1.
- Revenue: H2 sau dữ liệu đối chứng có COMPLETED1/CANCELLED10/PENDING1; aggregate chỉ28M/count1. Chart.instances có canvas kích thước thực, dataset sum28M và labels ngày/tháng. Phân bổ theo created_at, không phải completion time.
- WebSocket: browser ghi OPEN, frame chỉ tới owner, USER khác online không nhận frame. Status/toast CONFIRMED thay đổi khi navigation count0. Chủ động close, reconnect tự động, nhận SHIPPING; reload khớp DB. W06/W07 dùng test socket/JWT/transaction mới, không giả lập ảnh browser cho tình huống chưa thao tác UI.
- Responsive desktop1280×720 và mobile390×844: Cart/Revenue không body overflow, có screenshot thật. Console có thiếu asset P3 nêu trên; không nói toàn bộ network sạch.

## 6. JUnit và PostgreSQL

Đã chạy `.\mvnw.cmd clean compile test` trong shoptech với Java26, source target17; không dùng skipTests. Các test rollback/concurrent-stock đã tồn tại được **chạy lại thực sự trong lượt này**, không viết test trùng chỉ để tăng số lượng.

| Suite | Thực thi PASS | FAIL/ERROR | SKIP/NOT TESTED |
|---|---:|---:|---:|
| CommerceIntegrationTest | 30 | 0 | 0 |
| PostgresCommerceIntegrationTest | 0 | 0 | 31 |
| CartServiceTest | 13 | 0 | 0 |
| JwtServiceTest | 3 | 0 | 0 |
| OrderNotificationServiceTest | 3 | 0 | 0 |
| ShoptechApplicationTests | 4 | 0 | 0 |
| WebSocketNotificationTest | 8 | 0 | 0 |

### PostgreSQL chưa thực hiện

Không tìm thấy PostgreSQL/Docker local được cấp sẵn và không có PERSON3_POSTGRES_TEST_URL. Cả 31 testcase dưới đây đều **NOT TESTED**, không lấy H2 PASS làm PostgreSQL PASS. Không kết nối hay ghi Supabase để thay thế.

- NOT TESTED: `checkoutSnapshotAndHistory`
- NOT TESTED: `revenueProjectionHandlesPostgresLowercaseAlias`
- NOT TESTED: `cartRequiresLogin`
- NOT TESTED: `confirmedCancellationAndPaidOrderProtection`
- NOT TESTED: `checkoutRefreshesProductAlreadyInPersistenceContext`
- NOT TESTED: `csrfRequired`
- NOT TESTED: `existingAdminPagesRegression`
- NOT TESTED: `jwtCookieLoginLogoutRegression`
- NOT TESTED: `rejectsEmptyCheckout`
- NOT TESTED: `cancellationRestocksOnceAndIsTerminal`
- NOT TESTED: `concurrentFirstAddCreatesOneCartAndMerges`
- NOT TESTED: `adminSearchPaginationDetailAndTransitions`
- NOT TESTED: `shoppingLinksDoNotLoopBackToLoggedInLanding`
- NOT TESTED: `revenueEmptyAndValidation`
- NOT TESTED: `rollbackRestoresEarlierProductWhenLaterLineFails`
- NOT TESTED: `revenueAggregatesWithTimezoneBoundariesAndNoDetailDoubleCounting`
- NOT TESTED: `adminEndpointsDenyUser`
- NOT TESTED: `twoBuyersCompeteForLastStock`
- NOT TESTED: `otherUsersItemIsNotAccessible`
- NOT TESTED: `concurrentAdminsGetConflictAndRestockOnce`
- NOT TESTED: `cancellationRollsBackEarlierRestockWhenLaterLineFails`
- NOT TESTED: `userCannotAccessRevenue`
- NOT TESTED: `cartApiRejectsFractionalAndNegativeQuantity`
- NOT TESTED: `checkoutValidatesRecipientAndPayment`
- NOT TESTED: `sameRequestKeyCannotExposeAnotherOwnersOrderAndRollsBackStock`
- NOT TESTED: `concurrentDoubleSubmitReturnsOneOrder`
- NOT TESTED: `checkoutRechecksStockAndAvailability`
- NOT TESTED: `cartApiAndPage`
- NOT TESTED: `idempotentReplayDoesNotConsumeNewCart`
- NOT TESTED: `differentCheckoutKeysInParallelStillCreateOneOrder`
- NOT TESTED: `usesRealPostgresSchemaConstraintsAndArrayMapping`

Khi có DB local riêng, chỉ chạy PostgresCommerceIntegrationTest theo guard loopback/database/schema test đã chuẩn bị. Cần xem thực tế native Revenue projection, ARRAY, timezone, row locking và schema validation trên PostgreSQL trước tích hợp dev.

## 7. Đánh giá demo và phần cần người dùng kiểm tra thêm

- Bốn module chính và notification đã có minh chứng chạy thực tế trên H2; **52/53 macro-case PASS**, lỗi còn lại P3 loader asset. Có thể demo nghiệp vụ H2, chưa tự xác nhận production-ready/PostgreSQL-compatible100%.
- Cần nhóm xử lý/duyệt P3 asset chung và chạy lại G04; chạy PostgreSQL test cô lập; review source/diff trước tự quyết định tích hợp dev.
- Cần tự kiểm tra nếu triển khai thực: reverse proxy/TLS, Origin APP_PUBLIC_BASE_URL, nhiều instance/offline notification, SMTP/Cloudinary và toàn bộ CRUD của các thành viên khác. Những mục này không được đánh dấu PASS trong lượt QA.
- Video2fps mang tính minh họa; muốn đọc từng con số hãy mở ảnh độ phân giải đầy đủ và JSON/JUnit. Không có hình/video giả hay mock UI.
- Không có server H2/Chrome/FFmpeg test còn chạy. Mật khẩu chỉ trong phiên đã kết thúc. Không `.env`, Supabase, Chatbox/Chatbot hoặc thao tác Git bị cấm. [Kiểm tra cuối](person3-demo-evidence/verified-run/cleanup.json).
