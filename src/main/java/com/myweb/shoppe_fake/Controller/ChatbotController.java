package com.myweb.shoppe_fake.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Value("${gemini.api.key}")
    private String apiKey;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askBot(@RequestBody Map<String, String> payload) {
        String userMessage = payload.get("message");
        
        // URL gọi API của Gemini
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Nạp kiến thức chính sách cho AI
       String systemInstruction = "Bạn là trợ lý ảo AI thông minh và thân thiện của cửa hàng thời trang 'Shoppe Fashion'. " +
    "Nhiệm vụ của bạn là hỗ trợ khách hàng đa năng với các thông tin sau: " +
    "1. Tư vấn thời trang: Hỗ trợ khách hàng lựa chọn trang phục (áo thun, quần jean, váy đầm, áo khoác, phụ kiện) dựa trên phong cách, cân nặng, chiều cao hoặc sự kiện họ sắp tham gia. " +
    "2. Hỗ trợ chính sách: Giải đáp về chính sách đổi trả (trong 7 ngày nếu lỗi từ nhà sản xuất hoặc không vừa size, yêu cầu chưa qua sử dụng, còn nguyên tem mác) và quy trình hoàn tiền. " +
    "3. Giao tiếp: Luôn xưng hô là 'tôi' và gọi khách hàng là 'bạn'. Trả lời ngắn gọn, có gu, lịch sự và định dạng rõ ràng, dễ đọc. " +
    "Lưu ý: Nếu khách hỏi những vấn đề không liên quan đến thời trang, hãy khéo léo từ chối. Không tự ý bịa ra các chương trình giảm giá.";
        Map<String, Object> requestBody = new HashMap<>();
        
        Map<String, Object> sysInst = new HashMap<>();
        sysInst.put("parts", Map.of("text", systemInstruction));
        requestBody.put("system_instruction", sysInst);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(Map.of("text", userMessage)));
        requestBody.put("contents", List.of(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        Map<String, String> result = new HashMap<>();

        try {
            // Gửi Request tới Gemini
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            
            // Đọc JSON trả về để lấy câu trả lời của Bot
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            String replyText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            
            result.put("reply", replyText);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("reply", "Xin lỗi, hệ thống AI đang bận. Vui lòng thử lại sau.");
            return ResponseEntity.status(500).body(result);
        }
    }
}
