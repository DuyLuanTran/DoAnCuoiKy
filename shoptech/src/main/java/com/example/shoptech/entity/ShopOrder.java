package com.example.shoptech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** A shop's immutable commercial snapshot inside one customer checkout. */
@Entity @Table(name="shop_orders",uniqueConstraints=@UniqueConstraint(columnNames={"order_id","shop_id"}))
@Getter @Setter
public class ShopOrder {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id",nullable=false) private Order order;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="shop_id",nullable=false) private Shop shop;
    @Column(name="shop_name",nullable=false,length=100) private String shopName;
    @Column(nullable=false,precision=15,scale=2) private BigDecimal subtotal=BigDecimal.ZERO;
    @Column(name="shipping_fee",nullable=false,precision=15,scale=2) private BigDecimal shippingFee=BigDecimal.ZERO;
    @Column(nullable=false,precision=15,scale=2) private BigDecimal total=BigDecimal.ZERO;
    @Column(name="commission_rate",nullable=false,precision=5,scale=2) private BigDecimal commissionRate;
    @Column(name="commission_amount",nullable=false,precision=15,scale=2) private BigDecimal commissionAmount;
    @Column(name="seller_amount",nullable=false,precision=15,scale=2) private BigDecimal sellerAmount;
    @Column(name="historical_import",nullable=false) private Boolean historicalImport=false;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private OrderStatus status=OrderStatus.PENDING;
    @Column(name="payment_status",nullable=false,length=30) private String paymentStatus="UNPAID";
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    @PrePersist void create(){if(createdAt==null)createdAt=OffsetDateTime.now();updatedAt=createdAt;}
    @PreUpdate void update(){updatedAt=OffsetDateTime.now();}
}
