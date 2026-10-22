package com.example.shoptech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Entity @Table(name = "orders") @Getter @Setter
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_code", nullable = false, unique = true, length = 30) private String orderCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "recipient_name", nullable = false, length = 100) private String recipientName;
    @Column(name = "recipient_phone", nullable = false, length = 20) private String recipientPhone;
    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT") private String shippingAddress;
    @Column(columnDefinition = "TEXT") private String note;
    @Column(name = "subtotal_amount", nullable = false, precision = 15, scale = 2) private BigDecimal subtotalAmount;
    @Column(name = "shipping_fee", nullable = false, precision = 15, scale = 2) private BigDecimal shippingFee = BigDecimal.ZERO;
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2) private BigDecimal totalAmount;
    @Column(name = "payment_method", nullable = false, length = 30) private String paymentMethod = "COD";
    @Column(name = "payment_status", nullable = false, length = 30) private String paymentStatus = "UNPAID";
    @Enumerated(EnumType.STRING) @Column(name = "order_status", nullable = false, length = 30) private OrderStatus orderStatus = OrderStatus.PENDING;
    @Column(name = "is_deleted", nullable = false) private Boolean isDeleted = false;
    @Column(name = "deleted_at") private OffsetDateTime deletedAt;
    @Column(name = "created_at", updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at") private OffsetDateTime updatedAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL) @OrderBy("id ASC")
    private List<OrderDetail> details = new ArrayList<>();
    @PrePersist void create() { createdAt = updatedAt = OffsetDateTime.now(); }
    @PreUpdate void update() { updatedAt = OffsetDateTime.now(); }
}
