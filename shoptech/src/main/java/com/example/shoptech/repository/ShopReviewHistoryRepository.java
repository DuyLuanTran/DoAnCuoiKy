package com.example.shoptech.repository;
import com.example.shoptech.entity.ShopReviewHistory;
import org.springframework.data.jpa.repository.*;
import java.util.List;
public interface ShopReviewHistoryRepository extends JpaRepository<ShopReviewHistory,Long> {
    @EntityGraph(attributePaths="actor") List<ShopReviewHistory> findByShopIdOrderByCreatedAtDescIdDesc(Long shopId);
}
