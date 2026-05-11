package com.smarthome.controller;

import com.smarthome.common.ApiResponse;
import com.smarthome.dto.UserDto;
import com.smarthome.repository.UserRepository;
import com.smarthome.service.UserService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "API quản lý người dùng & hồ sơ")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    // ─── Hồ sơ cá nhân ────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin hồ sơ cá nhân")
    public ResponseEntity<ApiResponse<UserDto.Response>> getMe(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(getUserId(userDetails))));
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật hồ sơ cá nhân")
    public ResponseEntity<ApiResponse<UserDto.Response>> updateMe(
            @Valid @RequestBody UserDto.UpdateProfileRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công",
                userService.updateProfile(getUserId(userDetails), req)));
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Đổi mật khẩu")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody UserDto.ChangePasswordRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.changePassword(getUserId(userDetails), req);
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công", null));
    }

    // ─── Địa chỉ giao hàng ────────────────────────────────────────────

    @GetMapping("/me/addresses")
    @Operation(summary = "Lấy danh sách địa chỉ giao hàng")
    public ResponseEntity<ApiResponse<List<UserDto.AddressResponse>>> getAddresses(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAddresses(getUserId(userDetails))));
    }

    @PostMapping("/me/addresses")
    @Operation(summary = "Thêm địa chỉ giao hàng mới")
    public ResponseEntity<ApiResponse<UserDto.AddressResponse>> addAddress(
            @Valid @RequestBody UserDto.AddressRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Thêm địa chỉ thành công",
                        userService.addAddress(getUserId(userDetails), req)));
    }

    @DeleteMapping("/me/addresses/{addressId}")
    @Operation(summary = "Xoá địa chỉ giao hàng")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable Long addressId,
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteAddress(getUserId(userDetails), addressId);
        return ResponseEntity.ok(ApiResponse.ok("Đã xoá địa chỉ", null));
    }

    // ─── Admin: Quản lý người dùng ────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy danh sách tất cả người dùng [ADMIN]")
    public ResponseEntity<ApiResponse<Page<UserDto.Response>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String rank,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers(page, size, role, rank, sortDir)));
    }

    @PutMapping("/{id}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ban người dùng [ADMIN]")
    public ResponseEntity<ApiResponse<Void>> ban(@PathVariable Long id) {
        userService.banUser(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã ban người dùng", null));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kích hoạt lại người dùng [ADMIN]")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã kích hoạt người dùng", null));
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
    }
}
