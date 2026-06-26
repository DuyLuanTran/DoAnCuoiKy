package com.myweb.shoppe_fake.Model;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "users")
@Data
public class User {
    @Id
    private String id;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private Set<String> roles; // ROLE_USER hoặc ROLE_ADMIN
    private boolean active = true;
    private String resetToken;
}