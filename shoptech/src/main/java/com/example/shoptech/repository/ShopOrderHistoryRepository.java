package com.example.shoptech.repository;
import com.example.shoptech.entity.ShopOrderHistory;
import org.springframework.data.jpa.repository.*;
import java.util.List;
public interface ShopOrderHistoryRepository extends JpaRepository<ShopOrderHistory,Long> {
    @EntityGraph(attributePaths="actor") List<ShopOrderHistory> findByShopOrderIdOrderByIdDesc(Long id);
    List<ShopOrderHistory> findByShopOrderIdOrderByIdAsc(Long id);
}
