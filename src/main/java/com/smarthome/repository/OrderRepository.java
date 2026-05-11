package com.smarthome.repository;

import com.smarthome.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);

    long countByStatus(Order.OrderStatus status);

    List<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);

    @Query("SELECT SUM(o.finalAmount) FROM Order o WHERE o.status = 'DELIVERED' " +
           "AND o.createdAt BETWEEN :from AND :to")
    BigDecimal sumRevenueByDateRange(@Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :from AND :to")
    Long countOrdersByDateRange(@Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    @Query("SELECT SUM(o.finalAmount) FROM Order o WHERE o.status = 'DELIVERED'")
    BigDecimal sumTotalRevenue();

    @Query("SELECT MONTH(o.createdAt) as month, SUM(o.finalAmount) as revenue " +
           "FROM Order o WHERE o.status = 'DELIVERED' AND YEAR(o.createdAt) = :year " +
           "GROUP BY MONTH(o.createdAt) ORDER BY MONTH(o.createdAt)")
    List<Object[]> getMonthlyRevenue(@Param("year") int year);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o " +
           "WHERE o.user.id = :userId AND o.status = 'DELIVERED'")
    BigDecimal sumDeliveredAmountByUser(@Param("userId") Long userId);

    @Query("SELECT SUM(o.actualShippingCost) FROM Order o WHERE o.status = 'DELIVERED'")
    BigDecimal sumTotalActualShippingCost();

    @Query("SELECT SUM(p.costPrice * oi.quantity) FROM OrderItem oi JOIN oi.product p JOIN oi.order o WHERE o.status = 'DELIVERED'")
    BigDecimal sumTotalCostOfGoods();

    @Query("SELECT MONTH(o.createdAt) as month, SUM(o.finalAmount) as revenue, SUM(o.actualShippingCost) as shipping " +
           "FROM Order o WHERE o.status = 'DELIVERED' AND YEAR(o.createdAt) = :year " +
           "GROUP BY MONTH(o.createdAt) ORDER BY MONTH(o.createdAt)")
    List<Object[]> getMonthlyRevenueAndShipping(@Param("year") int year);

    @Query("SELECT MONTH(o.createdAt) as month, SUM(p.costPrice * oi.quantity) as cost " +
           "FROM OrderItem oi JOIN oi.product p JOIN oi.order o " +
           "WHERE o.status = 'DELIVERED' AND YEAR(o.createdAt) = :year " +
           "GROUP BY MONTH(o.createdAt) ORDER BY MONTH(o.createdAt)")
    List<Object[]> getMonthlyCostOfGoodsByYear(@Param("year") int year);
}
