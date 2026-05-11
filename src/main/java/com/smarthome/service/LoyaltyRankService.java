package com.smarthome.service;

import com.smarthome.entity.User;
import com.smarthome.entity.User.LoyaltyRank;
import com.smarthome.repository.OrderRepository;
import com.smarthome.repository.UserRepository;
import com.smarthome.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * =====================================================================
 *  NGUỒN CHÂN LÝ DUY NHẤT cho toàn bộ business rules về Loyalty Rank.
 *  - Tất cả ngưỡng chi tiêu và % giảm giá chỉ định nghĩa tại đây.
 *  - Các service khác PHẢI gọi qua class này, không tự tính.
 * =====================================================================
 *
 * Cơ chế giảm giá:
 *   1. Voucher áp dụng trước trên subtotal → ra afterVoucher
 *   2. Rank discount áp dụng sau trên afterVoucher
 *   3. Tổng giảm giá (voucher + rank) KHÔNG vượt quá MAX_TOTAL_DISCOUNT_RATE (13%) so với subtotal gốc
 *
 * Update rank:
 *   - Chỉ tăng, không tụt (một khi đạt VIP không xuống)
 *   - Auto-run sau mỗi đơn DELIVERED
 *   - totalSpent lưu trong DB, không tính lại mỗi lần query
 */
@Service
@Transactional(readOnly = true)
public class LoyaltyRankService {

    // ─── Ngưỡng chi tiêu (đơn vị: VNĐ) ───────────────────────────────
    private static final BigDecimal SILVER_THRESHOLD = new BigDecimal("30000000");   // 30 triệu
    private static final BigDecimal GOLD_THRESHOLD   = new BigDecimal("50000000");   // 50 triệu
    private static final BigDecimal VIP_THRESHOLD    = new BigDecimal("100000000");  // 100 triệu

    // ─── % giảm giá theo rank ─────────────────────────────────────────
    private static final BigDecimal SILVER_RATE = new BigDecimal("0.05"); // 5%
    private static final BigDecimal GOLD_RATE   = new BigDecimal("0.08"); // 8%
    private static final BigDecimal VIP_RATE    = new BigDecimal("0.10"); // 10%

    // ─── Giới hạn tổng giảm giá tối đa (voucher + rank) ─────────────
    private static final BigDecimal MAX_TOTAL_DISCOUNT_RATE = new BigDecimal("0.13"); // 13%

    private final UserRepository userRepo;
    private final OrderRepository orderRepo;

    public LoyaltyRankService(UserRepository userRepo, OrderRepository orderRepo) {
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
    }

    // ─── Tính hạng từ tổng tiền đã chi ───────────────────────────────

    /**
     * Tính hạng membership tương ứng với tổng chi tiêu.
     * Logic này phải đồng bộ với bảng hiển thị trên frontend.
     */
    public LoyaltyRank calculateRank(BigDecimal totalSpent) {
        if (totalSpent == null) return LoyaltyRank.NORMAL;
        if (totalSpent.compareTo(VIP_THRESHOLD)    >= 0) return LoyaltyRank.VIP;
        if (totalSpent.compareTo(GOLD_THRESHOLD)   >= 0) return LoyaltyRank.GOLD;
        if (totalSpent.compareTo(SILVER_THRESHOLD) >= 0) return LoyaltyRank.SILVER;
        return LoyaltyRank.NORMAL;
    }

    // ─── Lấy % discount theo rank ─────────────────────────────────────

    /**
     * Trả về tỷ lệ giảm giá (dạng thập phân) áp dụng trên số tiền sau voucher.
     * Ví dụ: SILVER → 0.05 (5%)
     */
    public BigDecimal getDiscountRate(LoyaltyRank rank) {
        if (rank == null) return BigDecimal.ZERO;
        return switch (rank) {
            case SILVER -> SILVER_RATE;
            case GOLD   -> GOLD_RATE;
            case VIP    -> VIP_RATE;
            default     -> BigDecimal.ZERO;
        };
    }

    // ─── Tính tiền giảm thực tế (có cap 13%) ──────────────────────────

