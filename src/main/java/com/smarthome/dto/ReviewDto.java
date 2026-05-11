package com.smarthome.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class ReviewDto {

    public static class CreateRequest {
        @NotNull(message = "ID sản phẩm không được để trống")
        private Long productId;

        @NotNull @Min(1) @Max(5)
        private Integer rating;

        private String comment;

        public CreateRequest() {}

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public static class Response {
        private Long id;
        private Long productId;
        private String userName;
        private Integer rating;
        private String comment;
        private Boolean isApproved;
        private LocalDateTime createdAt;

        public Response() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public Boolean getIsApproved() { return isApproved; }
        public void setIsApproved(Boolean isApproved) { this.isApproved = isApproved; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
