package com.smarthome.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CategoryDto {

    public static class CreateRequest {
        @NotBlank(message = "Tên danh mục không được để trống")
        private String name;
        private Long parentId;
        private String description;
        private String imageUrl;
        private Integer sortOrder;

        public CreateRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class Response {
        private Long id;
        private String name;
        private String slug;
        private String description;
        private String imageUrl;
        private Integer sortOrder;
        private Boolean isActive;
        private Long parentId;
        private String parentName;
        private List<Response> children;
        private Long productCount;

        public Response() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        public String getParentName() { return parentName; }
        public void setParentName(String parentName) { this.parentName = parentName; }
        public List<Response> getChildren() { return children; }
        public void setChildren(List<Response> children) { this.children = children; }
        public Long getProductCount() { return productCount; }
        public void setProductCount(Long productCount) { this.productCount = productCount; }
    }
}
