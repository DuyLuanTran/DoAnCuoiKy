package com.example.shoptech.service;

import com.example.shoptech.entity.Product;
import com.example.shoptech.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductCatalogService {

    private final ProductRepository productRepository;

    public ProductCatalogService(
            ProductRepository productRepository
    ) {
        this.productRepository = productRepository;
    }

    public Page<Product> search(
            String keyword,
            Long categoryId,
            Pageable pageable
    ) {
        String normalizedKeyword = keyword == null
                ? ""
                : keyword.strip();

        return productRepository.searchCatalog(
                normalizedKeyword,
                categoryId,
                pageable
        );
    }

    public Optional<Product> findBySlug(String slug) {
        return productRepository.findVisibleBySlug(slug);
    }
}