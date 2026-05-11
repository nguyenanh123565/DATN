package com.smarthome.controller;

import com.smarthome.common.ApiResponse;
import com.smarthome.dto.ReviewDto;
import com.smarthome.repository.UserRepository;
import com.smarthome.service.ReviewService;
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
@RequestMapping("/api/v1/reviews")
@Tag(name = "Reviews", description = "API đánh giá sản phẩm")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    public ReviewController(ReviewService reviewService, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Lấy đánh giá theo sản phẩm")
    public ResponseEntity<ApiResponse<Page<ReviewDto.Response>>> getByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getByProduct(productId, page, size)));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Gửi đánh giá sản phẩm")
    public ResponseEntity<ApiResponse<ReviewDto.Response>> create(
            @Valid @RequestBody ReviewDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đánh giá thành công!", reviewService.create(req, userId)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Duyệt đánh giá [ADMIN]")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id) {
        reviewService.approve(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã duyệt đánh giá", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Xoá đánh giá [ADMIN]")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xoá đánh giá", null));
    }
}
