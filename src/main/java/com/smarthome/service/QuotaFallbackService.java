package com.smarthome.service;

import com.smarthome.dto.OrderResponseDto;
import com.smarthome.entity.Product;
import com.smarthome.repository.ProductRepository;
import com.smarthome.service.IntentParserService.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Cung cấp câu trả lời thông minh từ dữ liệu DB khi Gemini API không khả dụng.
 * TUYỆT ĐỐI không hiển thị lỗi kỹ thuật (429, quota...) cho người dùng.
 */
@Service
public class QuotaFallbackService {

    private static final Logger log = LoggerFactory.getLogger(QuotaFallbackService.class);

    private final ProductRepository productRepository;
    private final OrderService orderService;

    private static final NumberFormat VND_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    public QuotaFallbackService(ProductRepository productRepository, OrderService orderService) {
        this.productRepository = productRepository;
        this.orderService = orderService;
    }

    /**
     * Tạo câu trả lời thông minh từ DB.
     */
    public String generateSmartFallback(Long userId, String userMessage, Intent intent, List<Product> dbProducts) {
        log.info("🔧 Chế độ Offline Fallback được kích hoạt cho: '{}'", userMessage);

        // 1. Nếu là tra cứu đơn hàng
        if (isOrderInquiry(userMessage) && userId != null) {
            return buildOrderStatusResponse(userId);
        }

        // 2. Lấy danh sách sản phẩm
        List<Product> products;
        if (dbProducts != null && !dbProducts.isEmpty()) {
            products = dbProducts.stream().limit(5).collect(Collectors.toList());
        } else {
            // Lấy 5 sản phẩm đang bán nếu không có danh sách
            products = productRepository.findByIsActiveTrue().stream().limit(5).collect(Collectors.toList());
        }

        if (products.isEmpty()) {
            return "Chào bạn! Tôi là trợ lý SmartHome. Hiện tại tôi đang gặp một chút gián đoạn kết nối với hệ thống AI.\n\n" +
                   "Tuy nhiên, bạn có thể gọi hotline **0823422987** để được các chuyên viên tư vấn trực tiếp và nhận ưu đãi mới nhất nhé! 🙏";
        }

        return buildProductListResponse(products);
    }

    private boolean isOrderInquiry(String msg) {
        String lower = msg.toLowerCase();
        return lower.contains("đơn hàng") || lower.contains("đã mua") || lower.contains("kiểm tra đơn");
    }

    private String buildOrderStatusResponse(Long userId) {
        try {
            Page<OrderResponseDto> orders = orderService.getMyOrders(userId, 0, 1);
            if (orders.isEmpty()) {
                return "Chào bạn! Tôi chưa thấy đơn hàng nào của bạn trên hệ thống. Bạn hãy chọn sản phẩm ưng ý và đặt hàng để trải nghiệm dịch vụ của SmartHome nhé! 🛒";
            }

            OrderResponseDto lastOrder = orders.getContent().get(0);
            return String.format(
                "Chào bạn! Tôi đã kiểm tra hệ thống. Đơn hàng mới nhất của bạn là **#%d**.\n\n" +
                "📍 **Trạng thái:** %s\n" +
                "💰 **Tổng thanh toán:** %sđ\n\n" +
                "Bạn có thể xem chi tiết trong mục 'Đơn hàng của tôi'. Nếu cần hỗ trợ gấp, vui lòng gọi **0823422987** nhé!",
                lastOrder.getId(),
                translateStatus(lastOrder.getStatus()),
                VND_FORMAT.format(lastOrder.getFinalAmount())
            );
        } catch (Exception e) {
            return "Chào bạn! Tôi chưa thể tra cứu đơn hàng ngay lúc này. Bạn vui lòng kiểm tra trong mục cá nhân hoặc thử lại sau ít phút nhé! 🙏";
        }
    }

    private String buildProductListResponse(List<Product> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("Chào bạn! Hệ thống AI của SmartHome đang bận xử lý một chút.\n\n");
        sb.append("Tuy nhiên, dựa trên yêu cầu của bạn, tôi tìm thấy một số sản phẩm nổi bật bạn có thể quan tâm:\n\n");

        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            sb.append(i + 1).append(". **").append(p.getName()).append("** — ");
            sb.append(VND_FORMAT.format(p.getSalePrice() != null ? p.getSalePrice() : p.getPrice())).append("đ\n");
        }

        sb.append("\n---\n💡 Bạn có thể chat lại sau vài phút hoặc gọi hotline **0823422987** để được hỗ trợ ngay!");
        return sb.toString();
    }

    private String translateStatus(String status) {
        return switch (status.toUpperCase()) {
            case "PENDING" -> "Chờ xác nhận";
            case "PROCESSING" -> "Đang xử lý";
            case "SHIPPING" -> "Đang giao hàng";
            case "DELIVERED" -> "Đã giao thành công";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }
}
