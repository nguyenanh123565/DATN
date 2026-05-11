package com.smarthome.dto;




import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponseDto {
    private Long id;
    private BigDecimal totalAmount;
    private BigDecimal finalAmount;
    private BigDecimal discountAmount;      // tổng giảm giá (voucher + rank)
    private BigDecimal loyaltyDiscountAmount; // phần giảm do rank
    private String loyaltyRankApplied;     // hạng đã áp dụng
    private String status;
    private Long userId;
    private String customerName;
    private String phone;
    private String address;
    private String notes;
    private BigDecimal shippingFee;
    private List<CartItemDto> items;
    private java.time.LocalDateTime createdAt;

    // Manual Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getLoyaltyDiscountAmount() { return loyaltyDiscountAmount; }
    public void setLoyaltyDiscountAmount(BigDecimal loyaltyDiscountAmount) { this.loyaltyDiscountAmount = loyaltyDiscountAmount; }

    public String getLoyaltyRankApplied() { return loyaltyRankApplied; }
    public void setLoyaltyRankApplied(String loyaltyRankApplied) { this.loyaltyRankApplied = loyaltyRankApplied; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }

    public List<CartItemDto> getItems() { return items; }
    public void setItems(List<CartItemDto> items) { this.items = items; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
}
