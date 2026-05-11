package com.smarthome.service;

import com.smarthome.entity.Product;
import com.smarthome.entity.User;
import com.smarthome.repository.ProductRepository;
import com.smarthome.repository.UserRepository;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RAG Orchestrator Service - Triển khai kiến trúc Senior Backend
 * Các lớp bảo vệ:
 * 1. Rate Limiting (Bucket4j)
 * 2. Caching (Caffeine)
 * 3. Offline Shortcut (Order status/Product list)
 * 4. AI Generation với Key Rotation & Retry
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final GeminiKeyRotationService keyRotationService;
    private final EmbeddingService embeddingService;
    private final IntentParserService intentParser;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final QuotaFallbackService quotaFallbackService;
    private final ChatCacheService cacheService;
    private final RateLimitService rateLimitService;

    public RagService(GeminiKeyRotationService keyRotationService,
                      EmbeddingService embeddingService,
                      IntentParserService intentParser,
                      ProductRepository productRepository,
                      UserRepository userRepository,
                      QuotaFallbackService quotaFallbackService,
                      ChatCacheService cacheService,
                      RateLimitService rateLimitService) {
        this.keyRotationService = keyRotationService;
        this.embeddingService = embeddingService;
        this.intentParser = intentParser;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.quotaFallbackService = quotaFallbackService;
        this.cacheService = cacheService;
        this.rateLimitService = rateLimitService;
    }

    @Value("${app.ai.rag.enabled:true}")
    private boolean ragEnabled;

    @Value("${app.ai.rag.top-k:5}")
    private int topK;

    private static final NumberFormat VND_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    private static final String SYSTEM_PROMPT = """
            Bạn là SmartHome AI Advisor - chuyên gia tư vấn cao cấp về nhà thông minh. Tôi sẽ cung cấp danh sách sản phẩm từ cửa hàng.
            NHIỆM VỤ:
            1. Phân tích nhu cầu của khách hàng dựa trên câu hỏi.
            2. Sử dụng dữ liệu sản phẩm được cung cấp để so sánh và đưa ra lựa chọn tốt nhất.
            3. Luôn cung cấp đầy đủ: **Tên sản phẩm**, Giá bán, và ít nhất 2 ưu điểm nổi bật.
            4. Nếu tìm thấy sản phẩm trong danh sách nhưng dữ liệu thiếu mô tả chi tiết hoặc thông số kỹ thuật (như 16A, công suất, pin...), hãy sử dụng kiến thức của bạn về model đó để bổ sung thông tin. Khi đó hãy thông báo rõ: "Dựa trên thông số kỹ thuật của model này, sản phẩm có các tính năng...".
            5. QUAN TRỌNG: Tuyệt đối không tự bịa ra tính năng không có thật (ví dụ: không được gán tính năng nhận diện khuôn mặt cho model chỉ hỗ trợ vân tay). Phải đảm bảo tính chính xác 100% theo model thực tế.
            6. QUAN TRỌNG: Tuyệt đối không được giới thiệu hoặc khuyên mua các sản phẩm mà cửa hàng không có trong danh sách dữ liệu cung cấp.
            7. Nếu không có dữ liệu phù hợp trong danh sách, hãy gợi ý sản phẩm gần nhất hoặc mời khách liên hệ hotline 0823422987.
            8. QUAN TRỌNG: Nếu khách hỏi đúng sản phẩm có "Trạng thái: Ngừng kinh doanh/Hết hàng", BẮT BUỘC TRẢ LỜI: "Tạm thời mặt hàng này đang hết, bạn đợi ít ngày hoặc là bạn xem sản phẩm tương tự" rồi mới gợi ý các sản phẩm "Đang bán" khác.
            """;

    public static class RagResult {
        public String reply;
        public List<Product> contextProducts;
        public RagResult(String reply, List<Product> contextProducts) {
            this.reply = reply;
            this.contextProducts = contextProducts;
        }
    }

    /**
     * Xử lý câu hỏi chatbot với kiến trúc nhiều lớp bảo vệ
     */
    public RagResult chat(String sessionId, String userMessage) {
        log.info("📩 Chat request [{}]: '{}'", sessionId, userMessage);

        // LỚP 1: RATE LIMITING (5 msg / 10s)
        if (!rateLimitService.tryConsume(sessionId)) {
            return new RagResult("Bạn đang hỏi hơi nhanh rồi đấy 😅 Hãy đợi vài giây để tôi chuẩn bị câu trả lời nhé!", new ArrayList<>());
        }

        // LỚP 2: CACHING (Caffeine)
        String cachedResponse = cacheService.get(userMessage);
        if (cachedResponse != null) {
            log.info("🚀 Cache Hit cho: '{}'", userMessage);
            return new RagResult(cachedResponse, new ArrayList<>()); // TODO: Cache products too if needed
        }

        // Lấy thông tin user hiện tại (nếu đã đăng nhập)
        Long userId = getCurrentUserId();

        // Bước 0: Nếu RAG bị tắt, trả về response AI thuần túy
        if (!ragEnabled) {
            String reply = callGeminiWithRotation(userId, userMessage, userMessage, null, new ArrayList<>());
            return new RagResult(reply, new ArrayList<>());
        }

        // Bước 1: Phân tích Intent
        IntentParserService.Intent intent = intentParser.parse(userMessage);
        
        // LỚP 3: OFFLINE SHORTCUT / FALLBACK
        // Nếu là câu hỏi về ĐƠN HÀNG hoặc các câu hỏi tra cứu cơ bản, xử lý offline luôn
        if (shouldHandleOffline(userMessage, intent)) {
            log.info("⚡ Shortcut: Xử lý offline cho intent: {}", intent);
            List<Product> products = intent.hasFilter() ? productRepository.filterForAI(
                    intent.categorySlug, intent.brand, intent.minPrice, intent.maxPrice
            ) : null;
            
            String offlineResponse = quotaFallbackService.generateSmartFallback(userId, userMessage, intent, products);
            cacheService.put(userMessage, offlineResponse); // Vẫn lưu cache cho kết quả offline
            return new RagResult(offlineResponse, products != null ? products : new ArrayList<>());
        }

        // Bước 2: Truy xuất dữ liệu (Retrieval) - Kết hợp cả Filter và Vector Search
        Set<Product> allRelevantProducts = new LinkedHashSet<>(); // Dùng Set để tránh trùng lặp

        // 2.1: Lấy sản phẩm theo bộ lọc (Brand, Category, Price...)
        if (intent.hasFilter()) {
            List<Product> filtered = productRepository.filterForAI(
                    intent.categorySlug, intent.brand, intent.minPrice, intent.maxPrice
            );
            if (filtered != null) {
                allRelevantProducts.addAll(filtered);
            }
        }

        // 2.2: Lấy sản phẩm theo Vector Search (Tìm kiếm theo tên model cụ thể)
        List<EmbeddingMatch<TextSegment>> matches = embeddingService.findRelevantProducts(userMessage);
        if (matches != null && !matches.isEmpty()) {
            List<Long> matchIds = matches.stream()
                    .map(m -> Long.parseLong(m.embedded().metadata().getString("productId")))
                    .collect(Collectors.toList());
            List<Product> vectorProducts = productRepository.findAllById(matchIds);
            allRelevantProducts.addAll(vectorProducts);
        }

        // Chuyển Set thành List và giới hạn số lượng để không làm tràn Prompt
        List<Product> contextProducts = allRelevantProducts.stream()
                .limit(topK * 2L)
                .collect(Collectors.toList());

        String context = buildContextFromProducts(contextProducts);

        // LỚP 4: AI GENERATION (Với Key Rotation & Retry nội bộ)
        String fullPrompt = buildFullPrompt(context, userMessage, intent);
        String aiResponse = callGeminiWithRotation(userId, userMessage, fullPrompt, intent, contextProducts);
        
        // Lưu cache kết quả AI thành công
        if (aiResponse != null && !aiResponse.contains("xin lỗi")) {
            cacheService.put(userMessage, aiResponse);
        }
        
        return new RagResult(aiResponse, contextProducts);
    }

    /**
     * Quyết định xem có nên xử lý offline không để tiết kiệm API
     */
    private boolean shouldHandleOffline(String msg, IntentParserService.Intent intent) {
        String lower = msg.toLowerCase();
        // Chỉ xử lý offline cho các câu hỏi về ĐƠN HÀNG của tôi (vì AI không có quyền truy cập DB trực tiếp cho dữ liệu cá nhân này)
        if (lower.contains("đơn hàng") || lower.contains("đã mua") || lower.contains("tình trạng giao")) return true;
        
        // Không xử lý offline cho tư vấn sản phẩm nữa, hãy để AI làm việc đó thông minh hơn
        return false;
    }

    private String callGeminiWithRotation(Long userId, String userMsg, String prompt, IntentParserService.Intent intent, List<Product> products) {
        int totalKeys = keyRotationService.getTotalKeys();
        
        for (int attempt = 0; attempt < totalKeys; attempt++) {
            ChatLanguageModel model = keyRotationService.getAvailableModel();
            
            if (model == null) break; // Không còn key nào

            try {
                String response = model.generate(prompt);
                log.info("✅ Gemini Success (Attempt #{})", attempt + 1);
                return response;
            } catch (Exception e) {
                log.error("❌ Lỗi AI chi tiết: ", e);
                keyRotationService.markCurrentKeyExhausted();
                
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("429")) {
                    errorMsg = "Hết hạn mức (Rate Limit). Vui lòng đợi vài giây.";
                }
                
                return "⚠️ Lỗi AI (" + keyRotationService.getStatusSummary() + "): " + errorMsg + "\n\n" + 
                       "Dưới đây là kết quả tìm kiếm từ hệ thống:\n" +
                       quotaFallbackService.generateSmartFallback(userId, userMsg, intent, products);
            }
        }

        // ULTIMATE FALLBACK: Khi tất cả đều thất bại (hết key hoặc lỗi mạng)
        log.warn("🆘 AI Failed! Sử dụng fallback thông minh cuối cùng.");
        return "⚠️ Lỗi AI: " + keyRotationService.getStatusSummary() + ".\n\n" + quotaFallbackService.generateSmartFallback(userId, userMsg, intent, products);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String email = auth.getName();
            return userRepository.findByEmail(email).map(User::getId).orElse(null);
        }
        return null;
    }

    private String buildContextFromProducts(List<Product> products) {
        if (products.isEmpty()) return "Không tìm thấy sản phẩm.";
        return products.stream()
                .limit(topK * 2L)
                .map(p -> String.format("- %s: %sđ | %s", p.getName(), VND_FORMAT.format(p.getPrice()), p.getBrand()))
                .collect(Collectors.joining("\n"));
    }

    private String buildContextFromMatches(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches.isEmpty()) return "Không tìm thấy dữ liệu liên quan.";
        return matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Lấy danh sách sản phẩm liên quan (dùng cho response gợi ý sản phẩm)
     */
    public List<EmbeddingMatch<TextSegment>> getRelevantProducts(String query) {
        return embeddingService.findRelevantProducts(query);
    }

    private String buildFullPrompt(String context, String userMessage, IntentParserService.Intent intent) {
        return String.format("%s\n=== DỮ LIỆU CỬA HÀNG ===\n%s\n===\nCâu hỏi: %s", 
                SYSTEM_PROMPT, context, userMessage);
    }
}
