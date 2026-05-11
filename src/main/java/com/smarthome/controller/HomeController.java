package com.smarthome.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Trang chủ → chuyển hướng sang /products
    @GetMapping("/")
    public String home() {
        return "redirect:/products";
    }

    // Trang danh sách sản phẩm (Thymeleaf)
    @GetMapping("/products")
    public String products() {
        return "index"; // sẽ tạo file resources/templates/index.html
    }

    // Test trang admin
    @GetMapping("/admin")
    public String admin() {
        return "admin/dashboard";
    }

    // Chuyển hướng các trang xác thực
    @GetMapping("/login")
    public String login() {
        return "redirect:/auth.html";
    }

    @GetMapping("/register")
    public String register() {
        return "redirect:/auth.html";
    }
}
