package com.example.shoptech.service;

import com.example.shoptech.dto.ChatMessage;
import com.example.shoptech.entity.Category;
import com.example.shoptech.entity.Product;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class ChatbotService {

    private static final Logger logger =
            LoggerFactory.getLogger(ChatbotService.class);
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";
    private static final int CATALOG_LIMIT = 15;

    private final RestClient restClient;
    private final ProductCatalogService productCatalogService;
    private final CategoryService categoryService;
    private final String apiKey;
    private final String model;

    public ChatbotService(
            ProductCatalogService productCatalogService,
            CategoryService categoryService,
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.api.model:gemini-3.5-flash-lite}") String model
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(45));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        this.productCatalogService = productCatalogService;
        this.categoryService = categoryService;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String reply(String message, List<ChatMessage> history) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Chatbot chưa được cấu hình API key."
            );
        }

        List<Map<String, Object>> contents = new ArrayList<>();
        if (history != null) {
            for (ChatMessage turn : history) {
                contents.add(content(turn.role(), turn.text().strip()));
            }
        }
        contents.add(content("user", message.strip()));

        Map<String, Object> requestBody = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemInstruction()))
                ),
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", 512
                )
        );

        try {
            JsonNode response = restClient.post()
                    .uri(GEMINI_URL, model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            String answer = extractAnswer(response);
            if (answer.isBlank()) {
                logger.warn("Gemini returned no text response.");
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Chatbot chưa thể tạo câu trả lời. Vui lòng thử lại."
                );
            }
            return answer;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            String providerError = providerError(exception);
            logger.warn(
                    "Gemini request failed with HTTP status {} for model '{}': {}",
                    status,
                    model,
                    providerError
            );
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    geminiFailureMessage(status, providerError)
            );
        } catch (ResourceAccessException exception) {
            logger.warn("Could not connect to Gemini.", exception);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể kết nối chatbot lúc này. Vui lòng thử lại sau."
            );
        }
        catch (org.springframework.http.converter.HttpMessageConversionException exception) {
    logger.error("Could not parse Gemini response.", exception);
    throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "Không đọc được phản hồi từ chatbot. Vui lòng thử lại."
    );
}
    }

    private String geminiFailureMessage(int status, String providerError) {
        String normalizedError = providerError.toLowerCase(Locale.ROOT);
        if (normalizedError.contains("api_key_invalid")
                || normalizedError.contains("api key not valid")
                || normalizedError.contains("invalid api key")) {
            return "Gemini báo API key không hợp lệ. Kiểm tra lại GEMINI_API_KEY trong file .env rồi khởi động lại ứng dụng.";
        }

        return switch (status) {
            case 400 ->
                    "Gemini từ chối nội dung hoặc cấu hình request (HTTP 400). Kiểm tra log backend để xem mã lỗi Gemini.";
            case 404 ->
                    "Không tìm thấy model Gemini. Kiểm tra gemini.api.model trong application.properties.";
            case 401, 403 ->
                    "Gemini từ chối API key hoặc API key chưa có quyền gọi dịch vụ. Kiểm tra cấu hình key và quyền Gemini API.";
            case 429 ->
                    "Gemini đang vượt giới hạn hoặc đã hết quota. Kiểm tra quota và billing của API key.";
            default -> status >= 500
                    ? "Gemini đang tạm thời gặp sự cố. Vui lòng thử lại sau."
                    : "Không thể gọi Gemini (HTTP " + status + "). Kiểm tra log backend.";
        };
    }

    private String providerError(RestClientResponseException exception) {
        try {
            JsonNode body = new ObjectMapper()
                    .readTree(exception.getResponseBodyAsString());
            JsonNode error = body.path("error");
            String status = error.path("status").asText("");
            String message = error.path("message").asText("");
            if (!status.isBlank() && !message.isBlank()) {
                return status + ": " + message;
            }
            if (!status.isBlank()) {
                return status;
            }
            if (!message.isBlank()) {
                return message;
            }
        } catch (JacksonException exceptionDuringParsing) {
                logger.debug("Gemini returned a non-JSON error response.");
        }
        return "No provider error details returned";
    }

    private Map<String, Object> content(String role, String text) {
        return Map.of(
                "role", role,
                "parts", List.of(Map.of("text", text))
        );
    }

    private String systemInstruction() {
        return """
                Bạn là Trợ lý AI chăm sóc khách hàng thông minh của sàn thương mại điện tử công nghệ ShopTech (ShopTech Marketplace).
                Nhiệm vụ của bạn là tư vấn sản phẩm công nghệ, giải đáp thắc mắc, hỗ trợ mua sắm và hướng dẫn người dùng sử dụng đầy đủ các tính năng trên website ShopTech.

                === NGUYÊN TẮC GIAO TIẾP & ĐỊNH DẠNG ===
                1. Luôn trả lời bằng tiếng Việt lịch sự, thân thiện, chu đáo và am hiểu sâu sắc về công nghệ. Xưng "mình" hoặc "ShopTech" và gọi khách hàng là "bạn" hoặc "quý khách".
                2. Định dạng câu trả lời gọn gàng bằng Markdown (dùng bullet points, in đậm tên sản phẩm, giá tiền và liên kết).
                3. Khi tư vấn tính năng hoặc sản phẩm, LUÔN chủ động cung cấp đường dẫn website tương ứng để người dùng có thể nhấp vào truy cập ngay:
                   - Đăng ký tài khoản mới: /register
                   - Đăng nhập: /login
                   - Quên mật khẩu: /forgot-password
                   - Trang hồ sơ cá nhân: /account
                   - Trang mua sắm / tìm kiếm sản phẩm: / hoặc /store
                   - Giỏ hàng: /cart
                   - Đặt hàng & thanh toán: /checkout
                   - Tra cứu & theo dõi lịch sử đơn hàng: /orders
                   - Danh sách sản phẩm yêu thích: /favorites
                   - Đăng ký mở gian hàng (dành cho người bán / seller): /shop-registration
                   - Trang quản trị gian hàng (dành cho shop đã duyệt): /seller
                4. Tuyệt đối KHÔNG yêu cầu khách hàng cung cấp mật khẩu, mã OTP, số thẻ ngân hàng hoặc dữ liệu cá nhân nhạy cảm.
                5. Với câu hỏi về sản phẩm, hãy dựa vào danh sách sản phẩm và danh mục bên dưới để tư vấn chính xác tên, giá bán, tình trạng còn hàng và link xem chi tiết (/products/{slug}).

                === HƯỚNG DẪN TÍNH NĂNG & NỀN TẢNG SHOPTECH ===
                - Đăng ký tài khoản: Khi khách hỏi cách tạo tài khoản, hướng dẫn khách truy cập liên kết /register để đăng ký tài khoản khách hàng miễn phí (điền Họ tên, Tên đăng nhập, Email, Số điện thoại, Địa chỉ và Mật khẩu). Sau khi tạo thành công, khách đăng nhập tại /login.
                - Đăng ký mở gian hàng (Seller / Người bán): ShopTech là sàn Marketplace đa người bán. Để bán hàng công nghệ, người dùng cần đăng nhập tài khoản rồi truy cập /shop-registration để gửi hồ sơ (Tên shop, SĐT, Email, Địa chỉ, Mô tả gian hàng). Sau khi Quản trị viên (Admin) phê duyệt, shop sẽ kích hoạt và người bán có thể truy cập /seller để đăng sản phẩm, theo dõi doanh thu và quản lý đơn hàng.
                - Quy trình mua hàng & thanh toán: Khách duyệt sản phẩm tại /store, bấm thêm vào giỏ hàng (/cart), rồi tiến hành đặt hàng tại /checkout. Hỗ trợ hình thức thanh toán khi nhận hàng (COD) và thanh toán trực tuyến tiện lợi.
                - Tra cứu đơn hàng: Khách hàng sau khi đăng nhập có thể vào /orders để xem danh sách các đơn hàng đã đặt và tình trạng xử lý đơn hàng.
                - Chính sách bán hàng & CSKH:
                  + 100% sản phẩm chính hãng, đầy đủ tem mác, phụ kiện nguyên hộp.
                  + Bảo hành chính hãng 12 tháng theo quy chuẩn của từng hãng.
                  + Chính sách đổi trả 1-đổi-1 trong vòng 7 ngày đầu nếu có lỗi phần cứng từ nhà sản xuất.
                  + Giao hàng toàn quốc nhanh chóng trong 2 - 4 ngày làm việc, được phép đồng kiểm tra ngoại quan trước khi thanh toán.
                  + Kênh hỗ trợ khách hàng: Hotline 1900-8888 (8:00 - 21:00 hàng ngày), Email hỗ trợ: support@shoptech.vn.

                === CÁC DANH MỤC NGÀNH HÀNG CÔNG NGHỆ HIỆN CÓ ===
                """ + categoryCatalog() + """

                === DANH SÁCH SẢN PHẨM NỔI BẬT ĐANG KINH DOANH ===
                """ + productCatalog();
    }

    private String categoryCatalog() {
        try {
            List<Category> categories = categoryService.getAvailableCategories();
            if (categories == null || categories.isEmpty()) {
                return "- Khám phá toàn bộ ngành hàng công nghệ tại: /store\n";
            }
            StringBuilder sb = new StringBuilder();
            for (Category category : categories) {
                sb.append("- ")
                        .append(category.getName())
                        .append(": /store?category=")
                        .append(category.getId())
                        .append('\n');
            }
            return sb.toString();
        } catch (Exception exception) {
            logger.warn("Could not load category catalog for chatbot.", exception);
            return "- Khám phá toàn bộ ngành hàng công nghệ tại: /store\n";
        }
    }

    private String productCatalog() {
        try {
            List<Product> products = productCatalogService.search(
                    "",
                    null,
                    PageRequest.of(0, CATALOG_LIMIT)
            ).getContent();

            if (products.isEmpty()) {
                return "Hiện chưa có sản phẩm hiển thị trong danh mục. Khách có thể ghé /store để cập nhật thêm sản phẩm mới nhất.\n";
            }

            StringBuilder catalog = new StringBuilder();
            for (Product product : products) {
                BigDecimal discountPrice = product.getDiscountPrice();
                BigDecimal displayedPrice = discountPrice == null
                        ? product.getPrice()
                        : discountPrice;

                catalog.append("- ")
                        .append(product.getName());

                if (product.getCategory() != null && product.getCategory().getName() != null) {
                    catalog.append(" | danh mục: ").append(product.getCategory().getName());
                }

                if (product.getShop() != null && product.getShop().getName() != null) {
                    catalog.append(" | shop: ").append(product.getShop().getName());
                }

                catalog.append(" | giá: ")
                        .append(displayedPrice != null ? displayedPrice.toPlainString() : "Liên hệ")
                        .append(" VND")
                        .append(" | tồn kho: ")
                        .append(product.getStockQuantity())
                        .append(" | xem chi tiết: /products/")
                        .append(product.getSlug());

                if (discountPrice != null
                        && product.getPrice() != null
                        && discountPrice.compareTo(product.getPrice()) < 0) {
                    catalog.append(" (giá gốc ")
                            .append(product.getPrice().toPlainString())
                            .append(" VND)");
                }
                catalog.append('\n');
            }
            return catalog.toString();
        } catch (Exception exception) {
            logger.warn("Could not load product catalog for chatbot.", exception);
            return "Khám phá danh sách sản phẩm tại /store\n";
        }
    }

    private String extractAnswer(JsonNode response) {
        if (response == null) {
            return "";
        }

        JsonNode parts = response.path("candidates")
                .path(0)
                .path("content")
                .path("parts");
        if (!parts.isArray()) {
            return "";
        }

        StringBuilder answer = new StringBuilder();
        for (JsonNode part : parts) {
            JsonNode text = part.get("text");
            if (text != null && text.isTextual()) {
                if (!answer.isEmpty()) {
                    answer.append('\n');
                }
                answer.append(text.asText());
            }
        }
        return answer.toString().strip();
    }
}
