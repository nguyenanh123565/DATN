package com.smarthome.controller;

import com.smarthome.common.ApiResponse;
import com.smarthome.dto.OrderRequestDto;
import com.smarthome.dto.OrderResponseDto;
import com.smarthome.repository.UserRepository;
import com.smarthome.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "API quản lý đơn hàng")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @GetMapping("/my")
    @Operation(summary = "Lấy đơn hàng của tôi")
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.ok(orderService.getMyOrders(userId, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết đơn hàng")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.ok(orderService.getById(id, userId)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy toàn bộ đơn hàng (Admin)")
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders(status, page, size)));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy thống kê đơn hàng theo trạng thái [ADMIN]")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> getStatistics() {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderStatistics()));
    }

    @PostMapping
    @Operation(summary = "Tạo đơn hàng mới")
    public ResponseEntity<ApiResponse<OrderResponseDto>> create(
            @Valid @RequestBody OrderRequestDto req,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đặt hàng thành công!", orderService.createOrder(req, userId)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật trạng thái đơn hàng [ADMIN]")
    public ResponseEntity<ApiResponse<OrderResponseDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) java.math.BigDecimal actualShippingCost
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công",
                orderService.updateStatus(id, status, actualShippingCost)));
    }

    @PostMapping("/{id}/confirm-received")
    @Operation(summary = "Xác nhận đã nhận hàng (User)")
    public ResponseEntity<ApiResponse<OrderResponseDto>> confirmReceived(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Xác nhận đã nhận hàng thành công!",
                orderService.confirmReceived(id, userId)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy đơn hàng (User)")
    public ResponseEntity<ApiResponse<OrderResponseDto>> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Hủy đơn hàng thành công!",
                orderService.cancelOrder(id, userId)));
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow().getId();
    }
}
