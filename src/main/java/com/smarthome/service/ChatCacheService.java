package com.smarthome.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Dịch vụ bộ nhớ đệm (Caching) cho chatbot.
 * Lưu trữ câu trả lời AI theo nội dung câu hỏi để tránh gọi API lặp lại.
 */
@Service
public class ChatCacheService {

    // Bộ nhớ đệm lưu tối đa 1000 câu hỏi, tự xóa sau 15 phút
    private final Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build();

    /**
     * Lấy câu trả lời từ cache
     */
    public String get(String query) {
        if (query == null) return null;
        return cache.getIfPresent(normalize(query));
    }

    /**
     * Lưu câu trả lời vào cache
     */
    public void put(String query, String answer) {
        if (query != null && answer != null) {
            cache.put(normalize(query), answer);
        }
    }

    /**
     * Xóa cache (dùng khi cập nhật dữ liệu cửa hàng)
     */
    public void clear() {
        cache.invalidateAll();
    }

    private String normalize(String text) {
        return text.toLowerCase().trim();
    }
}
