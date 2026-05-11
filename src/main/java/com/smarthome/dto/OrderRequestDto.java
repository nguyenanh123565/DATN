package com.smarthome.dto;

import jakarta.validation.constraints.NotBlank;

public class OrderRequestDto {

    @NotBlank(message = "Tên khách hàng không được để trống")
    private String customerName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    private String notes;

    private String discountCode;
    private java.math.BigDecimal shippingFee;

    // Manual getters and setters
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getDiscountCode() { return discountCode; }
    public void setDiscountCode(String discountCode) { this.discountCode = discountCode; }

    public java.math.BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(java.math.BigDecimal shippingFee) { this.shippingFee = shippingFee; }
}
