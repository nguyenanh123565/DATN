package com.smarthome.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthome.common.BusinessException;
import com.smarthome.common.ResourceNotFoundException;
import com.smarthome.dto.CartItemDto;
import com.smarthome.dto.OrderRequestDto;
import com.smarthome.dto.OrderResponseDto;
import com.smarthome.entity.*;
import com.smarthome.repository.CartRepository;
import com.smarthome.repository.OrderRepository;
import com.smarthome.repository.ProductRepository;
import com.smarthome.repository.UserRepository;
import com.smarthome.repository.VoucherRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;
    private final CartService cartService;
    private final CartRepository cartRepo;
    private final VoucherService voucherService;
    private final VoucherRepository voucherRepo;
    private final ShippingService shippingService;
    private final LoyaltyRankService loyaltyRankService;

    // Explicit constructor to bypass Lombok
    public OrderService(OrderRepository orderRepo, ProductRepository productRepo, UserRepository userRepo, ObjectMapper objectMapper, CartService cartService, CartRepository cartRepo, VoucherService voucherService, VoucherRepository voucherRepo, ShippingService shippingService, LoyaltyRankService loyaltyRankService) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.objectMapper = objectMapper;
        this.cartService = cartService;
        this.cartRepo = cartRepo;
        this.voucherService = voucherService;
        this.voucherRepo = voucherRepo;
        this.shippingService = shippingService;
        this.loyaltyRankService = loyaltyRankService;
    }

    public Page<OrderResponseDto> getMyOrders(Long userId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepo.findByUserId(userId, pageable).map(this::toResponse);
    }

    public Page<OrderResponseDto> getAllOrders(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            return orderRepo.findByStatus(orderStatus, pageable).map(this::toResponse);
        }
        return orderRepo.findAll(pageable).map(this::toResponse);
    }

    public java.util.Map<String, Long> getOrderStatistics() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("PENDING", orderRepo.countByStatus(Order.OrderStatus.PENDING));
        stats.put("PROCESSING", orderRepo.countByStatus(Order.OrderStatus.PROCESSING));
        stats.put("SHIPPING", orderRepo.countByStatus(Order.OrderStatus.SHIPPING));
        stats.put("DELIVERED", orderRepo.countByStatus(Order.OrderStatus.DELIVERED));
        stats.put("CANCELLED", orderRepo.countByStatus(Order.OrderStatus.CANCELLED));
        return stats;
    }

    public OrderResponseDto getById(Long orderId, Long userId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("Bạn không có quyền xem đơn hàng này!");
        }
        return toResponse(order);
    }

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto req, Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // 1. Lấy Cart
        Cart cart = cartRepo.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Giỏ hàng của bạn đang trống!"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessException("Giỏ hàng của bạn đang trống!");
        }

        // 2. Tính subtotal
        BigDecimal subtotal = cartService.calculateSubtotal(userId);

        // 3. Xử lý Voucher (nếu có)
        Voucher appliedVoucher = null;
        BigDecimal discountTotal = BigDecimal.ZERO;

        if (req.getDiscountCode() != null && !req.getDiscountCode().isBlank()) {
            VoucherService.VoucherApplyResult result = voucherService.applyVoucher(req.getDiscountCode(), subtotal);
            appliedVoucher = voucherRepo.findById(result.voucherId()).orElse(null);
            discountTotal = result.discountAmount();
        }

        // 4. Tính Loyalty Rank discount (áp dụng SAU voucher, có cap 13%)
        User.LoyaltyRank rank = user.getLoyaltyRank();
        BigDecimal loyaltyDiscountAmount = loyaltyRankService.calcLoyaltyDiscount(rank, subtotal, discountTotal);

        // 5. Tính phí vận chuyển và finalAmount
        BigDecimal shippingFee = shippingService.calculateShippingFee(req.getAddress(), subtotal);
        BigDecimal finalAmount = subtotal.add(shippingFee).subtract(discountTotal).subtract(loyaltyDiscountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

        Order order = new Order();
        order.setUser(user);
        order.setCustomerName(req.getCustomerName());
        order.setPhone(req.getPhone());
        order.setAddress(req.getAddress());
        order.setTotalAmount(subtotal);
        order.setDiscountAmount(discountTotal.add(loyaltyDiscountAmount));  // tổng giảm = voucher + rank
        order.setShippingFee(shippingFee);
        order.setLoyaltyDiscountAmount(loyaltyDiscountAmount);
        order.setLoyaltyRankApplied(rank.name());
        order.setVoucher(appliedVoucher);
        order.setFinalAmount(finalAmount);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setNotes(req.getNotes());

        order = orderRepo.save(order);

        // 4. Lưu OrderItem từ CartItem
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            // Load lại Product trực tiếp từ DB (tránh dùng Lazy Proxy từ CartItem)
            // để đảm bảo đối tượng được Hibernate quản lý và ghi xuống DB đúng cách
            Product p = productRepo.findById(ci.getProduct().getId())
                    .orElseThrow(() -> new BusinessException("Sản phẩm không còn tồn tại!"));

            if (p.getStockQuantity() < ci.getQuantity()) {
                throw new BusinessException("Sản phẩm '" + p.getName() + "' không đủ hàng! Cần "
                        + ci.getQuantity() + " nhưng chỉ còn " + p.getStockQuantity());
            }

            // Trừ tồn kho — lệnh này sẽ được Hibernate flush xuống DB đúng cách
            p.setStockQuantity(p.getStockQuantity() - ci.getQuantity());
            productRepo.save(p);

            String img = null;
            try {
                if (p.getImageUrls() != null) {
                    String[] imgs = objectMapper.readValue(p.getImageUrls(), String[].class);
                    if (imgs.length > 0) img = imgs[0];
                }
            } catch (Exception ignored) {}

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(p);
            orderItem.setProductName(p.getName());
            orderItem.setProductImage(img);
            orderItem.setQuantity(ci.getQuantity());
            orderItem.setUnitPrice(ci.getUnitPrice());
            orderItem.setSubtotal(ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
            orderItems.add(orderItem);
        }

        order.setItems(orderItems);
        order = orderRepo.save(order);

        // 6. Cập nhật lượt dùng voucher
        if (appliedVoucher != null) {
            voucherService.incrementUsage(appliedVoucher.getId());
        }

        // 7. Xoá giỏ hàng
        cartService.clearCart(userId);

        return toResponse(order);
    }

    @Transactional
    public OrderResponseDto updateStatus(Long orderId, String status, java.math.BigDecimal actualShippingCost) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", orderId));
        
        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status);
        
        // Ngăn chặn Admin tự cập nhật sang trạng thái Hoàn thành (DELIVERED)
        if (newStatus == Order.OrderStatus.DELIVERED) {
            throw new BusinessException("Trạng thái Hoàn thành (DELIVERED) chỉ có thể được xác nhận bởi khách hàng!");
        }
        
        order.setStatus(newStatus);
        if (actualShippingCost != null) {
            order.setActualShippingCost(actualShippingCost);
        }
        
        return toResponse(orderRepo.save(order));
    }

    @Transactional
    public OrderResponseDto confirmReceived(Long orderId, Long userId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("Bạn không có quyền xác nhận đơn hàng này!");
        }

        if (order.getStatus() != Order.OrderStatus.SHIPPING) {
            throw new BusinessException("Chủ có thể xác nhận khi đơn hàng đang ở trạng thái Đang giao (SHIPPING)!");
        }

        order.setStatus(Order.OrderStatus.DELIVERED);
        Order saved = orderRepo.save(order);

        // Tự động cập nhật rank sau khi xác nhận nhận hàng
        loyaltyRankService.updateAfterDelivered(userId, saved.getFinalAmount());

        return toResponse(saved);
    }

    @Transactional
    public OrderResponseDto cancelOrder(Long orderId, Long userId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("Bạn không có quyền hủy đơn hàng này!");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BusinessException("Chỉ có thể hủy đơn hàng khi đang ở trạng thái Chờ xác nhận (PENDING)!");
        }

        // 1. Cập nhật trạng thái
        order.setStatus(Order.OrderStatus.CANCELLED);

        // 2. Hoàn lại tồn kho
        if (order.getItems() != null) {
            for (OrderItem oi : order.getItems()) {
                Product p = productRepo.findById(oi.getProduct().getId())
                        .orElseThrow(() -> new BusinessException("Sản phẩm không còn tồn tại!"));
                p.setStockQuantity(p.getStockQuantity() + oi.getQuantity());
                productRepo.save(p);
            }
        }

        return toResponse(orderRepo.save(order));
    }

    private OrderResponseDto toResponse(Order o) {
        OrderResponseDto res = new OrderResponseDto();
        res.setId(o.getId());
        if (o.getUser() != null) {
            res.setUserId(o.getUser().getId());
        }
        res.setTotalAmount(o.getTotalAmount());
        res.setFinalAmount(o.getFinalAmount());
        res.setStatus(o.getStatus().name());
        res.setCustomerName(o.getCustomerName());
        res.setPhone(o.getPhone());
        res.setAddress(o.getAddress());
        res.setNotes(o.getNotes());
        res.setDiscountAmount(o.getDiscountAmount());
        res.setLoyaltyDiscountAmount(o.getLoyaltyDiscountAmount());
        res.setLoyaltyRankApplied(o.getLoyaltyRankApplied());
        res.setShippingFee(o.getShippingFee());
        res.setCreatedAt(o.getCreatedAt());

        if (o.getItems() != null) {
            res.setItems(o.getItems().stream().map(i -> {
                CartItemDto ir = new CartItemDto();
                ir.setProductId(i.getProduct().getId());
                ir.setProductName(i.getProductName());
                ir.setProductImage(i.getProductImage());
                ir.setQuantity(i.getQuantity());
                ir.setUnitPrice(i.getUnitPrice());
                ir.setSubtotal(i.getSubtotal());
                return ir;
            }).toList());
        }
        return res;
    }
}
