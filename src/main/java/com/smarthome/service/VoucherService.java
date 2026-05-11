package com.smarthome.service;

import com.smarthome.common.BusinessException;
import com.smarthome.common.ResourceNotFoundException;
import com.smarthome.dto.VoucherDto;
import com.smarthome.dto.VoucherRequestDto;
import com.smarthome.entity.Voucher;
import com.smarthome.repository.VoucherRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepo;

    public VoucherService(VoucherRepository voucherRepo) {
        this.voucherRepo = voucherRepo;
    }

    // ---- Admin CRUD ----

    public Page<VoucherDto> getAll(int page, int size) {
        return voucherRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(this::toDto);
    }

    public Page<VoucherDto> getAllActive(int page, int size) {
        return voucherRepo.findActiveVouchers(LocalDateTime.now(), PageRequest.of(page, size))
                .map(this::toDto);
    }

    @Transactional
    public VoucherDto create(VoucherRequestDto req) {
        String upperCode = req.getCode().toUpperCase().trim();
        if (voucherRepo.existsByCode(upperCode)) {
            throw new BusinessException("Mã voucher '" + upperCode + "' đã tồn tại!");
        }
        validateRequest(req);

        Voucher v = new Voucher();
        v.setCode(upperCode);
        v.setDescription(req.getDescription());
        v.setDiscountType(req.getDiscountType());
        v.setDiscountValue(req.getDiscountValue());
        v.setMinOrderValue(req.getMinOrderValue());
        v.setMaxDiscountAmount(req.getMaxDiscountAmount());
        v.setUsageLimit(req.getUsageLimit());
        v.setStartDate(req.getStartDate());
        v.setEndDate(req.getEndDate());
        v.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        v.setUsageCount(0); // Initial count

        return toDto(voucherRepo.save(v));
    }

    @Transactional
    public VoucherDto update(Long id, VoucherRequestDto req) {
        Voucher v = voucherRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher không tồn tại"));

        String upperCode = req.getCode().toUpperCase().trim();
        // Check for duplicate code only if changed
        if (!v.getCode().equals(upperCode) && voucherRepo.existsByCode(upperCode)) {
            throw new BusinessException("Mã voucher '" + upperCode + "' đã tồn tại!");
        }
        validateRequest(req);

        v.setCode(upperCode);
        v.setDescription(req.getDescription());
        v.setDiscountType(req.getDiscountType());
        v.setDiscountValue(req.getDiscountValue());
        v.setMinOrderValue(req.getMinOrderValue());
        v.setMaxDiscountAmount(req.getMaxDiscountAmount());
        v.setUsageLimit(req.getUsageLimit());
        v.setStartDate(req.getStartDate());
        v.setEndDate(req.getEndDate());
        if (req.getIsActive() != null) v.setIsActive(req.getIsActive());

        return toDto(voucherRepo.save(v));
    }

    @Transactional
    public void delete(Long id) {
        if (!voucherRepo.existsById(id)) {
            throw new ResourceNotFoundException("Voucher không tồn tại");
        }
        voucherRepo.deleteById(id);
    }

    // ---- User-facing: Apply Voucher ----

    /**
     * Validates a voucher code and returns the discount amount.
     * Does NOT increment usageCount — that happens on order creation.
     */
    public VoucherApplyResult applyVoucher(String code, BigDecimal orderTotal) {
        Voucher v = voucherRepo.findValidVoucher(code.toUpperCase().trim(), LocalDateTime.now())
                .orElseThrow(() -> new BusinessException(
                        "Mã voucher không hợp lệ, đã hết hạn hoặc đã dùng hết!"));

        if (v.getMinOrderValue() != null && orderTotal.compareTo(v.getMinOrderValue()) < 0) {
            throw new BusinessException(
                    String.format("Đơn hàng phải từ %,.0f đ mới được dùng voucher này!", v.getMinOrderValue()));
        }

        BigDecimal discount;
        if (v.getDiscountType() == Voucher.DiscountType.PERCENTAGE) {
            discount = orderTotal.multiply(v.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
            if (v.getMaxDiscountAmount() != null && discount.compareTo(v.getMaxDiscountAmount()) > 0) {
                discount = v.getMaxDiscountAmount();
            }
        } else {
            discount = v.getDiscountValue();
            if (discount.compareTo(orderTotal) > 0) discount = orderTotal; // Can't discount more than total
        }

        return new VoucherApplyResult(v.getId(), v.getCode(), discount);
    }

    @Transactional
    public void incrementUsage(Long voucherId) {
        voucherRepo.findById(voucherId).ifPresent(v -> {
            Integer currentUsage = v.getUsageCount();
            v.setUsageCount((currentUsage != null ? currentUsage : 0) + 1);
            voucherRepo.save(v);
        });
    }

    // ---- Private helpers ----

    private void validateRequest(VoucherRequestDto req) {
        if (req.getDiscountType() == Voucher.DiscountType.PERCENTAGE) {
            if (req.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException("Mức giảm % không được vượt quá 100%");
            }
        }
        if (req.getStartDate() != null && req.getEndDate() != null &&
                req.getStartDate().isAfter(req.getEndDate())) {
            throw new BusinessException("Ngày bắt đầu phải trước ngày kết thúc");
        }
    }

    private VoucherDto toDto(Voucher v) {
        VoucherDto dto = new VoucherDto();
        dto.setId(v.getId());
        dto.setCode(v.getCode());
        dto.setDescription(v.getDescription());
        dto.setDiscountType(v.getDiscountType());
        dto.setDiscountValue(v.getDiscountValue());
        dto.setMinOrderValue(v.getMinOrderValue());
        dto.setMaxDiscountAmount(v.getMaxDiscountAmount());
        dto.setUsageLimit(v.getUsageLimit());
        dto.setUsageCount(v.getUsageCount());
        dto.setStartDate(v.getStartDate());
        dto.setEndDate(v.getEndDate());
        dto.setIsActive(v.getIsActive());
        dto.setCreatedAt(v.getCreatedAt());
        return dto;
    }

    // Inner result class
    public record VoucherApplyResult(Long voucherId, String code, BigDecimal discountAmount) {}
}
