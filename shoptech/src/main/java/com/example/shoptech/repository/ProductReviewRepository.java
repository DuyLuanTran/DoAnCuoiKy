package com.example.shoptech.repository;

import com.example.shoptech.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;

public interface ProductReviewRepository extends JpaRepository<ProductReview,Long> {
    boolean existsByUserIdAndProductId(Long userId,Long productId);
    @EntityGraph(attributePaths={"user","product","product.shop"})
    Page<ProductReview> findByProductIdAndHiddenFalseOrderByCreatedAtDescIdDesc(Long productId,Pageable page);
    @Query("select r from ProductReview r where (:shopId is null or r.product.shop.id=:shopId) order by r.createdAt desc, r.id desc")
    @EntityGraph(attributePaths={"user","product","product.shop"}) Page<ProductReview> manage(Long shopId,Pageable page);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ProductReview r where r.id=:id") Optional<ProductReview> lock(Long id);
    @Query("select r.product.id, avg(r.rating), count(r) from ProductReview r join r.product p where "+ProductFavoriteRepository.VISIBLE+" and r.hidden=false and r.product.id in :ids group by r.product.id")
    List<Object[]> summaries(Collection<Long> ids);
}
