package com.example.shoptech.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
@Entity @Table(name="shop_order_history") @Getter @Setter
public class ShopOrderHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="shop_order_id") private ShopOrder shopOrder;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="actor_id") private User actor;
    @Enumerated(EnumType.STRING) @Column(name="previous_status",nullable=false,length=30) private OrderStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name="new_status",nullable=false,length=30) private OrderStatus newStatus;
    @Column(nullable=false,length=1000) private String reason;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @PrePersist void create(){createdAt=OffsetDateTime.now();}
}
