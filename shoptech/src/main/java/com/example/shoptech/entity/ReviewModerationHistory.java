package com.example.shoptech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name="review_moderation_history") @Getter @Setter
public class ReviewModerationHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="review_id") private ProductReview review;
    @Column(nullable=false,length=50) private String actor;
    @Column(nullable=false) private boolean hidden;
    @Column(nullable=false,length=1000) private String reason;
    @Column(nullable=false) private Instant createdAt=Instant.now();
}
