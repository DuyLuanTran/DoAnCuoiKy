package com.myweb.shoppe_fake.Model;

import lombok.Data;

@Data
public class CartItem {
    private String productId;
    private int quantity;
}