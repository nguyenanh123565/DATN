package com.smarthome.repository;

import com.smarthome.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    Page<Product> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    List<Product> findByIsActiveTrue();

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchAllProducts(@Param("keyword") String keyword, Pageable pageable);

    boolean existsBySlug(String slug);
    boolean existsBySku(String sku);
    long countByCategoryIdAndIsActiveTrue(Long categoryId);

    Page<Product> findByIsFeaturedTrueAndIsActiveTrue(Pageable pageable);

    /**
     * Pre-filter sản phẩm cho AI Chat theo giá và danh mục.
     * Các tham số nullable — nếu null thì bỏ qua điều kiện đó.
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true " +
           "AND (:categorySlug IS NULL OR p.category.slug = :categorySlug) " +
           "AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "ORDER BY p.viewCount DESC")
    List<Product> filterForAI(
            @Param("categorySlug") String categorySlug,
            @Param("brand") String brand,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    List<Product> findByIsFlashSaleTrue();
}
