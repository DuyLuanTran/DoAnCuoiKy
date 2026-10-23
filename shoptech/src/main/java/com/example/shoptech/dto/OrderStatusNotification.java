package com.example.shoptech.dto;
import com.example.shoptech.entity.OrderStatus;
public record OrderStatusNotification(Long orderId, String orderCode, OrderStatus status) {}