    /**
     * Tính toán số tiền giảm theo rank, áp dụng TRÊN SỐ TIỀN SAU VOUCHER.
     * Đảm bảo tổng giảm giá (voucher + rank) không vượt 13% so với subtotal gốc.
     *
     * @param rank          hạng hiện tại của user
     * @param subtotal      giá trị đơn hàng TRƯỚC khi áp dụng bất kỳ giảm giá nào
     * @param voucherDiscount tiền đã giảm bởi voucher
     * @return số tiền giảm thêm từ rank (có thể = 0 nếu đã đạt cap)
     */
    public BigDecimal calcLoyaltyDiscount(LoyaltyRank rank, BigDecimal subtotal, BigDecimal voucherDiscount) {
        if (rank == null || subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal safeVoucherDiscount = (voucherDiscount != null) ? voucherDiscount : BigDecimal.ZERO;

        // Giới hạn tổng giảm giá tối đa = 13% của subtotal gốc
        BigDecimal maxTotalDiscount = subtotal.multiply(MAX_TOTAL_DISCOUNT_RATE).setScale(0, RoundingMode.DOWN);

        // Còn room giảm được bao nhiêu nữa?
        BigDecimal remainingRoom = maxTotalDiscount.subtract(safeVoucherDiscount);
        if (remainingRoom.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO; // Voucher đã dùng hết quota 13%
        }

        // Số tiền sau khi trừ voucher
        BigDecimal afterVoucher = subtotal.subtract(safeVoucherDiscount);
        if (afterVoucher.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Tính rank discount trên số tiền sau voucher
        BigDecimal rankDiscount = afterVoucher.multiply(getDiscountRate(rank)).setScale(0, RoundingMode.DOWN);

        // Cap: không được vượt quá room còn lại
        return rankDiscount.min(remainingRoom);
    }

    // ─── Cập nhật rank sau khi đơn DELIVERED ──────────────────────────

    /**
     * Tự động cập nhật totalSpent và loyaltyRank của user sau khi đơn hàng được xác nhận nhận.
     * - Rank chỉ tăng (không tụt).
     * - Gọi method này từ OrderService.confirmReceived().
     *
     * @param userId        ID của user cần cập nhật
     * @param deliveredAmount finalAmount của đơn vừa DELIVERED
     */
    @Transactional
    public void updateAfterDelivered(Long userId, BigDecimal deliveredAmount) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (deliveredAmount == null || deliveredAmount.compareTo(BigDecimal.ZERO) <= 0) return;

        // Cộng dồn tổng tiền đã chi
        BigDecimal newTotalSpent = (user.getTotalSpent() != null ? user.getTotalSpent() : BigDecimal.ZERO)
                .add(deliveredAmount);
        user.setTotalSpent(newTotalSpent);

        // Tính hạng mới từ tổng tiền mới
        LoyaltyRank newRank = calculateRank(newTotalSpent);

        // Rank chỉ tăng — không bao giờ tụt xuống
        if (newRank.ordinal() > user.getLoyaltyRank().ordinal()) {
            user.setLoyaltyRank(newRank);
        }

        userRepo.save(user);
    }

    // ─── Backfill: tính lại từ lịch sử đơn hàng (dùng cho data cũ) ───

    /**
     * Tính lại totalSpent và rank từ lịch sử đơn hàng DELIVERED.
     * Dùng cho admin để sync data cũ, hoặc khi cần recalibrate.
     */
    @Transactional
    public void recalculateForUser(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        BigDecimal totalSpent = orderRepo.sumDeliveredAmountByUser(userId);
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;

        user.setTotalSpent(totalSpent);
        user.setLoyaltyRank(calculateRank(totalSpent));
        userRepo.save(user);
    }

    // ─── Getters cho ngưỡng (dùng ở frontend/admin panel) ─────────────

    public BigDecimal getSilverThreshold() { return SILVER_THRESHOLD; }
    public BigDecimal getGoldThreshold()   { return GOLD_THRESHOLD; }
    public BigDecimal getVipThreshold()    { return VIP_THRESHOLD; }
    public BigDecimal getMaxTotalDiscountRate() { return MAX_TOTAL_DISCOUNT_RATE; }
}
