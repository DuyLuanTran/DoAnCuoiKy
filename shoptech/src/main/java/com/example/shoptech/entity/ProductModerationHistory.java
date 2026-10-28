package com.example.shoptech.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
@Entity @Table(name="product_moderation_history") @Getter @Setter
public class ProductModerationHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="product_id") private Product product;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="actor_id") private User actor;
    @Column(nullable=false) private boolean blocked;
    @Column(nullable=false,length=1000) private String reason;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt=OffsetDateTime.now();
}
