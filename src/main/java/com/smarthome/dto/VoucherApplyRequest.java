package com.smarthome.dto;

import jakarta.validation.constraints.NotBlank;

public class VoucherApplyRequest {
    @NotBlank(message = "Mã giảm giá không được để trống")
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
