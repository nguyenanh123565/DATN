package com.smarthome.controller;

import com.smarthome.common.ApiResponse;
import com.smarthome.entity.Order;
import com.smarthome.repository.OrderItemRepository;
import com.smarthome.repository.OrderRepository;
import com.smarthome.repository.ProductRepository;
import com.smarthome.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard", description = "API thống kê Admin Dashboard")
public class DashboardController {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    public DashboardController(OrderRepository orderRepo, OrderItemRepository orderItemRepo, ProductRepository productRepo, UserRepository userRepo) {
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/stats")
    @Operation(summary = "Lấy thống kê tổng quan cho Admin Dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // ---- STAT CARDS ----
        // Tổng đơn hàng
        long totalOrders = orderRepo.count();
        stats.put("totalOrders", totalOrders);

        // Tổng doanh thu (chỉ tính đơn DELIVERED)
        BigDecimal totalRevenue = orderRepo.sumTotalRevenue();
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

        // Tổng sản phẩm
        long totalProducts = productRepo.count();
        stats.put("totalProducts", totalProducts);

        // Tổng người dùng (trừ admin)
        long totalUsers = userRepo.count();
        stats.put("totalUsers", totalUsers);

        // Tổng lợi nhuận thực nhận (Công thức: Doanh thu - Giá vốn - Phí ship thực trả)
        BigDecimal totalRev = (BigDecimal) stats.get("totalRevenue");
        BigDecimal totalActualShipping = orderRepo.sumTotalActualShippingCost();
        BigDecimal totalCostOfGoods = orderRepo.sumTotalCostOfGoods();
        
        BigDecimal totalProfit = (totalRev != null ? totalRev : BigDecimal.ZERO)
            .subtract(totalActualShipping != null ? totalActualShipping : BigDecimal.ZERO)
            .subtract(totalCostOfGoods != null ? totalCostOfGoods : BigDecimal.ZERO);
            
        stats.put("totalProfit", totalProfit);

        // Đơn chờ xác nhận
        long pendingOrders = orderRepo.countByStatus(Order.OrderStatus.PENDING);
        stats.put("pendingOrders", pendingOrders);

        // Đơn đang giao
        long shippingOrders = orderRepo.countByStatus(Order.OrderStatus.SHIPPING);
        stats.put("shippingOrders", shippingOrders);

        // ---- REVENUE CHART (12 tháng của năm hiện tại) ----
        int currentYear = Year.now().getValue();
        List<Object[]> monthlyRaw = orderRepo.getMonthlyRevenue(currentYear);

        // Khởi tạo mảng 12 tháng với giá trị 0
        BigDecimal[] monthlyRevenue = new BigDecimal[12];
        for (int i = 0; i < 12; i++) {
            monthlyRevenue[i] = BigDecimal.ZERO;
        }
        // Điền dữ liệu thực
        for (Object[] row : monthlyRaw) {
            int month = ((Number) row[0]).intValue(); // 1-indexed
            BigDecimal revenue = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            monthlyRevenue[month - 1] = revenue;
        }
        stats.put("monthlyRevenue", monthlyRevenue);

        // ---- PROFIT CHART ----
        List<Object[]> monthlyRevShip = orderRepo.getMonthlyRevenueAndShipping(currentYear);
        List<Object[]> monthlyCosts = orderRepo.getMonthlyCostOfGoodsByYear(currentYear);
        
        BigDecimal[] monthlyProfit = new BigDecimal[12];
        for (int i = 0; i < 12; i++) monthlyProfit[i] = BigDecimal.ZERO;
        
        // Tạo map để tra cứu giá vốn nhanh
        Map<Integer, BigDecimal> costMap = new HashMap<>();
        for (Object[] row : monthlyCosts) {
            costMap.put(((Number) row[0]).intValue(), (BigDecimal) row[1]);
        }

        for (Object[] row : monthlyRevShip) {
            int month = ((Number) row[0]).intValue();
            BigDecimal revenue = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            BigDecimal shipping = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            BigDecimal cost = costMap.getOrDefault(month, BigDecimal.ZERO);
            
            monthlyProfit[month - 1] = revenue.subtract(shipping).subtract(cost);
        }
        stats.put("monthlyProfit", monthlyProfit);

        stats.put("revenueYear", currentYear);

        // ---- TOP SELLING PRODUCTS ----
        List<Object[]> topRaw = orderItemRepo.findTopSellingProducts();
        List<Map<String, Object>> topProducts = new ArrayList<>();
        int limit = Math.min(topRaw.size(), 5);
        for (int i = 0; i < limit; i++) {
            Object[] row = topRaw.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("productName", row[0]);
            item.put("totalSold", row[1]);
            item.put("unitPrice", row[2]);
            item.put("totalProfit", row[3]); // New profit field
            topProducts.add(item);
        }
        stats.put("topProducts", topProducts);

        return ResponseEntity.ok(ApiResponse.ok("Lấy thống kê thành công", stats));
    }
}
