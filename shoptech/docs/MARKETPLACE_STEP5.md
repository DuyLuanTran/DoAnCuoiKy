# Checkpoint 5 — Trang chủ marketplace và dọn trang tổng quan Seller

## Đã hoàn tất
- `/` là trang mua sắm chung cho Guest, User, Seller và Admin. Logo ShopTech trở về trang này. Đăng nhập Admin/Seller vẫn vào khu quản trị tương ứng; User vào `/`, `/user/home` cũ chuyển hướng về `/`.
- Giữ template Electro: banner danh mục với lớp đỏ chéo, header/footer, màu sắc và ảnh sản phẩm từ database. Bỏ toàn bộ dữ liệu demo liên hệ/USD/newsletter/stars của trang index cũ.
- Banner lấy tối đa 3 danh mục khác nhau từ các sản phẩm mới đang công khai. Không tạo ảnh hoặc danh mục giả.
- Khu vực sản phẩm mới và bán chạy có 20 sản phẩm/trang, chuyển tab và phân trang qua URL. Bán chạy chỉ hiển thị sản phẩm có lượt mua hoàn tất, sắp số lượng giảm dần, rồi ngày tạo/ID để ổn định thứ tự.
- Khu vực bán trên 10 dùng ngưỡng >=11, tối đa 4 thẻ xem trước, đường dẫn Xem tất cả giữ ngưỡng và sắp xếp. Nếu chưa có thì hiện trạng thái trống đúng dữ liệu.
- Số bán = tổng quantity từ dòng hàng của shop_order COMPLETED, đơn cha không bị xóa và shop_id khớp sản phẩm. Không đếm số đơn thay cho số lượng, không tính trạng thái cha thay trạng thái từng shop. CANCELLED/RETURN_REQUESTED/REFUNDED và các trạng thái chưa hoàn tất không được tính.
- Đơn nhập lịch sử bị lệch subtotal so với tổng dòng hàng bị loại khỏi số bán; đơn lịch sử khớp vẫn được tính. Quy tắc chung ProductSalesCriteria áp dụng cho cả thứ hạng, ngưỡng và số hiển thị trên thẻ.
- Các điều kiện công khai về sản phẩm/danh mục/shop/chủ shop giữ nguyên. Thẻ sản phẩm dùng chung fragment cho trang chủ, toàn sàn và trang shop để giữ cùng ảnh, giá và tên shop.
- Bộ lọc `/products` thêm Bán chạy, số dòng 20 và minSold. Giá, danh mục, shop, checkbox và ngưỡng bán được giữ khi tìm kiếm/sắp xếp/phân trang.
- `/shops` có tìm tên, 12 shop/trang, chỉ các shop/chủ shop đang hoạt động. Thẻ shop có số sản phẩm công khai và giới thiệu; không lộ hồ sơ riêng.
- Seller dashboard bỏ toàn bộ các nút lặp dưới thông tin shop. Xem trang shop chuyển vào sidebar, dùng cùng cấu trúc nav-item/nav-link/icon fa-fw/span, chỉ hiển thị cho shop ACTIVE.

## Môi trường và dữ liệu
- Không cần migration, không đổi cấu trúc DB ở bước này. Bản thử dùng PostgreSQL riêng `127.0.0.1:55432/shoptech_marketplace` và web cổng 18081. Không cập nhật Supabase gốc.
- Thử UI checkpoint 5 chỉ tìm kiếm/đọc trang, không tạo thêm đơn hoặc sửa sản phẩm.
- Dữ liệu hiện tại: 10 sản phẩm công khai; 3 sản phẩm có đơn COMPLETED đủ điều kiện đếm, mỗi sản phẩm 1 lượt bán; chưa có sản phẩm >10. Hai shop An Khang Tech và ShopTech.
- Web được khởi động bằng scripts/Start-MarketplaceSandbox.ps1; tài khoản thử vẫn như các checkpoint trước. Email/chatbot/Cloudinary ghi thật bị vô hiệu trong profile riêng.

## Kiểm tra
- `.local/test-step5-home.log`: 28 PASS (14 H2 + 14 PostgreSQL), sau khi biên dịch lại từ đầu để loại bản class cũ của trình soạn thảo.
- `.local/test-step5-full.log`: 200 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS lúc 23:52 ngày 27/10/2026 (+07). Gồm 125 kiểm thử thông thường + 75 PostgreSQL trong schema riêng trên shoptech_person3_test.
- Phủ ngưỡng đúng 10/11, cộng quantity nhiều đơn, loại trạng thái hủy/trả/hoàn/deleted, đơn lịch sử lệch/khớp, shop con hoàn tất trong đơn cha còn pending, phân trang 20, scope shop, không lộ hồ sơ riêng, sidebar không lặp nút, và hồi quy giỏ hàng/đơn hàng/thông báo/đăng nhập.
- UI đã thử: trang chủ dùng ảnh đúng DB; tab Bán chạy hiển thị 3 sản phẩm và số bán 1; Các shop trả 2 shop; tìm An Khang còn 1 shop và xóa tìm kiếm về đủ 2; console không có lỗi/warning ở các trang đã thử.
- Ảnh: .local/evidence/home-step5.png, home-best-selling-step5.png, seller-menu-step5.png.
- Ngày 28/10/2026 hoàn tất lượt bàn giao: khởi động lại Docker/container/web sau khi máy tắt; giữ nguyên dữ liệu riêng đã lưu. Kiểm tra Guest, đăng nhập Seller, menu Xem trang shop và tab Bán chạy; console không có lỗi/warning.
- Kiểm tra viewport điện thoại 390×844: menu và trang Các shop hoạt động, không cuộn ngang. Chỉnh CSS để các thẻ shop trên màn hình nhỏ rộng đều 100%, xác nhận cả hai cùng rộng 375px trong vùng nội dung. Đã trả viewport về mặc định. Thay đổi cuối này chỉ là CSS và đã kiểm tra trực quan; bộ 200 test logic vẫn là kết quả ngày 27/10 ở trên.

## Bản thử
- http://localhost:18081/
- http://localhost:18081/?feed=bestSelling#home-products
- http://localhost:18081/shops
- http://localhost:18081/seller

## Ngoài checkpoint này
- Đánh giá/yêu thích, OTP mới, địa chỉ nhận hàng nhiều địa chỉ, voucher và cổng thanh toán online chưa được bổ sung ở bước 5.
- Mốc hợp lý tiếp theo: nhóm giỏ hàng và tóm tắt thanh toán theo shop, giữ cấu trúc đơn theo shop đã có. Dừng xem UI checkpoint 5 trước khi tiếp tục theo quy trình đã thống nhất.


Bước tiếp theo đã thực hiện: xem [Bước 6 — Yêu thích và đánh giá](MARKETPLACE_STEP6.md).
