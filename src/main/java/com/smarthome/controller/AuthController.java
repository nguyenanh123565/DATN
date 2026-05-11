package com.smarthome.controller;

import com.smarthome.entity.User;
import com.smarthome.repository.UserRepository;
import com.smarthome.security.JwtUtil;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authManager, UserDetailsService userDetailsService, JwtUtil jwtUtil, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authManager = authManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email    = request.getOrDefault("email", "");
        String password = request.getOrDefault("password", "");

        authManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList());

        String token = jwtUtil.generateToken(claims, userDetails);

        // Lấy thêm fullName và role từ DB
        User userEntity = userRepository.findByEmail(email).orElseThrow();

        return ResponseEntity.ok(Map.of(
                "token", token,
                "email", email,
                "fullName", userEntity.getFullName(),
                "role", userEntity.getRole().name(),
                "message", "Đăng nhập thành công"
        ));
    }

    // POST /api/auth/register — đăng ký user thường (USER)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String email    = request.getOrDefault("email", "");
        String password = request.getOrDefault("password", "");
        String fullName = request.getOrDefault("fullName", "");

        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email không được để trống!"));
        }
        if (!email.toLowerCase().endsWith("@gmail.com")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chỉ chấp nhận tài khoản đăng ký bằng @gmail.com!"));
        }
        if (password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu không được để trống!"));
        }
        if (fullName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Họ tên không được để trống!"));
        }

        // Kiểm tra độ phức tạp mật khẩu: ít nhất 6 ký tự, 1 chữ, 1 số
        if (password.length() < 6 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu phải có ít nhất 6 ký tự, bao gồm cả chữ và số!"));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email đã tồn tại!"));
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(User.Role.USER);
        user.setStatus(User.Status.ACTIVE);

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Đăng ký thành công!"));
    }

    // POST /api/auth/admin/create-user — Admin tạo user với role tùy chọn
    @PostMapping("/admin/create-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminCreateUser(@RequestBody Map<String, String> request) {
        String email    = request.getOrDefault("email", "");
        String password = request.getOrDefault("password", "");
        String fullName = request.getOrDefault("fullName", "");
        String roleStr  = request.getOrDefault("role", "USER");

        if (email.isBlank() || password.isBlank() || fullName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng điền đầy đủ thông tin!"));
        }
        if (!email.toLowerCase().endsWith("@gmail.com")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email phải có đuôi @gmail.com!"));
        }

        // Kiểm tra độ phức tạp mật khẩu: ít nhất 6 ký tự, 1 chữ, 1 số
        if (password.length() < 6 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu phải có ít nhất 6 ký tự, bao gồm cả chữ và số!"));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email đã tồn tại!"));
        }

        User.Role role;
        try {
            role = User.Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            role = User.Role.USER;
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        user.setStatus(User.Status.ACTIVE);

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Tạo tài khoản thành công! Email: " + email));
    }
}
