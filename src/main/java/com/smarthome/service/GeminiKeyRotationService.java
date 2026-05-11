package com.smarthome.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Quản lý Gemini API Key.
 * Hỗ trợ xoay vòng nếu có nhiều key, hoặc quản lý cooldown nếu chỉ có 1 key.
 */
@Service
public class GeminiKeyRotationService {

    private static final Logger log = LoggerFactory.getLogger(GeminiKeyRotationService.class);
    private static final long COOLDOWN_DURATION_MS = 10 * 1000L; // Giảm xuống 10 giây để debug nhanh

    @Value("${app.ai.gemini.api-key:}")
    private String singleKey;

    @Value("${app.ai.gemini.api-keys:}")
    private String multiKeysRaw;

    @Value("${app.ai.gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${app.ai.groq.enabled:false}")
    private boolean groqEnabled;

    @Value("${app.ai.groq.api-key:}")
    private String groqApiKey;

    @Value("${app.ai.groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    private final List<KeyInfo> keys = new ArrayList<>();
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        // 1. Thử load từ multi-keys trước
        if (multiKeysRaw != null && !multiKeysRaw.isBlank()) {
            for (String key : multiKeysRaw.split(",")) {
                if (!key.trim().isEmpty() && !key.contains("REPLACE_KEY")) {
                    keys.add(new KeyInfo(key.trim()));
                }
            }
        }
        
        // 2. Nếu vẫn trống, load từ single key (hỗ trợ cả trường hợp người dùng nhập danh sách vào đây)
        if (keys.isEmpty() && singleKey != null && !singleKey.isBlank()) {
            for (String key : singleKey.split(",")) {
                if (!key.trim().isEmpty()) {
                    keys.add(new KeyInfo(key.trim()));
                }
            }
        }

        log.info("🔑 Đã cấu hình {} Gemini API key", keys.size());
    }

    public ChatLanguageModel getAvailableModel() {
        // 1. Ưu tiên sử dụng Groq (Tốc độ cao nhất)
        if (groqEnabled && groqApiKey != null && !groqApiKey.isBlank() && !groqApiKey.contains("YOUR_GROQ")) {
            log.info("🚀 Using Primary AI: Groq ({})", groqModel);
            return OpenAiChatModel.builder()
                    .baseUrl("https://api.groq.com/openai/v1")
                    .apiKey(groqApiKey)
                    .modelName(groqModel)
                    .temperature(0.7)
                    .maxTokens(1024)
                    .build();
        }

        // 2. Dự phòng: Xoay vòng Gemini nếu Groq không khả dụng hoặc tắt
        if (!keys.isEmpty()) {
            int total = keys.size();
            int start = currentIndex.get();

            for (int i = 0; i < total; i++) {
                int idx = (start + i) % total;
                KeyInfo info = keys.get(idx);

                if (info.isAvailable()) {
                    currentIndex.set((idx + 1) % total);
                    log.info("🤖 Using Backup AI: Gemini ({})", geminiModel);
                    return GoogleAiGeminiChatModel.builder()
                            .apiKey(info.key)
                            .modelName(geminiModel)
                            .temperature(0.7)
                            .maxOutputTokens(1024)
                            .build();
                }
            }
        }

        return null;
    }

    public void markCurrentKeyExhausted() {
        if (keys.size() <= 1) return; // Nếu chỉ có 1 key, không mark exhausted để tránh bị kẹt cứng
        int failedIdx = (currentIndex.get() - 1 + keys.size()) % keys.size();
        keys.get(failedIdx).markExhausted();
        log.warn("🚫 API Key #{} đã hết quota, tạm nghỉ 10 giây", failedIdx + 1);
    }

    public int getTotalKeys() {
        return keys.size();
    }

    public String getStatusSummary() {
        long avail = keys.stream().filter(KeyInfo::isAvailable).count();
        return String.format("Keys: %d tổng / %d sẵn sàng", keys.size(), avail);
    }

    private static class KeyInfo {
        final String key;
        volatile long cooldownUntil = 0;

        KeyInfo(String key) { this.key = key; }
        boolean isAvailable() { return Instant.now().toEpochMilli() >= cooldownUntil; }
        void markExhausted() { this.cooldownUntil = Instant.now().toEpochMilli() + COOLDOWN_DURATION_MS; }
    }
}
