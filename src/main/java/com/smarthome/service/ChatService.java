package com.smarthome.service;

import com.smarthome.dto.ChatDto;
import com.smarthome.entity.AiConversation;
import com.smarthome.entity.AiMessage;
import com.smarthome.entity.Product;
import com.smarthome.entity.User;
import com.smarthome.repository.AiConversationRepository;
import com.smarthome.repository.AiMessageRepository;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service quản lý chat AI
 * - Tạo/quản lý phiên chat
 * - Lưu lịch sử tin nhắn
 * - Gọi RagService để xử lý câu hỏi
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final RagService ragService;
    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;

    public ChatService(RagService ragService, 
                       AiConversationRepository conversationRepository, 
                       AiMessageRepository messageRepository) {
        this.ragService = ragService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * Xử lý tin nhắn chat từ user
     * @param request chứa message và sessionId
     * @return ChatResponse với câu trả lời AI
     */
    @Transactional
    public ChatDto.ChatResponse processMessage(ChatDto.ChatRequest request) {
        // 1. Tạo hoặc tìm phiên chat
        String sessionId = request.getSessionId();
        AiConversation conversation;

        if (sessionId == null || sessionId.isEmpty()) {
            // Tạo phiên chat mới
            sessionId = UUID.randomUUID().toString();
            conversation = new AiConversation();
            conversation.setSessionId(sessionId);
            conversation.setTitle(truncate(request.getMessage(), 100));
            conversation.setMessages(new ArrayList<>());
            
            conversation = conversationRepository.save(conversation);
            log.info("📝 Tạo phiên chat mới: {}", sessionId);
        } else {
            final String finalSessionId = sessionId;
            conversation = conversationRepository.findBySessionId(sessionId)
                    .orElseGet(() -> {
                        AiConversation newConv = new AiConversation();
                        newConv.setSessionId(finalSessionId);
                        newConv.setTitle(truncate(request.getMessage(), 100));
                        newConv.setMessages(new ArrayList<>());
                        return conversationRepository.save(newConv);
                    });
        }

        // 2. Lưu tin nhắn user
        AiMessage userMsg = new AiMessage();
        userMsg.setConversation(conversation);
        userMsg.setRole(AiMessage.MessageRole.USER);
        userMsg.setContent(request.getMessage());
        messageRepository.save(userMsg);

        try {
            // 3. Gọi RAG pipeline để lấy câu trả lời (Truyền thêm sessionId để Rate Limit)
            RagService.RagResult ragResult = ragService.chat(sessionId, request.getMessage());
            String aiReply = ragResult.reply;

            // 4. Lưu tin nhắn AI
            AiMessage aiMsg = new AiMessage();
            aiMsg.setConversation(conversation);
            aiMsg.setRole(AiMessage.MessageRole.ASSISTANT);
            aiMsg.setContent(aiReply);
            messageRepository.save(aiMsg);

            // 5. Lấy sản phẩm gợi ý từ chính danh sách sản phẩm mà AI đã dùng làm Context
            List<ChatDto.ProductSuggestion> suggestions = buildSuggestionsFromProducts(ragResult.contextProducts);

            // 6. Trả response
            ChatDto.ChatResponse response = new ChatDto.ChatResponse();
            response.setReply(aiReply);
            response.setSessionId(sessionId);
            response.setSuggestedProducts(suggestions);
            return response;

        } catch (Exception e) {
            log.error("❌ Lỗi xử lý tin nhắn Chat: {}", e.getMessage(), e);
            ChatDto.ChatResponse errorRes = new ChatDto.ChatResponse();
            errorRes.setReply("Xin lỗi, tôi đang gặp sự cố kỹ thuật 😅 Vui lòng thử lại sau hoặc gọi hotline 0823422987 để được tư vấn trực tiếp!");
            errorRes.setSessionId(sessionId);
            return errorRes;
        }
    }

    /**
     * Lấy lịch sử chat theo sessionId
     */
    public ChatDto.ChatHistoryResponse getHistory(String sessionId) {
        List<AiMessage> messages = messageRepository
                .findByConversationSessionIdOrderByCreatedAtAsc(sessionId);

        List<ChatDto.MessageDto> messageDtos = messages.stream()
                .map(msg -> {
                    ChatDto.MessageDto dto = new ChatDto.MessageDto();
                    dto.setRole(msg.getRole().name());
                    dto.setContent(msg.getContent());
                    dto.setTimestamp(msg.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());

        ChatDto.ChatHistoryResponse history = new ChatDto.ChatHistoryResponse();
        history.setSessionId(sessionId);
        history.setMessages(messageDtos);
        return history;
    }

    /**
     * Xóa lịch sử chat theo sessionId
     */
    @Transactional
    public void deleteHistory(String sessionId) {
        conversationRepository.deleteBySessionId(sessionId);
        log.info("🗑️ Đã xóa lịch sử chat: {}", sessionId);
    }

    /**
     * Lấy toàn bộ lịch sử chat cho Admin
     */
    public List<ChatDto.AdminHistoryDto> getAllHistoryForAdmin() {
        List<AiMessage> messages = messageRepository.findAllByOrderByCreatedAtDesc();
        return messages.stream().map(msg -> {
            ChatDto.AdminHistoryDto dto = new ChatDto.AdminHistoryDto();
            dto.setSessionId(msg.getConversation().getSessionId());
            
            if (msg.getRole() == AiMessage.MessageRole.ASSISTANT) {
                dto.setUserId("AI Assistant");
            } else {
                User user = msg.getConversation().getUser();
                if (user != null) {
                    dto.setUserId(user.getId() + " (" + user.getFullName() + ")");
                } else {
                    dto.setUserId("Khách vãng lai");
                }
            }
            
            dto.setRole(msg.getRole().name());
            dto.setMessage(msg.getContent());
            dto.setTimestamp(msg.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Tạo danh sách sản phẩm gợi ý từ danh sách Product thực tế được AI sử dụng
     */
    private List<ChatDto.ProductSuggestion> buildSuggestionsFromProducts(List<Product> products) {
        if (products == null) return new ArrayList<>();
        return products.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive())) // Chỉ hiển thị thẻ cho sản phẩm đang bán
                .map(p -> new ChatDto.ProductSuggestion(
                        p.getId(),
                        p.getName(),
                        p.getPrice() != null ? p.getPrice().toString() : "0",
                        p.getBrand(),
                        p.getSlug(),
                        p.getImageUrls()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Cắt chuỗi nếu quá dài
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
