package com.smarthome.controller;

import com.smarthome.common.ApiResponse;
import com.smarthome.dto.ProductDto;
import com.smarthome.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "API quản lý sản phẩm")
public class ProductApiController {

    private final ProductService productService;

    public ProductApiController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách sản phẩm có phân trang")
    public ResponseEntity<ApiResponse<Page<ProductDto.Response>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String keyword
    ) {
        Page<ProductDto.Response> data = keyword != null && !keyword.isBlank()
                ? productService.search(keyword, page, size)
                : productService.getAll(page, size, sort);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết sản phẩm theo ID")
    public ResponseEntity<ApiResponse<ProductDto.Response>> getById(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getById(id)));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Lấy sản phẩm theo slug")
    public ResponseEntity<ApiResponse<ProductDto.Response>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getBySlug(slug)));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Lấy sản phẩm theo danh mục")
    public ResponseEntity<ApiResponse<Page<ProductDto.Response>>> getByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getByCategory(categoryId, page, size)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lấy danh sách TẤT CẢ sản phẩm (kể cả ẩn) [ADMIN]")
    public ResponseEntity<ApiResponse<Page<ProductDto.Response>>> getAllForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String keyword
    ) {
        Page<ProductDto.Response> data = keyword != null && !keyword.isBlank()
                ? productService.searchAll(keyword, page, size)
                : productService.getAllAdmin(page, size, sort);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Tạo sản phẩm mới [ADMIN]")
    public ResponseEntity<ApiResponse<ProductDto.Response>> create(
            @Valid @RequestBody ProductDto.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo sản phẩm thành công", productService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cập nhật sản phẩm [ADMIN]")
    public ResponseEntity<ApiResponse<ProductDto.Response>> update(
            @PathVariable Long id,
            @RequestBody ProductDto.UpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", productService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Xoá mềm sản phẩm [ADMIN]")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xoá sản phẩm", null));
    }

    @PostMapping("/sync-flash-sale-time")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Đồng bộ thời gian kết thúc cho tất cả sản phẩm Flash Sale [ADMIN]")
    public ResponseEntity<ApiResponse<Void>> syncFlashSaleTime(
            @RequestParam LocalDateTime endTime) {
        productService.syncFlashSaleTime(endTime);
        return ResponseEntity.ok(ApiResponse.ok("Đã đồng bộ thời gian Flash Sale", null));
    }

    @GetMapping("/flash-sale")
    @Operation(summary = "Lấy danh sách sản phẩm đang Flash Sale")
    public ResponseEntity<ApiResponse<List<ProductDto.Response>>> getFlashSaleProducts() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getFlashSaleProducts()));
    }
}
