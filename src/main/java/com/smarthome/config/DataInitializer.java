package com.smarthome.config;

import com.smarthome.entity.Category;
import com.smarthome.entity.Product;
import com.smarthome.entity.User;
import com.smarthome.repository.CategoryRepository;
import com.smarthome.repository.ProductRepository;
import com.smarthome.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tự động chèn dữ liệu mẫu khi app khởi động lần đầu (DB trống).
 */
@Component
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void init() {
        // ====== TẠO ADMIN MẶC ĐỊNH ======
        if (!userRepository.existsByEmail("admin@smarthome.vn")) {
            User admin = new User();
            admin.setEmail("admin@smarthome.vn");
            admin.setPassword(passwordEncoder.encode("Admin@1234"));
            admin.setFullName("Quản trị viên");
            admin.setRole(User.Role.ADMIN);
            admin.setStatus(User.Status.ACTIVE);
            userRepository.save(admin);
            log.info("✅ Đã tạo tài khoản admin: admin@smarthome.vn / Admin@1234");
        }

        if (categoryRepository.count() > 0 && productRepository.count() > 0) {
            log.info("✅ Dữ liệu mẫu đã tồn tại, bỏ qua.");
            return;
        }

        if (productRepository.count() == 0 && categoryRepository.count() > 0) {
            log.info("⚠️ Có danh mục nhưng mất sản phẩm -> reset sạch để nạp lại.");
            categoryRepository.deleteAll();
        }

        log.info("🔄 Chèn dữ liệu mẫu SmartHome...");

        // ====== DANH MỤC ======
        Category robot = save(cat("Robot hút bụi", "robot-hut-bui", 1));
        Category air   = save(cat("Máy lọc không khí", "may-loc-khong-khi", 2));
        Category cam   = save(cat("Camera an ninh", "camera-an-ninh", 3));
        Category light = save(cat("Đèn thông minh", "den-thong-minh", 4));
        Category plug  = save(cat("Ổ cắm thông minh", "o-cam-thong-minh", 5));
        Category lock  = save(cat("Khóa cửa thông minh", "khoa-cua-thong-minh", 6));
        Category spk   = save(cat("Loa thông minh", "loa-thong-minh", 7));

        // ====== SẢN PHẨM ======
        Product p1 = new Product();
        p1.setName("Ecovacs Deebot T30 Pro");
        p1.setSlug("ecovacs-deebot-t30-pro");
        p1.setSku("ECO-T30");
        p1.setDescription("Robot hút bụi lau nhà cao cấp, lực hút 11000Pa, tự giặt giẻ lau nước nóng, sấy khô, tự đổ rác, điều khiển app ECOVACS HOME.");
        p1.setPrice(new BigDecimal("18990000"));
        p1.setSalePrice(new BigDecimal("16990000"));
        p1.setCostPrice(new BigDecimal("13500000"));
        p1.setStockQuantity(25);
        p1.setCategory(robot);
        p1.setBrand("Ecovacs");
        p1.setSpecs("{\"Lực hút\":\"11000Pa\",\"Pin\":\"5200mAh\",\"Diện tích\":\"300m²\"}");
        p1.setIsActive(true);
        p1.setIsFeatured(true);
        p1.setViewCount(150);

        Product p2 = new Product();
        p2.setName("Roborock S8 MaxV Ultra");
        p2.setSlug("roborock-s8-maxv-ultra");
        p2.setSku("RBR-S8MU");
        p2.setDescription("Robot hút bụi Roborock S8 MaxV Ultra, camera AI nhận diện vật cản, lực hút 10000Pa, tự giặt giẻ, sấy nóng, tự đổ rác, tự bổ sung nước.");
        p2.setPrice(new BigDecimal("22990000"));
        p2.setSalePrice(new BigDecimal("19990000"));
        p2.setCostPrice(new BigDecimal("15000000"));
        p2.setStockQuantity(15);
        p2.setCategory(robot);
        p2.setBrand("Roborock");
        p2.setSpecs("{\"Lực hút\":\"10000Pa\",\"Camera\":\"AI StarSight 2.0\"}");
        p2.setIsActive(true);
        p2.setIsFeatured(true);
        p2.setViewCount(200);

        Product p3 = new Product();
        p3.setName("Xiaomi Robot Vacuum X20 Pro");
        p3.setSlug("xiaomi-robot-x20-pro");
        p3.setSku("XM-X20P");
        p3.setDescription("Robot hút bụi Xiaomi X20 Pro giá tốt, lực hút 8000Pa, tự giặt giẻ, tự đổ rác. Hỗ trợ Google Assistant.");
        p3.setPrice(new BigDecimal("9990000"));
        p3.setSalePrice(new BigDecimal("8490000"));
        p3.setCostPrice(new BigDecimal("6500000"));
        p3.setStockQuantity(50);
        p3.setCategory(robot);
        p3.setBrand("Xiaomi");
        p3.setSpecs("{\"Lực hút\":\"8000Pa\",\"Pin\":\"4000mAh\",\"Diện tích\":\"250m²\"}");
        p3.setIsActive(true);
        p3.setIsFeatured(false);
        p3.setViewCount(320);

        Product p4 = new Product();
        p4.setName("Xiaomi Smart Air Purifier 4 Pro");
        p4.setSlug("xiaomi-air-purifier-4-pro");
        p4.setSku("XM-AP4P");
        p4.setDescription("Máy lọc không khí Xiaomi 4 Pro, lọc HEPA H13, diện tích 60m², cảm biến PM2.5, điều khiển app Mi Home và giọng nói.");
        p4.setPrice(new BigDecimal("4990000"));
        p4.setSalePrice(new BigDecimal("3990000"));
        p4.setCostPrice(new BigDecimal("2800000"));
        p4.setStockQuantity(40);
        p4.setCategory(air);
        p4.setBrand("Xiaomi");
        p4.setSpecs("{\"Lọc\":\"HEPA H13\",\"Diện tích\":\"60m²\",\"CADR\":\"500m³/h\"}");
        p4.setIsActive(true);
        p4.setIsFeatured(true);
        p4.setViewCount(180);

        Product p5 = new Product();
        p5.setName("Dyson Purifier Big Quiet");
        p5.setSlug("dyson-purifier-big-quiet");
        p5.setSku("DYS-PBQ");
        p5.setDescription("Máy lọc không khí Dyson cao cấp, HEPA H13 + Carbon, diện tích 100m², siêu yên tĩnh, hiển thị chất lượng không khí realtime.");
        p5.setPrice(new BigDecimal("25990000"));
        p5.setCostPrice(new BigDecimal("18500000"));
        p5.setStockQuantity(10);
        p5.setCategory(air);
        p5.setBrand("Dyson");
        p5.setSpecs("{\"Lọc\":\"HEPA H13 + Carbon\",\"Diện tích\":\"100m²\"}");
        p5.setIsActive(true);
        p5.setIsFeatured(false);
        p5.setViewCount(90);

        Product p6 = new Product();
        p6.setName("Camera Xiaomi C500 Pro 5MP");
        p6.setSlug("camera-xiaomi-c500-pro");
        p6.setSku("XM-C500");
        p6.setDescription("Camera Xiaomi C500 Pro 5MP, quay 360°, nhìn đêm, phát hiện người AI, đàm thoại 2 chiều, lưu cloud.");
        p6.setPrice(new BigDecimal("990000"));
        p6.setSalePrice(new BigDecimal("790000"));
        p6.setCostPrice(new BigDecimal("550000"));
        p6.setStockQuantity(100);
        p6.setCategory(cam);
        p6.setBrand("Xiaomi");
        p6.setSpecs("{\"Phân giải\":\"5MP 2K\",\"Góc quay\":\"360°\",\"Nhìn đêm\":\"10m\"}");
        p6.setIsActive(true);
        p6.setIsFeatured(false);
        p6.setViewCount(500);

        Product p7 = new Product();
        p7.setName("Camera Ezviz C6 2K Pro");
        p7.setSlug("camera-ezviz-c6-2k");
        p7.setSku("EZV-C6");
        p7.setDescription("Camera trong nhà Ezviz C6 2K, AI phát hiện người và thú cưng, theo dõi chuyển động, đàm thoại 2 chiều.");
        p7.setPrice(new BigDecimal("1290000"));
        p7.setSalePrice(new BigDecimal("990000"));
        p7.setCostPrice(new BigDecimal("700000"));
        p7.setStockQuantity(80);
        p7.setCategory(cam);
        p7.setBrand("Ezviz");
        p7.setSpecs("{\"Phân giải\":\"2K 4MP\",\"Góc quay\":\"340°\"}");
        p7.setIsActive(true);
        p7.setIsFeatured(true);
        p7.setViewCount(350);

        Product p8 = new Product();
        p8.setName("Philips Hue White & Color Ambiance");
        p8.setSlug("philips-hue-starter-kit");
        p8.setSku("PHI-HUE");
        p8.setDescription("Bộ đèn thông minh Philips Hue Starter Kit 3 bóng + Bridge, 16 triệu màu, điều khiển giọng nói Alexa/Google, hẹn giờ.");
        p8.setPrice(new BigDecimal("3990000"));
        p8.setSalePrice(new BigDecimal("3490000"));
        p8.setCostPrice(new BigDecimal("2600000"));
        p8.setStockQuantity(30);
        p8.setCategory(light);
        p8.setBrand("Philips");
        p8.setSpecs("{\"Bóng\":\"3 + Bridge\",\"Màu\":\"16 triệu\",\"Kết nối\":\"Zigbee+WiFi\"}");
        p8.setIsActive(true);
        p8.setIsFeatured(true);
        p8.setViewCount(120);

        Product p9 = new Product();
        p9.setName("Yeelight Smart LED Bulb W3");
        p9.setSlug("yeelight-bulb-w3");
        p9.setSku("YEE-W3");
        p9.setDescription("Bóng đèn thông minh Yeelight W3 đổi màu, hỗ trợ Google Home, Alexa. Giá rẻ, chất lượng tốt.");
        p9.setPrice(new BigDecimal("290000"));
        p9.setSalePrice(new BigDecimal("250000"));
        p9.setCostPrice(new BigDecimal("180000"));
        p9.setStockQuantity(200);
        p9.setCategory(light);
        p9.setBrand("Yeelight");
        p9.setSpecs("{\"Công suất\":\"8W\",\"Màu\":\"16 triệu\",\"Kết nối\":\"WiFi\"}");
        p9.setIsActive(true);
        p9.setIsFeatured(false);
        p9.setViewCount(280);

        Product p10 = new Product();
        p10.setName("Samsung SHP-DP609");
        p10.setSlug("samsung-shp-dp609");
        p10.setSku("SS-DP609");
        p10.setDescription("Khóa cửa vân tay Samsung DP609, mở bằng vân tay, mật mã, thẻ từ, chìa khóa, Bluetooth, WiFi.");
        p10.setPrice(new BigDecimal("8990000"));
        p10.setSalePrice(new BigDecimal("7490000"));
        p10.setCostPrice(new BigDecimal("5800000"));
        p10.setStockQuantity(20);
        p10.setCategory(lock);
        p10.setBrand("Samsung");
        p10.setSpecs("{\"Mở khóa\":\"Vân tay + Mật mã + Thẻ + Chìa + BT\",\"Pin\":\"8 pin AA\"}");
        p10.setIsActive(true);
        p10.setIsFeatured(true);
        p10.setViewCount(95);

        Product p11 = new Product();
        p11.setName("Google Nest Hub Max");
        p11.setSlug("google-nest-hub-max");
        p11.setSku("GG-NHM");
        p11.setDescription("Loa thông minh Google Nest Hub Max 10 inch, Google Assistant, camera, video call, điều khiển nhà thông minh.");
        p11.setPrice(new BigDecimal("6990000"));
        p11.setSalePrice(new BigDecimal("5490000"));
        p11.setCostPrice(new BigDecimal("4200000"));
        p11.setStockQuantity(15);
        p11.setCategory(spk);
        p11.setBrand("Google");
        p11.setSpecs("{\"Màn hình\":\"10 inch HD\",\"Loa\":\"Stereo 30W\",\"Camera\":\"6.5MP\"}");
        p11.setIsActive(true);
        p11.setIsFeatured(true);
        p11.setViewCount(75);

        Product p12 = new Product();
        p12.setName("Amazon Echo Dot Gen 5");
        p12.setSlug("amazon-echo-dot-5");
        p12.setSku("AMZ-ED5");
        p12.setDescription("Loa thông minh Amazon Echo Dot Gen 5, Alexa, âm thanh tốt, giá tốt, điều khiển thiết bị nhà thông minh.");
        p12.setPrice(new BigDecimal("1490000"));
        p12.setSalePrice(new BigDecimal("1190000"));
        p12.setCostPrice(new BigDecimal("850000"));
        p12.setStockQuantity(60);
        p12.setCategory(spk);
        p12.setBrand("Amazon");
        p12.setSpecs("{\"Loa\":\"1.73 inch\",\"Trợ lý\":\"Alexa\",\"Kết nối\":\"WiFi+BT 5.0\"}");
        p12.setIsActive(true);
        p12.setIsFeatured(false);
        p12.setViewCount(210);

        productRepository.saveAll(List.of(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12));

        log.info("✅ Đã chèn 7 danh mục và 12 sản phẩm mẫu thành công!");
    }

    // Helper tạo Category
    private Category cat(String name, String slug, int order) {
        Category c = new Category();
        c.setName(name);
        c.setSlug(slug);
        c.setIsActive(true);
        c.setSortOrder(order);
        return c;
    }

    private Category save(Category c) {
        return categoryRepository.save(c);
    }
}
