package com.smarthome.service;

import com.smarthome.common.ResourceNotFoundException;
import com.smarthome.dto.ProductDto;
import com.smarthome.entity.Category;
import com.smarthome.entity.Product;
import com.smarthome.repository.CategoryRepository;
import com.smarthome.repository.ProductRepository;
import com.smarthome.repository.ReviewRepository;
import com.smarthome.event.ProductChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.text.Normalizer;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final ReviewRepository reviewRepo;
    private final ApplicationEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepo, 
                        CategoryRepository categoryRepo, 
                        ReviewRepository reviewRepo, 
                        ApplicationEventPublisher eventPublisher) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.reviewRepo = reviewRepo;
        this.eventPublisher = eventPublisher;
    }

    public Page<ProductDto.Response> getAll(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return productRepo.findByIsActiveTrue(pageable).map(this::toResponse);
    }

    public Page<ProductDto.Response> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepo.searchProducts(keyword, pageable).map(this::toResponse);
    }

    public Page<ProductDto.Response> getAllAdmin(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return productRepo.findAll(pageable).map(this::toResponse);
    }

    public Page<ProductDto.Response> searchAll(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepo.searchAllProducts(keyword, pageable).map(this::toResponse);
    }

    public Page<ProductDto.Response> getByCategory(Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepo.findByCategoryIdAndIsActiveTrue(categoryId, pageable).map(this::toResponse);
    }

    public ProductDto.Response getById(Long id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));
        product.setViewCount(product.getViewCount() + 1);
        productRepo.save(product);
        return toResponse(product);
    }

    public ProductDto.Response getBySlug(String slug) {
        Product product = productRepo.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm với slug: " + slug));
        return toResponse(product);
    }

    @Transactional
    public ProductDto.Response create(ProductDto.CreateRequest req) {
        Product product = new Product();
        product.setName(req.getName());
        product.setSlug(generateSlug(req.getName()));
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setSalePrice(req.getSalePrice());
        product.setCostPrice(req.getCostPrice());
        product.setStockQuantity(req.getStockQuantity());
        product.setBrand(req.getBrand());
        product.setImageUrls(formatImageUrls(req.getImageUrls()));
        product.setSpecs(req.getSpecs());
        product.setIsActive(true);
        product.setIsFlashSale(Boolean.TRUE.equals(req.getIsFlashSale()));
        product.setFlashSaleEndDate(req.getFlashSaleEndDate());
        product.setViewCount(0);

        if (req.getCategoryId() != null) {
            Category cat = categoryRepo.findById(req.getCategoryId()).orElse(null);
            product.setCategory(cat);
        }

        Product savedProduct = productRepo.save(product);
        eventPublisher.publishEvent(new ProductChangedEvent(this, savedProduct, "CREATE"));
        return toResponse(savedProduct);
    }

    @Transactional
    public ProductDto.Response update(Long id, ProductDto.UpdateRequest req) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));

        if (req.getName() != null)          product.setName(req.getName());
        if (req.getDescription() != null)   product.setDescription(req.getDescription());
        if (req.getPrice() != null)         product.setPrice(req.getPrice());
        if (req.getSalePrice() != null)     product.setSalePrice(req.getSalePrice());
        if (req.getCostPrice() != null)     product.setCostPrice(req.getCostPrice());
        if (req.getStockQuantity() != null) product.setStockQuantity(req.getStockQuantity());
        if (req.getBrand() != null)         product.setBrand(req.getBrand());
        if (req.getImageUrls() != null)     product.setImageUrls(formatImageUrls(req.getImageUrls()));
        if (req.getSpecs() != null)         product.setSpecs(req.getSpecs());
        if (req.getIsActive() != null)      product.setIsActive(req.getIsActive());
        if (req.getIsFlashSale() != null)   product.setIsFlashSale(req.getIsFlashSale());
        if (req.getFlashSaleEndDate() != null) product.setFlashSaleEndDate(req.getFlashSaleEndDate());

        if (req.getCategoryId() != null) {
            categoryRepo.findById(req.getCategoryId()).ifPresent(product::setCategory);
        }

        Product savedProduct = productRepo.save(product);
        eventPublisher.publishEvent(new ProductChangedEvent(this, savedProduct, "UPDATE"));
        return toResponse(savedProduct);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));
        product.setIsActive(false);
        productRepo.save(product); // Soft delete
        eventPublisher.publishEvent(new ProductChangedEvent(this, product, "DELETE"));
    }

    @Transactional
    public void syncFlashSaleTime(LocalDateTime endTime) {
        List<Product> flashSaleProducts = productRepo.findByIsFlashSaleTrue();
        for (Product p : flashSaleProducts) {
            p.setFlashSaleEndDate(endTime);
        }
        productRepo.saveAll(flashSaleProducts);
    }

    public List<ProductDto.Response> getFlashSaleProducts() {
        return productRepo.findByIsFlashSaleTrue().stream()
                .filter(p -> p.getIsActive() != null && p.getIsActive())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ProductDto.Response toResponse(Product p) {
        ProductDto.Response res = new ProductDto.Response();
        res.setId(p.getId());
        res.setName(p.getName());
        res.setSlug(p.getSlug());
        res.setSku(p.getSku());
        res.setDescription(p.getDescription());
        res.setPrice(p.getPrice());
        res.setSalePrice(p.getSalePrice());
        res.setCostPrice(p.getCostPrice());
        res.setStockQuantity(p.getStockQuantity());
        res.setBrand(p.getBrand());
        res.setImageUrls(p.getImageUrls());
        res.setSpecs(p.getSpecs());
        res.setIsActive(p.getIsActive() != null && p.getIsActive());
        res.setIsFlashSale(p.getIsFlashSale() != null && p.getIsFlashSale());
        res.setFlashSaleEndDate(p.getFlashSaleEndDate());
        res.setViewCount(p.getViewCount());
        if (p.getCategory() != null) {
            res.setCategoryId(p.getCategory().getId());
            res.setCategoryName(p.getCategory().getName());
        }
        res.setAvgRating(reviewRepo.avgRatingByProductId(p.getId()));
        res.setReviewCount(reviewRepo.countApprovedByProductId(p.getId()));
        return res;
    }

    private String generateSlug(String name) {
        String slug = Normalizer.normalize(name, Normalizer.Form.NFD);
        slug = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(slug).replaceAll("");
        slug = slug.toLowerCase()
                   .replaceAll("[đĐ]", "d")
                   .replaceAll("[^a-z0-9\\s-]", "")
                   .replaceAll("\\s+", "-")
                   .replaceAll("-+", "-");
        // Đảm bảo slug unique
        String base = slug;
        int counter = 1;
        while (productRepo.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    private String formatImageUrls(String imageUrls) {
        if (imageUrls == null || imageUrls.trim().isEmpty()) return null;
        String raw = imageUrls.trim();
        
        // Nếu đã là JSON array thì trả về luôn
        if (raw.startsWith("[")) return raw;
        
        // Nếu là chuỗi phân cách bởi dấu phẩy
        String[] urls = raw.split(",");
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < urls.length; i++) {
            sb.append("\"").append(urls[i].trim()).append("\"");
            if (i < urls.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
