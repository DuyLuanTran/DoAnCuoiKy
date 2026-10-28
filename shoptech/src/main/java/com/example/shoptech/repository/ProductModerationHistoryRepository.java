package com.example.shoptech.repository;
import com.example.shoptech.entity.ProductModerationHistory;
import org.springframework.data.jpa.repository.*;
import java.util.List;
public interface ProductModerationHistoryRepository extends JpaRepository<ProductModerationHistory,Long> {
    @EntityGraph(attributePaths="actor") List<ProductModerationHistory> findByProductIdOrderByCreatedAtDescIdDesc(Long productId);
}
