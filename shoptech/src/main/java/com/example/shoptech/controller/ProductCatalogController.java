package com.example.shoptech.controller;

import com.example.shoptech.entity.Product;
import com.example.shoptech.service.CategoryService;
import com.example.shoptech.service.ProductCatalogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/products")
public class ProductCatalogController {

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model
    ) {
        String normalizedKeyword = keyword.strip();
        int normalizedSize = normalizeSize(size);

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                normalizedSize,
                Sort.by("id").descending()
        );

        Page<Product> products = productCatalogService.search(
                normalizedKeyword,
                categoryId,
                pageable
        );

        // Nếu người dùng nhập số trang vượt phạm vi,
        // đưa về trang cuối đang có dữ liệu.
        if (products.getTotalPages() > 0
                && products.getNumber() >= products.getTotalPages()) {

            pageable = PageRequest.of(
                    products.getTotalPages() - 1,
                    normalizedSize,
                    Sort.by("id").descending()
            );

            products = productCatalogService.search(
                    normalizedKeyword,
                    categoryId,
                    pageable
            );
        }

        model.addAttribute("products", products);
        model.addAttribute(
                "categories",
                categoryService.getAvailableCategories()
        );
        model.addAttribute("keyword", normalizedKeyword);
        model.addAttribute("categoryId", categoryId);

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
        model.addAttribute("keyword", "");
        model.addAttribute(
                "categoryId",
                product.getCategory().getId()
        );

        return "user/product";
    }

    private int normalizeSize(int size) {
        return size == 12 || size == 24 || size == 48
                ? size
                : 12;
    }
}