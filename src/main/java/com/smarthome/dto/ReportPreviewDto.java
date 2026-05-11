package com.smarthome.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReportPreviewDto {
    private Long id;
    private String fullName;
    private String phone;
    private String shippingAddress;
    private BigDecimal finalAmount;
    private String status;
    private LocalDateTime createdAt;

    public ReportPreviewDto() {
    }

    public ReportPreviewDto(Long id, String fullName, String phone, String shippingAddress, BigDecimal finalAmount, String status, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.shippingAddress = shippingAddress;
        this.finalAmount = finalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
