package com.smarthome.event;

import com.smarthome.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener lắng nghe các thay đổi của sản phẩm để cập nhật dữ liệu AI (Embeddings).
 */
@Component
public class ProductEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProductEventListener.class);
    private final EmbeddingService embeddingService;

    public ProductEventListener(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    /**
     * Tự động refresh embedding khi có sự thay đổi sản phẩm.
     * Chạy bất đồng bộ (@Async) để không làm chậm quá trình lưu sản phẩm chính.
     */
    @EventListener
    @Async
    public void handleProductChanged(ProductChangedEvent event) {
        log.info("🔔 [AI Sync] Nhận tín hiệu thay đổi sản phẩm ({}): {}. Đang cập nhật bộ nhớ AI...",
                event.getAction(), event.getProduct().getName());
        
        try {
            embeddingService.refreshEmbeddings();
            log.info("✅ [AI Sync] Đã cập nhật xong bộ nhớ AI cho sản phẩm: {}", event.getProduct().getName());
        } catch (Exception e) {
            log.error("❌ [AI Sync] Lỗi khi cập nhật bộ nhớ AI: {}", e.getMessage());
        }
    }
}
