package com.example.shoptech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="product_reviews",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","product_id"})) @Getter @Setter
public class ProductReview {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Version private long version;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private User user;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="product_id") private Product product;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_detail_id") private OrderDetail purchase;
    @Column(nullable=false) private int rating;
    @Column(nullable=false,length=2000) private String content;
    @Column(nullable=false) private boolean hidden;
    @Column(length=1000) private String moderationReason;
    @Column(length=2000) private String sellerReply;
    private Instant repliedAt;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    @ElementCollection @CollectionTable(name="product_review_media",joinColumns=@JoinColumn(name="review_id")) @OrderColumn(name="position")
    private List<ReviewAttachment> media=new ArrayList<>();
}
