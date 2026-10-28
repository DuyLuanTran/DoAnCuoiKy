package com.example.shoptech.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
@Entity @Table(name="shop_review_history") @Getter @Setter
public class ShopReviewHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="shop_id") private Shop shop;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="actor_id") private User actor;
    @Enumerated(EnumType.STRING) @Column(name="previous_status",nullable=false) private ShopStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name="new_status",nullable=false) private ShopStatus newStatus;
    @Column(length=1000) private String reason;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt=OffsetDateTime.now();
}
