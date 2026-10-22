package com.example.shoptech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(name = "uq_cart_product", columnNames = {"cart_id", "product_id"}))
@Getter @Setter
public class CartItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "cart_id", nullable = false)
    private Long cartId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(nullable = false)
    private Integer quantity;
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @PrePersist void create() { createdAt = updatedAt = OffsetDateTime.now(); }
    @PreUpdate void update() { updatedAt = OffsetDateTime.now(); }
}
