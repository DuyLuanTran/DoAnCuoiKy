package com.myweb.shoppe_fake.Model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Document(collection = "orders")
@Data
public class Order {
    @Id
    private String id;
    private String userId;
    private Date orderDate = new Date();
    private double totalAmount;
    private String status; // PENDING, SHIPPING, COMPLETED, CANCELLED
    private String shippingAddress;
    private String phone;
    private List<OrderDetail> details;
}