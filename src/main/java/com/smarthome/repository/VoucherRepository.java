package com.smarthome.repository;

import com.smarthome.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCode(String code);

    boolean existsByCode(String code);

    Page<Voucher> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT v FROM Voucher v WHERE v.isActive = true " +
           "AND (v.startDate IS NULL OR v.startDate <= :now) " +
           "AND (v.endDate IS NULL OR v.endDate >= :now) " +
           "AND (v.usageLimit IS NULL OR COALESCE(v.usageCount, 0) < v.usageLimit) " +
           "ORDER BY v.createdAt DESC")
    Page<Voucher> findActiveVouchers(LocalDateTime now, Pageable pageable);

    @Query("SELECT v FROM Voucher v WHERE v.code = :code AND v.isActive = true " +
           "AND (v.startDate IS NULL OR v.startDate <= :now) " +
           "AND (v.endDate IS NULL OR v.endDate >= :now) " +
           "AND (v.usageLimit IS NULL OR COALESCE(v.usageCount, 0) < v.usageLimit)")
    Optional<Voucher> findValidVoucher(String code, LocalDateTime now);
}
