package com.example.shoptech.service;
import com.example.shoptech.dto.OrderStatusNotification;
public record OrderStatusChanged(String username, OrderStatusNotification notification) {}
