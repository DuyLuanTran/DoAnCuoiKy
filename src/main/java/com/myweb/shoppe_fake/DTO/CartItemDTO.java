package com.myweb.shoppe_fake.DTO;

import lombok.Data;

@Data
public class CartItemDTO {
    private String productId;
    private String productName;
    private String image;
    private double price;
    private int quantity;
    private double subtotal; 
}