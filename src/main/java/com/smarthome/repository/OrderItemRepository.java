package com.smarthome.repository;

import com.smarthome.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT p.name, SUM(oi.quantity) as totalSold, MAX(p.price), " +
           "SUM((oi.unitPrice - p.costPrice) * oi.quantity) as totalProfit " +
           "FROM OrderItem oi JOIN oi.product p GROUP BY p.id, p.name " +
           "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts();
}
