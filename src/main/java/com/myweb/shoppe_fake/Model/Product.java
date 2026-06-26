package com.myweb.shoppe_fake.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "products")
@Data
public class Product {
    @Id
    private String id;
    private String name;
    private String description;
    private double price;
    private int quantity;
    private String image;
    private String categoryId; // Liên kết với Category
    private boolean active = true;
}