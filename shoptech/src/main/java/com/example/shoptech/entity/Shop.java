package com.example.shoptech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity @Table(name="shops") @Getter @Setter
public class Shop {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="owner_id",unique=true,nullable=false) private User owner;
    @Column(nullable=false,length=100) private String name;
    @Column(nullable=false,unique=true,length=160) private String slug;
    @Column(length=2000) private String description;
    @Column(nullable=false,length=100) private String email;
    @Column(nullable=false,length=20) private String phone;
    @Column(nullable=false,length=500) private String address;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ShopStatus status=ShopStatus.PENDING;
    @Column(name="commission_rate",precision=5,scale=2) private BigDecimal commissionRate;
    @Column(name="submitted_at",nullable=false) private OffsetDateTime submittedAt;
    @Column(name="reviewed_at") private OffsetDateTime reviewedAt;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reviewed_by") private User reviewedBy;
    @Column(name="rejection_reason",length=1000) private String rejectionReason;
    @Column(name="suspension_reason",length=1000) private String suspensionReason;
    @Version private long version;
    @Column(name="created_at",updatable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at") private OffsetDateTime updatedAt;
    @PrePersist void create(){createdAt=updatedAt=OffsetDateTime.now();if(submittedAt==null)submittedAt=createdAt;}
    @PreUpdate void update(){updatedAt=OffsetDateTime.now();}
}
