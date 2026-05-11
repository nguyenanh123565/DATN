package com.smarthome.controller;

import com.smarthome.common.ApiResponse;
import com.smarthome.dto.VoucherDto;
import com.smarthome.dto.VoucherRequestDto;
import com.smarthome.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vouchers")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Vouchers", description = "API quản lý mã giảm giá (Voucher)")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    // ---- ADMIN endpoints ----

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy danh sách voucher (Admin)")
    public ResponseEntity<ApiResponse<Page<VoucherDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(voucherService.getAll(page, size)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo voucher mới (Admin)")
    public ResponseEntity<ApiResponse<VoucherDto>> create(@Valid @RequestBody VoucherRequestDto req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo voucher thành công!", voucherService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật voucher (Admin)")
    public ResponseEntity<ApiResponse<VoucherDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody VoucherRequestDto req
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật voucher thành công!", voucherService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa voucher (Admin)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        voucherService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa voucher thành công!", null));
    }

    // ---- USER endpoint: validate/apply voucher ----

    @GetMapping("/public")
    @Operation(summary = "Lấy danh sách voucher đang hoạt động (Public - dùng cho trang chủ)")
    public ResponseEntity<ApiResponse<Page<VoucherDto>>> getAllActive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(voucherService.getAllActive(page, size)));
    }

    @GetMapping("/validate")
    @Operation(summary = "Kiểm tra mã voucher (User - dùng ở trang checkout)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validate(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal
    ) {
        VoucherService.VoucherApplyResult result = voucherService.applyVoucher(code, orderTotal);
        return ResponseEntity.ok(ApiResponse.ok("Áp dụng voucher thành công!", Map.of(
                "voucherId", result.voucherId(),
                "code", result.code(),
                "discountAmount", result.discountAmount()
        )));
    }
}
