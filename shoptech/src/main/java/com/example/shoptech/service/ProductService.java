package com.example.shoptech.service;

import com.example.shoptech.dto.ProductForm;
import com.example.shoptech.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    Page<Product> searchAdmin(
            String keyword,
            Long categoryId,
            Pageable pageable
    );

    Product getById(Long id);

    ProductForm getFormById(Long id);

    void create(ProductForm form);

    void update(Long id, ProductForm form);

    void moveToTrash(Long id);
}