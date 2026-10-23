package com.example.shoptech.controller;

import com.example.shoptech.entity.Product;
import com.example.shoptech.service.CategoryService;
import com.example.shoptech.service.ProductCatalogService;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Set;

@Controller
@RequestMapping("/products")
public class ProductCatalogController {

    private static final Set<String> SORT_OPTIONS = Set.of(
            "newest", "priceAsc", "priceDesc", "nameAsc"
    );

    private final ProductCatalogService productCatalogService;
    private final CategoryService categoryService;

    public ProductCatalogController(
            ProductCatalogService productCatalogService,
            CategoryService categoryService
    ) {
        this.productCatalogService = productCatalogService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "false") boolean inStock,
            @RequestParam(defaultValue = "false") boolean onSale,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model
    ) {
        String normalizedKeyword = keyword.strip();
        String normalizedSort = SORT_OPTIONS.contains(sort)
                ? sort
                : "newest";

        int normalizedSize = normalizeSize(size);

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                normalizedSize
        );

        Page<Product> products;

        try {
            products = productCatalogService.search(
                    normalizedKeyword,
                    categoryId,
                    minPrice,
                    maxPrice,
                    inStock,
                    onSale,
                    normalizedSort,
                    pageable
            );
        } catch (IllegalArgumentException exception) {
            model.addAttribute("filterError", exception.getMessage());

            products = Page.empty(
                    PageRequest.of(0, normalizedSize)
            );
        }

        model.addAttribute("products", products);
        model.addAttribute(
                "categories",
                categoryService.getAvailableCategories()
        );

        addFilterAttributes(
                model, normalizedKeyword, categoryId,
                minPrice, maxPrice, inStock, onSale,
                normalizedSort, normalizedSize
        );

        return "user/store";
    }

    @GetMapping("/{slug}")
    public String detail(
            @PathVariable String slug,
            Model model
    ) {
        Product product = productCatalogService.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm."
                ));

        model.addAttribute("product", product);
        model.addAttribute(
                "categories",
                categoryService.getAvailableCategories()
        );

        // Header của trang chi tiết đang dùng lại từ store.html.
        addFilterAttributes(
                model, "", product.getCategory().getId(),
                null, null, false, false, "newest", 12
        );

        return "user/product";
    }

    private void addFilterAttributes(
            Model model,
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean inStock,
            boolean onSale,
            String sort,
            int pageSize
    ) {
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("inStock", inStock);
        model.addAttribute("onSale", onSale);
        model.addAttribute("sort", sort);
        model.addAttribute("pageSize", pageSize);
    }

    private int normalizeSize(int size) {
        return size == 12 || size == 24 || size == 48
                ? size
                : 12;
    }
}