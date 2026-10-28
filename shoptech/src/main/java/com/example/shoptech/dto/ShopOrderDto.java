package com.example.shoptech.dto;
import com.example.shoptech.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
public record ShopOrderDto(Long id,Long orderId,String code,String shopName,String customer,String recipientName,
        BigDecimal total,OrderStatus status,String paymentStatus,OffsetDateTime createdAt,
        BigDecimal commissionRate,BigDecimal commissionAmount,BigDecimal sellerAmount,boolean historicalImport) {}
