package com.smarthome.dto;


import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs cho AI Chat API
 */
public class ChatDto {

    // === Request từ client ===
    public static class ChatRequest {
        private String message;     // Câu hỏi của user
        private String sessionId;   // ID phiên chat (null = tạo mới)

        public ChatRequest() {}
        public ChatRequest(String message, String sessionId) {
            this.message = message;
            this.sessionId = sessionId;
        }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }

    // === Response trả về client ===
    public static class ChatResponse {
        private String reply;           // Câu trả lời từ AI
        private String sessionId;       // ID phiên chat
        private List<ProductSuggestion> suggestedProducts; // Sản phẩm gợi ý

        public ChatResponse() {}
        public ChatResponse(String reply, String sessionId, List<ProductSuggestion> suggestedProducts) {
            this.reply = reply;
            this.sessionId = sessionId;
            this.suggestedProducts = suggestedProducts;
        }
        public String getReply() { return reply; }
        public void setReply(String reply) { this.reply = reply; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public List<ProductSuggestion> getSuggestedProducts() { return suggestedProducts; }
        public void setSuggestedProducts(List<ProductSuggestion> suggestedProducts) { this.suggestedProducts = suggestedProducts; }
    }

    // === Thông tin sản phẩm gợi ý ===
    public static class ProductSuggestion {
        private Long id;
        private String name;
        private String price;
        private String brand;
        private String slug;
        private String imageUrls;

        public ProductSuggestion() {}
        public ProductSuggestion(Long id, String name, String price, String brand, String slug, String imageUrls) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.brand = brand;
            this.slug = slug;
            this.imageUrls = imageUrls;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPrice() { return price; }
        public void setPrice(String price) { this.price = price; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getImageUrls() { return imageUrls; }
        public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
    }

    // === Lịch sử chat ===
    public static class MessageDto {
        private String role;        // USER hoặc ASSISTANT
        private String content;     // Nội dung
        private LocalDateTime timestamp;

        public MessageDto() {}
        public MessageDto(String role, String content, LocalDateTime timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class ChatHistoryResponse {
        private String sessionId;
        private List<MessageDto> messages;

        public ChatHistoryResponse() {}
        public ChatHistoryResponse(String sessionId, List<MessageDto> messages) {
            this.sessionId = sessionId;
            this.messages = messages;
        }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public List<MessageDto> getMessages() { return messages; }
        public void setMessages(List<MessageDto> messages) { this.messages = messages; }
    }

    // === Lịch sử chat cho Admin ===
    public static class AdminHistoryDto {
        private String sessionId;
        private String userId;
        private String role;
        private String message;
        private LocalDateTime timestamp;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
