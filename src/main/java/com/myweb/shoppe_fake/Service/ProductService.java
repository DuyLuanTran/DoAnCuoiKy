package com.myweb.shoppe_fake.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.myweb.shoppe_fake.Model.Product;
import com.myweb.shoppe_fake.Repository.OrderRepository;
import com.myweb.shoppe_fake.Repository.ProductRepository;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderDetailRepository;
    public List<Product> getAllProducts() { 
        return productRepository.findAll(); 
    }
    
    public Optional<Product> getProductById(String id) { 
        return productRepository.findById(id); 
    }
    
    public List<Product> getProductsByCategory(String categoryId) { 
        return productRepository.findByCategoryId(categoryId); 
    }
    
    public Product saveProduct(Product product) { 
        return productRepository.save(product); 
    }
    
    public void deleteProduct(String id) { 
        boolean hasOrders = orderDetailRepository.existsByProductId(id);
    
    if (hasOrders) {
        throw new RuntimeException("LỖI: Không thể xóa vì sản phẩm này đã phát sinh đơn đặt hàng!");
    }
    
        productRepository.deleteById(id); 
    }
    
    // === PHÂN TRANG ===
    public Page<Product> getAllProductsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findAll(pageable);
    }
    
    // === TÌM KIẾM + PHÂN TRANG ===
    public Page<Product> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            keyword, keyword, pageable
        );
    }
}