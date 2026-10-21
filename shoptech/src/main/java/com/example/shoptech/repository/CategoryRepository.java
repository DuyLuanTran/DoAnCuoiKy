package com.example.shoptech.repository;

import com.example.shoptech.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
        SELECT c
        FROM Category c
        WHERE c.isDeleted = false
          AND LOWER(c.name) LIKE LOWER(
              CONCAT(CONCAT('%', :keyword), '%')
          )
        """)
    Page<Category> search(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Optional<Category> findByIdAndIsDeletedFalse(Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<Category> findByIsDeletedFalseAndIsActiveTrueOrderByNameAsc();

    Page<Category> findByIsDeletedTrue(Pageable pageable);

    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM products
            WHERE category_id = :categoryId
              AND is_deleted = false
        )
        """, nativeQuery = true)
    boolean hasNonDeletedProducts(
            @Param("categoryId") Long categoryId
    );
}