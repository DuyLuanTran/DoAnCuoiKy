package com.example.shoptech.controller;

import com.example.shoptech.dto.ChatRequest;
import com.example.shoptech.dto.ChatResponse;
import com.example.shoptech.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return new ChatResponse(
                chatbotService.reply(request.message(), request.history())
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleChatbotError(
            ResponseStatusException exception
    ) {
        String detail = exception.getReason() == null
                ? "Chatbot không thể xử lý yêu cầu."
                : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("detail", detail));
    }
}
