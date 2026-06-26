package com.myweb.shoppe_fake.Model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "carts")
@Data
public class Cart {
    @Id
    private String id;
    private String userId; // Mỗi user có 1 giỏ hàng
    private List<CartItem> items = new ArrayList<>();
}