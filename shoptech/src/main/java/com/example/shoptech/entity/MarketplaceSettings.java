package com.example.shoptech.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
@Entity @Table(name="marketplace_settings") @Getter @Setter
public class MarketplaceSettings {
    @Id private Integer id=1;
    @Column(name="default_commission_rate",nullable=false,precision=5,scale=2) private BigDecimal defaultCommissionRate=BigDecimal.ZERO;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="updated_by") private User updatedBy;
    @Column(name="updated_at") private OffsetDateTime updatedAt;
    @PrePersist @PreUpdate void update(){updatedAt=OffsetDateTime.now();}
}
