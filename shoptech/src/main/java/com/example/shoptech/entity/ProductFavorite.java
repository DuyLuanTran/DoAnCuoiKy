package com.example.shoptech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name="product_favorites",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","product_id"})) @Getter @Setter
public class ProductFavorite {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE) private User user;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="product_id") private Product product;
    @Column(nullable=false) private Instant createdAt=Instant.now();
}
