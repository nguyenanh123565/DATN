package com.smarthome.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class ShippingService {

    /**
     * Tính phí vận chuyển dựa trên địa chỉ và tổng tiền hàng
     * Hà Nội: < 2tr phí 50k, >= 2tr free
     * Tỉnh khác: < 2.5tr phí 100k, >= 2.5tr free
     */
    public BigDecimal calculateShippingFee(String address, BigDecimal subtotal) {
        if (address == null) address = "";
        
        boolean isHanoi = isHanoi(address);
        BigDecimal hanoiThreshold = new BigDecimal("2000000");
        BigDecimal provinceThreshold = new BigDecimal("2500000");

        if (isHanoi) {
            if (subtotal.compareTo(hanoiThreshold) >= 0) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal("50000");
        } else {
            if (subtotal.compareTo(provinceThreshold) >= 0) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal("100000");
        }
    }

    private boolean isHanoi(String address) {
        String addr = address.toLowerCase();
        return addr.contains("hà nội") || addr.contains("ha noi") || addr.contains("hn");
    }
}
