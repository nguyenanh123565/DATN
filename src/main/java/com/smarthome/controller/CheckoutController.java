package com.smarthome.controller;

import com.smarthome.common.ApiResponse;
import com.smarthome.dto.OrderRequestDto;
import com.smarthome.dto.OrderResponseDto;
import com.smarthome.dto.VoucherApplyRequest;
import com.smarthome.repository.UserRepository;
import com.smarthome.service.CartService;
import com.smarthome.service.OrderService;
import com.smarthome.service.ShippingService;
import com.smarthome.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/checkout")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Checkout", description = "API Đặt hàng")
public class CheckoutController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final VoucherService voucherService;
    private final ShippingService shippingService;

    // Explicit constructor to bypass Lombok
    public CheckoutController(OrderService orderService, UserRepository userRepository, CartService cartService, VoucherService voucherService, ShippingService shippingService) {
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.voucherService = voucherService;
        this.shippingService = shippingService;
    }

    @PostMapping("/apply-discount")
    @Operation(summary = "Kiểm tra và áp dụng mã giảm giá")
    public ResponseEntity<ApiResponse<VoucherService.VoucherApplyResult>> applyDiscount(
            @Valid @RequestBody VoucherApplyRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = getUserId(userDetails);
        BigDecimal subtotal = cartService.calculateSubtotal(userId);
        VoucherService.VoucherApplyResult result = voucherService.applyVoucher(request.getCode(), subtotal);
        
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping
    @Operation(summary = "Tiến hành đặt hàng")
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            @Valid @RequestBody OrderRequestDto req,
            @AuthenticationPrincipal UserDetails userDetails) {

        OrderResponseDto response = orderService.createOrder(req, getUserId(userDetails));
        return ResponseEntity.ok(ApiResponse.ok("Đặt hàng thành công", response));
    }

    @GetMapping("/shipping-fee")
    @Operation(summary = "Tính toán phí vận chuyển thực tế")
    public ResponseEntity<ApiResponse<BigDecimal>> getShippingFee(
            @RequestParam String address,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = getUserId(userDetails);
        BigDecimal subtotal = cartService.calculateSubtotal(userId);
        BigDecimal fee = shippingService.calculateShippingFee(address, subtotal);
        
        return ResponseEntity.ok(ApiResponse.ok(fee));
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
    }
}
