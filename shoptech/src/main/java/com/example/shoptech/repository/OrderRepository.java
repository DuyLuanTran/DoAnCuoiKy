package com.example.shoptech.repository;
import com.example.shoptech.entity.Order;
import com.example.shoptech.entity.OrderStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface OrderRepository extends JpaRepository<Order, Long> {
    interface RevenueDay {
        java.time.LocalDate getBucket();
        java.math.BigDecimal getRevenue();
        Long getOrderCount();
    }
    @Query(value = """
        SELECT CAST(created_at AT TIME ZONE 'Asia/Ho_Chi_Minh' AS DATE) AS bucket,
               SUM(total_amount) AS revenue, COUNT(*) AS orderCount
        FROM orders
        WHERE is_deleted = false AND order_status = 'COMPLETED'
          AND created_at >= :start AND created_at < :end
        GROUP BY CAST(created_at AT TIME ZONE 'Asia/Ho_Chi_Minh' AS DATE)
        ORDER BY bucket
        """, nativeQuery = true)
    java.util.List<RevenueDay> revenueDays(@org.springframework.data.repository.query.Param("start") java.time.OffsetDateTime start,
            @org.springframework.data.repository.query.Param("end") java.time.OffsetDateTime end);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.isDeleted = false")
    Optional<Order> lockActiveById(@org.springframework.data.repository.query.Param("id") Long id);
    @EntityGraph(attributePaths = {"user", "details"})
    Optional<Order> findByIdAndIsDeletedFalse(Long id);
    @EntityGraph(attributePaths = "user")
    @Query("SELECT o FROM Order o WHERE o.isDeleted = false AND (:status IS NULL OR o.orderStatus = :status) AND "
            + "(LOWER(o.orderCode) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(o.user.username) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(o.user.fullName) LIKE LOWER(CONCAT('%',:keyword,'%')))")
    Page<Order> searchAdmin(@org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("status") OrderStatus status, Pageable pageable);
    @EntityGraph(attributePaths = {"user", "details"})
    Optional<Order> findByOrderCodeAndUserIdAndIsDeletedFalse(String code, Long userId);
    @EntityGraph(attributePaths = {"user", "details"})
    Optional<Order> findByIdAndUserUsernameAndIsDeletedFalse(Long id, String username);
    @EntityGraph(attributePaths = "user")
    Page<Order> findByUserUsernameAndIsDeletedFalse(String username, Pageable pageable);
}
