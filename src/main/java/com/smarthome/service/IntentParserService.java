package com.smarthome.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phân tích intent (ý định) từ câu hỏi của người dùng.
 * Trích xuất: giá tiền, danh mục, thương hiệu.
 *
 * Ví dụ:
 *  "robot hút bụi dưới 10 triệu"   → { maxPrice: 10M, category: "robot" }
 *  "camera từ 1 đến 3 triệu"       → { minPrice: 1M, maxPrice: 3M, category: "camera" }
 *  "đèn Philips dưới 5 triệu"      → { maxPrice: 5M, category: "đèn", brand: "Philips" }
 */
@Service
public class IntentParserService {

    private static final Logger log = LoggerFactory.getLogger(IntentParserService.class);

    // ====== Mapping Keyword → Category Slug ======
    private static final Map<String, String> CATEGORY_KEYWORDS = new HashMap<>();
    static {
        CATEGORY_KEYWORDS.put("robot", "robot-hut-bui");
        CATEGORY_KEYWORDS.put("hút bụi", "robot-hut-bui");
        CATEGORY_KEYWORDS.put("máy lọc", "may-loc-khong-khi");
        CATEGORY_KEYWORDS.put("lọc khí", "may-loc-khong-khi");
        CATEGORY_KEYWORDS.put("không khí", "may-loc-khong-khi");
        CATEGORY_KEYWORDS.put("camera", "camera-an-ninh");
        CATEGORY_KEYWORDS.put("an ninh", "camera-an-ninh");
        CATEGORY_KEYWORDS.put("đèn", "den-thong-minh");
        CATEGORY_KEYWORDS.put("bóng đèn", "den-thong-minh");
        CATEGORY_KEYWORDS.put("ổ cắm", "o-cam-thong-minh");
        CATEGORY_KEYWORDS.put("khóa", "khoa-cua-thong-minh");
        CATEGORY_KEYWORDS.put("vân tay", "khoa-cua-thong-minh");
        CATEGORY_KEYWORDS.put("loa", "loa-thong-minh");
        CATEGORY_KEYWORDS.put("speaker", "loa-thong-minh");
    }

    // ====== Brands ======
    private static final String[] KNOWN_BRANDS = {
        "xiaomi", "ecovacs", "roborock", "dyson", "philips", "yeelight",
        "samsung", "ezviz", "google", "amazon", "tp-link", "aqara"
    };

    // ====== Regex Patterns ======
    // "dưới X triệu" / "dưới Xtr" / "dưới X000000đ"
    private static final Pattern UNDER_PRICE = Pattern.compile(
        "(?:dưới|không quá|tầm|khoảng|<=)\\s*(\\d+(?:[,.]\\d+)?)\\s*(?:triệu|tr|000\\.?000|đ)?",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    // "trên X triệu"
    private static final Pattern ABOVE_PRICE = Pattern.compile(
        "(?:trên|từ|>=)\\s*(\\d+(?:[,.]\\d+)?)\\s*(?:triệu|tr)?",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    // "từ X đến Y triệu" / "X-Y triệu"
    private static final Pattern RANGE_PRICE = Pattern.compile(
        "(?:từ\\s*)?(\\d+(?:[,.]\\d+)?)\\s*(?:đến|-)\\s*(\\d+(?:[,.]\\d+)?)\\s*(?:triệu|tr)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    /**
     * Phân tích câu hỏi và trả về Intent object
     */
    public Intent parse(String userMessage) {
        String lowerMsg = userMessage.toLowerCase().trim();
        Intent intent = new Intent();

        // Parse giá
        parsePriceRange(lowerMsg, intent);

        // Parse danh mục
        parseCategorySlug(lowerMsg, intent);

        // Parse thương hiệu
        parseBrand(lowerMsg, intent);

        log.info("🔍 Intent parsed: category='{}', brand='{}', minPrice={}, maxPrice={}",
                intent.categorySlug, intent.brand, intent.minPrice, intent.maxPrice);

        return intent;
    }

    private void parsePriceRange(String msg, Intent intent) {
        // Thử range trước (từ X đến Y)
        Matcher rangeMatcher = RANGE_PRICE.matcher(msg);
        if (rangeMatcher.find()) {
            intent.minPrice = toVND(rangeMatcher.group(1));
            intent.maxPrice = toVND(rangeMatcher.group(2));
            return;
        }

        // Thử "dưới X triệu"
        Matcher underMatcher = UNDER_PRICE.matcher(msg);
        if (underMatcher.find()) {
            intent.maxPrice = toVND(underMatcher.group(1));
        }

        // Thử "trên X triệu"
        Matcher aboveMatcher = ABOVE_PRICE.matcher(msg);
        if (aboveMatcher.find()) {
            intent.minPrice = toVND(aboveMatcher.group(1));
        }
    }

    private void parseCategorySlug(String msg, Intent intent) {
        for (Map.Entry<String, String> entry : CATEGORY_KEYWORDS.entrySet()) {
            if (msg.contains(entry.getKey())) {
                intent.categorySlug = entry.getValue();
                return;
            }
        }
    }

    private void parseBrand(String msg, Intent intent) {
        for (String brand : KNOWN_BRANDS) {
            if (msg.contains(brand)) {
                intent.brand = brand;
                return;
            }
        }
    }

    /**
     * Chuyển số (VD: "10" hoặc "10.5") sang VND (nhân 1_000_000)
     */
    private BigDecimal toVND(String rawNumber) {
        try {
            // Normalize: "10,5" → "10.5"
            String normalized = rawNumber.replace(",", ".");
            double val = Double.parseDouble(normalized);
            return BigDecimal.valueOf((long) (val * 1_000_000));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Intent object chứa kết quả parse
     */
    public static class Intent {
        /** Slug danh mục (ví dụ: "robot-hut-bui"), null nếu không detect được */
        public String categorySlug;
        /** Thương hiệu lowercase (ví dụ: "xiaomi"), null nếu không detect */
        public String brand;
        /** Giá tối thiểu (VND), null nếu không có */
        public BigDecimal minPrice;
        /** Giá tối đa (VND), null nếu không có */
        public BigDecimal maxPrice;

        /** True nếu có bất kỳ filter nào */
        public boolean hasFilter() {
            return categorySlug != null || brand != null
                    || minPrice != null || maxPrice != null;
        }

        @Override
        public String toString() {
            return String.format("Intent{category='%s', brand='%s', min=%s, max=%s}",
                    categorySlug, brand, minPrice, maxPrice);
        }
    }
}
