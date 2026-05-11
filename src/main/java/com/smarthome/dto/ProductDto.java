package com.smarthome.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductDto {

    public static class CreateRequest {
        @NotBlank(message = "Tên sản phẩm không được để trống")
        private String name;

        private String description;

        @NotNull(message = "Giá không được để trống")
        @Positive(message = "Giá phải lớn hơn 0")
        private BigDecimal price;

        private BigDecimal salePrice;
        private BigDecimal costPrice;

        @NotNull(message = "Số lượng không được để trống")
        private Integer stockQuantity;

        private Long categoryId;
        private String brand;
        private String imageUrls;  // JSON string
        private String specs;      // JSON string
        private Boolean isFlashSale = false;
        private LocalDateTime flashSaleEndDate;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public BigDecimal getSalePrice() { return salePrice; }
        public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
        public BigDecimal getCostPrice() { return costPrice; }
        public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
        public Integer getStockQuantity() { return stockQuantity; }
        public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public String getImageUrls() { return imageUrls; }
        public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
        public String getSpecs() { return specs; }
        public void setSpecs(String specs) { this.specs = specs; }
        public Boolean getIsFlashSale() { return isFlashSale; }
        public void setIsFlashSale(Boolean isFlashSale) { this.isFlashSale = isFlashSale; }
        public LocalDateTime getFlashSaleEndDate() { return flashSaleEndDate; }
        public void setFlashSaleEndDate(LocalDateTime flashSaleEndDate) { this.flashSaleEndDate = flashSaleEndDate; }
    }

    public static class UpdateRequest {
        private String name;
        private String description;
        private BigDecimal price;
        private BigDecimal salePrice;
        private BigDecimal costPrice;
        private Integer stockQuantity;
        private Long categoryId;
        private String brand;
        private String imageUrls;
        private String specs;
        private Boolean isActive;
        private Boolean isFlashSale;
        private LocalDateTime flashSaleEndDate;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public BigDecimal getSalePrice() { return salePrice; }
        public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
        public BigDecimal getCostPrice() { return costPrice; }
        public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
        public Integer getStockQuantity() { return stockQuantity; }
        public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public String getImageUrls() { return imageUrls; }
        public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
        public String getSpecs() { return specs; }
        public void setSpecs(String specs) { this.specs = specs; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public Boolean getIsFlashSale() { return isFlashSale; }
        public void setIsFlashSale(Boolean isFlashSale) { this.isFlashSale = isFlashSale; }
        public LocalDateTime getFlashSaleEndDate() { return flashSaleEndDate; }
        public void setFlashSaleEndDate(LocalDateTime flashSaleEndDate) { this.flashSaleEndDate = flashSaleEndDate; }
    }

    public static class Response {
        private Long id;
        private String name;
        private String slug;
        private String sku;
        private String description;
        private BigDecimal price;
        private BigDecimal salePrice;
        private BigDecimal costPrice;
        private Integer stockQuantity;
        private Long categoryId;
        private String categoryName;
        private String brand;
        private String imageUrls;
        private String specs;
        private Boolean isActive;
        private Boolean isFlashSale;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime flashSaleEndDate;
        private Integer viewCount;
        private Double avgRating;
        private Long reviewCount;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public BigDecimal getSalePrice() { return salePrice; }
        public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
        public BigDecimal getCostPrice() { return costPrice; }
        public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
        public Integer getStockQuantity() { return stockQuantity; }
        public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public String getImageUrls() { return imageUrls; }
        public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
        public String getSpecs() { return specs; }
        public void setSpecs(String specs) { this.specs = specs; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public Boolean getIsFlashSale() { return isFlashSale; }
        public void setIsFlashSale(Boolean isFlashSale) { this.isFlashSale = isFlashSale; }
        public LocalDateTime getFlashSaleEndDate() { return flashSaleEndDate; }
        public void setFlashSaleEndDate(LocalDateTime flashSaleEndDate) { this.flashSaleEndDate = flashSaleEndDate; }
        public Integer getViewCount() { return viewCount; }
        public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
        public Double getAvgRating() { return avgRating; }
        public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }
        public Long getReviewCount() { return reviewCount; }
        public void setReviewCount(Long reviewCount) { this.reviewCount = reviewCount; }
    }
}
