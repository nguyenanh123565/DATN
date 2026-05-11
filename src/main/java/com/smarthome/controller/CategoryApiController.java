package com.smarthome.controller;

import com.smarthome.common.ApiResponse;
import com.smarthome.dto.CategoryDto;
import com.smarthome.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "API quản lý danh mục sản phẩm")
public class CategoryApiController {

    private final CategoryService categoryService;

    public CategoryApiController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách danh mục (Tất cả)")
    public ResponseEntity<ApiResponse<List<CategoryDto.Response>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getAllCategories()));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Lấy danh mục con")
    public ResponseEntity<ApiResponse<List<CategoryDto.Response>>> getChildren(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getChildren(id)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết danh mục theo ID")
    public ResponseEntity<ApiResponse<CategoryDto.Response>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getById(id)));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Chi tiết danh mục theo slug")
    public ResponseEntity<ApiResponse<CategoryDto.Response>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getBySlug(slug)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Tạo danh mục mới [ADMIN]")
    public ResponseEntity<ApiResponse<CategoryDto.Response>> create(
            @Valid @RequestBody CategoryDto.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo danh mục thành công", categoryService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cập nhật danh mục [ADMIN]")
    public ResponseEntity<ApiResponse<CategoryDto.Response>> update(
            @PathVariable Long id, @RequestBody CategoryDto.CreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", categoryService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Xoá danh mục [ADMIN]")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xoá danh mục", null));
    }
}
