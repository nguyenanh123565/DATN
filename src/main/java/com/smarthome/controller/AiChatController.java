package com.smarthome.controller;

import com.smarthome.dto.ChatDto;
import com.smarthome.service.ChatService;
import com.smarthome.service.EmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller cho AI Chat - Trợ lý tư vấn sản phẩm SmartHome
 *
 * Endpoints:
 * - POST /api/ai/chat         → Gửi tin nhắn, nhận tư vấn AI
 * - GET  /api/ai/history/{id} → Xem lịch sử chat
 * - DELETE /api/ai/history/{id} → Xóa lịch sử
 * - POST /api/ai/refresh      → Admin: reload embeddings
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    private final ChatService chatService;
    private final EmbeddingService embeddingService;

    public AiChatController(ChatService chatService, EmbeddingService embeddingService) {
        this.chatService = chatService;
        this.embeddingService = embeddingService;
    }

    /**
     * Gửi tin nhắn và nhận tư vấn từ AI
     * Nếu không có sessionId → tạo phiên chat mới
     * Nếu có sessionId → tiếp tục phiên chat cũ
     */
    @PostMapping("/chat")
    @Operation(summary = "Gửi tin nhắn cho AI",
            description = "Gửi câu hỏi và nhận tư vấn sản phẩm từ AI. " +
                    "Gửi sessionId=null để tạo phiên mới, hoặc gửi sessionId cũ để tiếp tục.")
    public ResponseEntity<ChatDto.ChatResponse> chat(@RequestBody ChatDto.ChatRequest request) {
        log.info("💬 Chat request: message='{}', session='{}'",
                request.getMessage(), request.getSessionId());

        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ChatDto.ChatResponse response = chatService.processMessage(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy lịch sử chat theo sessionId
     */
/* 
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<ChatDto.ChatHistoryResponse> getHistory(@PathVariable String sessionId) {
        ChatDto.ChatHistoryResponse history = chatService.getHistory(sessionId);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<Map<String, String>> deleteHistory(@PathVariable String sessionId) {
        chatService.deleteHistory(sessionId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa lịch sử chat thành công!"));
    }

    @GetMapping("/admin/history")
    public ResponseEntity<List<ChatDto.AdminHistoryDto>> getAdminHistory() {
        return ResponseEntity.ok(chatService.getAllHistoryForAdmin());
    }
*/

    /**
     * Admin: Reload embedding sản phẩm
     * Gọi khi thêm/sửa/xóa sản phẩm
     */
    @PostMapping("/refresh")
    @Operation(summary = "[Admin] Refresh embeddings",
            description = "Reload lại embedding sản phẩm khi có thay đổi trong database")
    public ResponseEntity<Map<String, String>> refreshEmbeddings() {
        embeddingService.refreshEmbeddings();
        return ResponseEntity.ok(Map.of("message", "Đã refresh embeddings thành công!"));
    }
}
