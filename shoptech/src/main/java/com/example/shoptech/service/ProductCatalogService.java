package com.example.shoptech.service;

import com.example.shoptech.entity.Product;
import com.example.shoptech.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductCatalogService {

    private final ProductRepository productRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ProductCatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Giữ phương thức cũ để các phần đang gọi không bị ảnh hưởng.
    public Page<Product> search(
            String keyword,
            Long categoryId,
            Pageable pageable
    ) {
        return search(
                keyword, categoryId, null, null,
                false, false, "newest", pageable
        );
    }

    public Page<Product> search(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean inStock,
            boolean onSale,
            String sort,
            Pageable pageable
    ) {
        validatePrices(minPrice, maxPrice);

        String normalizedKeyword = keyword == null ? "" : keyword.strip();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Đếm kết quả bằng cùng điều kiện với truy vấn danh sách.
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Product> countRoot = countQuery.from(Product.class);

        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(
                cb, countRoot, normalizedKeyword, categoryId,
                minPrice, maxPrice, inStock, onSale
        ));

        long total = entityManager.createQuery(countQuery).getSingleResult();
        int size = pageable.getPageSize();

        if (total == 0) {
            return new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, size),
                    0
            );
        }

        int lastPage = (int) Math.min(
                Integer.MAX_VALUE,
                (total - 1) / size
        );

        int page = Math.min(pageable.getPageNumber(), lastPage);
        Pageable actualPageable = PageRequest.of(page, size);

        if (actualPageable.getOffset() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Số trang vượt giới hạn.");
        }

        CriteriaQuery<Product> query = cb.createQuery(Product.class);
        Root<Product> root = query.from(Product.class);

        // Tải kèm danh mục để hiển thị tên mà không query từng sản phẩm.
        root.fetch("category", JoinType.INNER);

        query.select(root);
        query.where(buildPredicates(
                cb, root, normalizedKeyword, categoryId,
                minPrice, maxPrice, inStock, onSale
        ));

        Expression<BigDecimal> sellingPrice = cb.coalesce(
                root.<BigDecimal>get("discountPrice"),
                root.<BigDecimal>get("price")
        );

        switch (sort == null ? "newest" : sort) {
            case "priceAsc" ->
                    query.orderBy(
                            cb.asc(sellingPrice),
                            cb.desc(root.get("id"))
                    );
            case "priceDesc" ->
                    query.orderBy(
                            cb.desc(sellingPrice),
                            cb.desc(root.get("id"))
                    );
            case "nameAsc" ->
                    query.orderBy(
                            cb.asc(cb.lower(root.<String>get("name"))),
                            cb.desc(root.get("id"))
                    );
            default ->
                    query.orderBy(
                            cb.desc(root.get("createdAt")),
                            cb.desc(root.get("id"))
                    );
        }

        List<Product> content = entityManager.createQuery(query)
                .setFirstResult((int) actualPageable.getOffset())
                .setMaxResults(size)
                .getResultList();

        return new PageImpl<>(content, actualPageable, total);
    }

    public Optional<Product> findBySlug(String slug) {
        return productRepository.findVisibleBySlug(slug);
    }

    private Predicate[] buildPredicates(
            CriteriaBuilder cb,
            Root<Product> root,
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean inStock,
            boolean onSale
    ) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.isFalse(root.<Boolean>get("isDeleted")));
        predicates.add(cb.isTrue(root.<Boolean>get("isActive")));
        predicates.add(cb.isFalse(
                root.get("category").<Boolean>get("isDeleted")
        ));
        predicates.add(cb.isTrue(
                root.get("category").<Boolean>get("isActive")
        ));

        if (!keyword.isBlank()) {
            String escapedKeyword = keyword
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");

            predicates.add(cb.like(
                    cb.lower(root.<String>get("name")),
                    cb.lower(cb.literal("%" + escapedKeyword + "%")),
                    '\\'
            ));
        }

        if (categoryId != null) {
            predicates.add(cb.equal(
                    root.get("category").get("id"),
                    categoryId
            ));
        }

        Expression<BigDecimal> sellingPrice = cb.coalesce(
                root.<BigDecimal>get("discountPrice"),
                root.<BigDecimal>get("price")
        );

        if (minPrice != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    sellingPrice, minPrice
            ));
        }

        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(
                    sellingPrice, maxPrice
            ));
        }

        if (inStock) {
            predicates.add(cb.greaterThan(
                    root.<Integer>get("stockQuantity"), 0
            ));
        }

        if (onSale) {
            predicates.add(cb.isNotNull(root.get("discountPrice")));
            predicates.add(cb.greaterThan(
                    root.<BigDecimal>get("discountPrice"),
                    BigDecimal.ZERO
            ));
            predicates.add(cb.lessThan(
                    root.<BigDecimal>get("discountPrice"),
                    root.<BigDecimal>get("price")
            ));
        }

        return predicates.toArray(new Predicate[0]);
    }

    private void validatePrices(
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        if ((minPrice != null && minPrice.signum() < 0)
                || (maxPrice != null && maxPrice.signum() < 0)) {
            throw new IllegalArgumentException(
                    "Khoảng giá không được là số âm."
            );
        }

        if (minPrice != null && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException(
                    "Giá từ phải nhỏ hơn hoặc bằng giá đến."
            );
        }
    }
}