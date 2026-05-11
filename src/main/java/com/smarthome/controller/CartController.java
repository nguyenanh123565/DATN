package com.smarthome.controller;

import com.smarthome.common.ApiResponse;
import com.smarthome.dto.CartDto;
import com.smarthome.repository.UserRepository;
import com.smarthome.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cart", description = "API quản lý giỏ hàng")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    public CartController(CartService cartService, UserRepository userRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Xem giỏ hàng hiện tại")
    public ResponseEntity<ApiResponse<CartDto.Response>> getCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(getUserId(userDetails))));
    }

    @PostMapping("/items")
    @Operation(summary = "Thêm sản phẩm vào giỏ")
    public ResponseEntity<ApiResponse<CartDto.Response>> addItem(
            @Valid @RequestBody CartDto.AddItemRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok("Đã thêm vào giỏ hàng",
                cartService.addItem(getUserId(userDetails), req)));
    }

    @PutMapping("/items")
    @Operation(summary = "Cập nhật số lượng sản phẩm (quantity=0 để xoá)")
    public ResponseEntity<ApiResponse<CartDto.Response>> updateItem(
            @Valid @RequestBody CartDto.UpdateItemRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật giỏ hàng",
                cartService.updateItem(getUserId(userDetails), req)));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Xoá sản phẩm khỏi giỏ")
    public ResponseEntity<ApiResponse<CartDto.Response>> removeItem(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok("Đã xoá khỏi giỏ hàng",
                cartService.removeItem(getUserId(userDetails), productId)));
    }

    @DeleteMapping
    @Operation(summary = "Xoá toàn bộ giỏ hàng")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        cartService.clearCart(getUserId(userDetails));
        return ResponseEntity.ok(ApiResponse.ok("Đã xoá giỏ hàng", null));
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
    }
}
