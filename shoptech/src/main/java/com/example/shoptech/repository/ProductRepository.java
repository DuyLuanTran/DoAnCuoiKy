package com.example.shoptech.repository;

import com.example.shoptech.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "category")
    @Query("""
        SELECT p
        FROM Product p
        WHERE p.isDeleted = false
          AND (
              LOWER(p.name) LIKE LOWER(
                  CONCAT(CONCAT('%', :keyword), '%')
              )
              OR LOWER(p.sku) LIKE LOWER(
                  CONCAT(CONCAT('%', :keyword), '%')
              )
          )
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
        """)
    Page<Product> searchAdmin(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "category")
    Optional<Product> findByIdAndIsDeletedFalse(Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByIsDeletedTrue(Pageable pageable);
}