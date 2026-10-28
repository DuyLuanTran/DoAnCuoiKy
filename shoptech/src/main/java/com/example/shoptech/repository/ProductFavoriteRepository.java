package com.example.shoptech.repository;

import com.example.shoptech.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;

public interface ProductFavoriteRepository extends JpaRepository<ProductFavorite,Long> {
    String VISIBLE="p.isActive=true and p.isDeleted=false and p.isBlocked=false and p.category.isActive=true and p.category.isDeleted=false and p.shop.status=com.example.shoptech.entity.ShopStatus.ACTIVE and p.shop.owner.isActive=true and p.shop.owner.isDeleted=false";
    Optional<ProductFavorite> findByUserIdAndProductId(Long userId,Long productId);
    boolean existsByUserIdAndProductId(Long userId,Long productId);
    @Query("select f.product.id from ProductFavorite f where f.user.username=:username and f.product.id in :ids")
    Set<Long> selected(String username,Collection<Long> ids);
    @Query(value="select p from ProductFavorite f join f.product p join fetch p.category join fetch p.shop where f.user.username=:username and "+VISIBLE+" order by f.createdAt desc, f.id desc",
      countQuery="select count(f) from ProductFavorite f join f.product p where f.user.username=:username and "+VISIBLE)
    Page<Product> visible(String username,Pageable pageable);
}
