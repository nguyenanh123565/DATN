package com.smarthome.service;

import com.smarthome.entity.Product;
import com.smarthome.repository.ProductRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service quản lý embedding sản phẩm.
 * - Khi app khởi động: load sản phẩm từ MySQL → tạo embedding → lưu vào RAM
 * - Khi user hỏi: tạo embedding câu hỏi → tìm sản phẩm tương tự (similarity search)
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final ProductRepository productRepository;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    
    public EmbeddingService(ProductRepository productRepository, 
                        EmbeddingModel embeddingModel, 
                        EmbeddingStore<TextSegment> embeddingStore) {
        this.productRepository = productRepository;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @Value("${app.ai.rag.top-k:5}")
    private int topK;

    @Value("${app.ai.rag.min-score:0.5}")
    private double minScore;

    /**
     * Tự động load sản phẩm và tạo embedding khi app đã khởi động xong.
     * Chạy bất đồng bộ để không chặn việc khởi động ứng dụng.
     * - [x] Kích hoạt `@EnableAsync` trong `SmartHomeApplication.java`
     * - [x] Cấu hình `@Async` cho `EmbeddingService.loadProductEmbeddings()`
     * - [/] Tạo `ProductEventListener.java` để tự động cập nhật AI khi sản phẩm thay đổi
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initEmbeddings() {
        log.info("🔄 [AI] Đang tạo bộ nhớ thông minh (Embedding) cho sản phẩm (Chạy Offline)...");
        loadProductEmbeddings();
    }

    /**
     * Load tất cả sản phẩm từ MySQL, tạo embedding và lưu vào store
     */
    public void loadProductEmbeddings() {
        // Lấy tất cả sản phẩm để AI có thể nhận biết cả sản phẩm đã ngừng bán
        List<Product> products = productRepository.findAll();

        if (products.isEmpty()) {
            log.warn("⚠️ Không có sản phẩm nào trong database để tạo embedding!");
            return;
        }

        int count = 0;
        for (Product product : products) {
            try {
                // Tạo text mô tả đầy đủ cho sản phẩm
                String productText = buildProductText(product);

                // Tạo TextSegment với metadata (chứa product ID để truy vấn lại)
                Metadata metadata = Metadata.from("productId", product.getId().toString());
                metadata.put("productName", product.getName());
                metadata.put("productPrice", product.getPrice().toString());
                if (product.getBrand() != null) {
                    metadata.put("productBrand", product.getBrand());
                }
                if (product.getSlug() != null) {
                    metadata.put("productSlug", product.getSlug());
                }
                if (product.getImageUrls() != null) {
                    metadata.put("productImageUrls", product.getImageUrls());
                }
                TextSegment segment = TextSegment.from(productText, metadata);

                // Tạo embedding vector
                Response<Embedding> embeddingResponse = embeddingModel.embed(segment);
                Embedding embedding = embeddingResponse.content();

                // Lưu vào store
                embeddingStore.add(embedding, segment);
                count++;
            } catch (Exception e) {
                log.error("Lỗi tạo embedding cho sản phẩm {}: {}", product.getName(), e.getMessage());
            }
        }

        log.info("✅ Đã tạo embedding cho {}/{} sản phẩm", count, products.size());
    }

    /**
     * Tìm kiếm sản phẩm liên quan dựa trên câu hỏi (Similarity Search)
     */
    public List<EmbeddingMatch<TextSegment>> findRelevantProducts(String query) {
        // Tạo embedding cho câu hỏi
        Response<Embedding> queryEmbedding = embeddingModel.embed(query);

        // Tìm kiếm top-K sản phẩm tương tự
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding.content())
                .maxResults(topK)
                .minScore(minScore)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

        log.debug("🔍 Tìm thấy {} sản phẩm liên quan cho: '{}'",
                result.matches().size(), query);

        return result.matches();
    }

    /**
     * Xây dựng text mô tả sản phẩm để tạo embedding
     * Kết hợp: tên + mô tả + giá + brand + specs
     */
    private String buildProductText(Product product) {
        StringBuilder sb = new StringBuilder();

        sb.append("Sản phẩm: ").append(product.getName()).append(". ");
        
        if (Boolean.TRUE.equals(product.getIsActive())) {
            sb.append("Trạng thái: Đang bán. ");
        } else {
            sb.append("Trạng thái: Ngừng kinh doanh/Hết hàng. ");
        }

        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            sb.append("Mô tả: ").append(product.getDescription()).append(". ");
        }

        sb.append("Giá: ").append(product.getPrice()).append(" VNĐ. ");

        if (product.getSalePrice() != null) {
            sb.append("Giá khuyến mãi: ").append(product.getSalePrice()).append(" VNĐ. ");
        }

        if (product.getBrand() != null && !product.getBrand().isEmpty()) {
            sb.append("Thương hiệu: ").append(product.getBrand()).append(". ");
        }

        if (product.getSpecs() != null && !product.getSpecs().isEmpty()) {
            sb.append("Thông số: ").append(product.getSpecs()).append(". ");
        }

        return sb.toString();
    }

    /**
     * Reload lại embedding (dùng khi admin thêm/sửa sản phẩm)
     */
    public void refreshEmbeddings() {
        log.info("🔄 Đang refresh embedding sản phẩm...");
        // Xóa store cũ và tạo mới
        // InMemoryEmbeddingStore không có method clear(), tạo mới
        loadProductEmbeddings();
        log.info("✅ Đã refresh embedding thành công!");
    }
}
