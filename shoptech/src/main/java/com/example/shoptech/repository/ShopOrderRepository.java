package com.example.shoptech.repository;
import com.example.shoptech.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ShopOrderRepository extends JpaRepository<ShopOrder,Long> {
    @EntityGraph(attributePaths={"order","order.user","shop"})
    @Query("select s from ShopOrder s where s.order.isDeleted=false and (:shopId is null or s.shop.id=:shopId) and (:status is null or s.status=:status) and (lower(s.order.orderCode) like lower(concat('%',:keyword,'%')) or lower(s.order.user.username) like lower(concat('%',:keyword,'%')) or lower(s.order.recipientName) like lower(concat('%',:keyword,'%')))")
    Page<ShopOrder> search(String keyword,OrderStatus status,Long shopId,Pageable pageable);
    @EntityGraph(attributePaths={"order","order.user","shop"})
    Optional<ShopOrder> findByIdAndShopIdAndOrderIsDeletedFalse(Long id,Long shopId);
    @EntityGraph(attributePaths={"order","order.user","shop"})
    Optional<ShopOrder> findByIdAndOrderIsDeletedFalse(Long id);
    @Query("select s from ShopOrder s join fetch s.order where s.order.isDeleted=false and s.status=com.example.shoptech.entity.OrderStatus.COMPLETED and (:shopId is null or s.shop.id=:shopId) and s.createdAt>=:start and s.createdAt<:end order by s.createdAt,s.id")
    List<ShopOrder> revenue(Long shopId,java.time.OffsetDateTime start,java.time.OffsetDateTime end);
}
