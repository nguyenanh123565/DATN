package com.smarthome.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class UserDto {

    public static class UpdateProfileRequest {
        @NotBlank(message = "Họ tên không được để trống")
        private String fullName;
        private String phone;
        private String avatarUrl;

        public UpdateProfileRequest() {}

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }

    public static class ChangePasswordRequest {
        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        private String currentPassword;

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @jakarta.validation.constraints.Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
        @jakarta.validation.constraints.Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Mật khẩu phải chứa ít nhất một chữ cái và một chữ số"
        )
        private String newPassword;

        @NotBlank(message = "Xác nhận mật khẩu không được để trống")
        private String confirmPassword;

        public ChangePasswordRequest() {}

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }

    public static class Response {
        private Long id;
        private String email;
        private String fullName;
        private String phone;
        private String avatarUrl;
        private String role;
        private String status;
        private String loyaltyRank;
        private java.math.BigDecimal totalSpent;
        private java.math.BigDecimal loyaltyDiscountRate;
        private java.math.BigDecimal nextRankThreshold;  // ngưỡng tiếp theo để lên hạng
        private java.time.LocalDateTime createdAt;

        public Response() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getLoyaltyRank() { return loyaltyRank; }
        public void setLoyaltyRank(String loyaltyRank) { this.loyaltyRank = loyaltyRank; }
        public java.math.BigDecimal getTotalSpent() { return totalSpent; }
        public void setTotalSpent(java.math.BigDecimal totalSpent) { this.totalSpent = totalSpent; }
        public java.math.BigDecimal getLoyaltyDiscountRate() { return loyaltyDiscountRate; }
        public void setLoyaltyDiscountRate(java.math.BigDecimal loyaltyDiscountRate) { this.loyaltyDiscountRate = loyaltyDiscountRate; }
        public java.math.BigDecimal getNextRankThreshold() { return nextRankThreshold; }
        public void setNextRankThreshold(java.math.BigDecimal nextRankThreshold) { this.nextRankThreshold = nextRankThreshold; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class AddressRequest {
        @NotBlank private String name;
        @NotBlank private String phone;
        @NotBlank private String fullAddress;
        private Boolean isDefault;

        public AddressRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getFullAddress() { return fullAddress; }
        public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }
        public Boolean getIsDefault() { return isDefault; }
        public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    }

    public static class AddressResponse {
        private Long id;
        private String name;
        private String phone;
        private String fullAddress;
        private Boolean isDefault;

        public AddressResponse() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getFullAddress() { return fullAddress; }
        public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }
        public Boolean getIsDefault() { return isDefault; }
        public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    }
}
