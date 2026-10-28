package com.example.shoptech.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Entity @Table(name = "order_details") @Getter @Setter
public class OrderDetail {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id") private Order order;
    @Column(name = "product_id", nullable = false) private Long productId;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="shop_order_id",nullable=false) private ShopOrder shopOrder;
    @Column(name = "product_name", nullable = false, length = 255) private String productName;
    @Column(name = "product_image", columnDefinition = "TEXT") private String productImage;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal price;
    @Column(nullable = false) private Integer quantity;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal subtotal;
}
