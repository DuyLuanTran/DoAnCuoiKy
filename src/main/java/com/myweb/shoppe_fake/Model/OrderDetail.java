package com.myweb.shoppe_fake.Model;

import lombok.Data;

@Data
public class OrderDetail {
    private String productId;
    private String productName;
    private int quantity;
    private double price; // Giá tại thời điểm đặt hàng
}