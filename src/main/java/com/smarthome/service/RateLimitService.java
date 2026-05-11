package com.smarthome.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dịch vụ giới hạn tốc độ (Rate Limiting) để tránh spam chatbot.
 * Sử dụng thuật toán Token Bucket.
 */
@Service
public class RateLimitService {

    @Value("${app.ai.chat.rate-limit.max-requests:5}")
    private int maxRequests;

    @Value("${app.ai.chat.rate-limit.window-seconds:10}")
    private int windowSeconds;

    // Mỗi user có một "xô" token riêng
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Kiểm tra xem user có được phép gửi tin nhắn không.
     * 
     * @param sessionId ID phiên chat của user
     * @return true nếu được phép, false nếu vượt quá giới hạn
     */
    public boolean tryConsume(String sessionId) {
        Bucket bucket = buckets.computeIfAbsent(sessionId, this::createNewBucket);
        return bucket.tryConsume(1);
    }

    private Bucket createNewBucket(String key) {
        // Mỗi windowSeconds hồi lại maxRequests request
        Bandwidth limit = Bandwidth.classic(maxRequests, Refill.intervally(maxRequests, Duration.ofSeconds(windowSeconds)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
