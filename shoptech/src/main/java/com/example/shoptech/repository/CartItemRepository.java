package com.example.shoptech.repository;
import com.example.shoptech.entity.CartItem;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @EntityGraph(attributePaths = {"product", "product.category"})
    List<CartItem> findByCartIdOrderByProductIdAsc(Long cartId);
    Optional<CartItem> findByIdAndCartId(Long id, Long cartId);
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}
