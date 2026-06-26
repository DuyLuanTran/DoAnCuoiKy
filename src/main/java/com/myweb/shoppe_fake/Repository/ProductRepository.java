package com.myweb.shoppe_fake.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.myweb.shoppe_fake.Model.Product;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByCategoryId(String categoryId);
    
    // Tìm kiếm sản phẩm theo tên (case-insensitive) với phân trang
    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
    
    // Tìm kiếm sản phẩm theo tên hoặc mô tả với phân trang
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        String name, String description, Pageable pageable);
}