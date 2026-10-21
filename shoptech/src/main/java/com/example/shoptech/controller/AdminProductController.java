package com.example.shoptech.controller;

import com.example.shoptech.dto.ProductForm;
import com.example.shoptech.entity.Category;
import com.example.shoptech.entity.Product;
import com.example.shoptech.service.CategoryService;
import com.example.shoptech.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public AdminProductController(
            ProductService productService,
            CategoryService categoryService
    ) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @ModelAttribute
    public void initializeCsrfToken(CsrfToken csrfToken) {
        csrfToken.getToken();
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        String normalizedKeyword = keyword.strip();

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                normalizeSize(size),
                Sort.by("id").descending()
        );

        model.addAttribute(
                "products",
                productService.searchAdmin(
                        normalizedKeyword,
                        categoryId,
                        pageable
                )
        );
        model.addAttribute(
                "categories",
                categoryService.getAvailableCategories()
        );
        model.addAttribute("keyword", normalizedKeyword);
        model.addAttribute("categoryId", categoryId);

        return "admin/product/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("productForm", new ProductForm());
        prepareCreateForm(model);

        return "admin/product/form";
    }

    @PostMapping("/new")
    public String create(
            @Valid @ModelAttribute("productForm") ProductForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepareCreateForm(model);
            return "admin/product/form";
        }

        try {
            productService.create(form);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Thêm sản phẩm thành công!"
            );

            return "redirect:/admin/products";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject(
                    "product.create.invalid",
                    exception.getMessage()
            );
        } catch (DataIntegrityViolationException exception) {
            bindingResult.reject(
                    "product.create.conflict",
                    "Mã sản phẩm hoặc slug bị trùng, "
                            + "hoặc dữ liệu liên quan đã thay đổi. "
                            + "Vui lòng kiểm tra lại."
            );
        } catch (IllegalStateException exception) {
            bindingResult.reject(
                    "product.create.uploadFailed",
                    "Không thể hoàn tất upload ảnh. Vui lòng thử lại."
            );
        }

        prepareCreateForm(model);
        return "admin/product/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            model.addAttribute(
                    "productForm",
                    productService.getFormById(id)
            );
            prepareEditForm(model, id);

            return "admin/product/form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/admin/products";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("productForm") ProductForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        // Kiểm tra sản phẩm vẫn tồn tại trước khi dựng lại form.
        try {
            productService.getById(id);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/admin/products";
        }

        if (bindingResult.hasErrors()) {
            return renderEditForm(model, id, redirectAttributes);
        }

        try {
            productService.update(id, form);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cập nhật sản phẩm thành công!"
            );

            return "redirect:/admin/products";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject(
                    "product.update.invalid",
                    exception.getMessage()
            );
        } catch (DataIntegrityViolationException exception) {
            bindingResult.reject(
                    "product.update.conflict",
                    "Không thể lưu do mã sản phẩm bị trùng "
                            + "hoặc dữ liệu liên quan đã thay đổi."
            );
        } catch (IllegalStateException exception) {
            bindingResult.reject(
                    "product.update.uploadFailed",
                    "Không thể hoàn tất upload ảnh. Vui lòng thử lại."
            );
        }

        return renderEditForm(model, id, redirectAttributes);
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            productService.moveToTrash(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã chuyển sản phẩm vào thùng rác!"
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        } catch (DataIntegrityViolationException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không thể xóa sản phẩm do ràng buộc dữ liệu."
            );
        }

        return "redirect:/admin/products";
    }

    private void prepareCreateForm(Model model) {
        model.addAttribute("editing", false);
        model.addAttribute("formTitle", "Thêm sản phẩm");
        model.addAttribute("formAction", "/admin/products/new");
        model.addAttribute(
                "categories",
                categoryService.getAvailableCategories()
        );
    }

    private void prepareEditForm(Model model, Long id) {
        Product product = productService.getById(id);

        List<Category> categories = new ArrayList<>(
                categoryService.getAvailableCategories()
        );

        Category currentCategory = product.getCategory();

        // Cho phép giữ danh mục hiện tại nếu nó đang ẩn.
        boolean containsCurrentCategory = categories.stream()
                .anyMatch(category ->
                        category.getId().equals(currentCategory.getId())
                );

        if (!Boolean.TRUE.equals(currentCategory.getIsDeleted())
                && !containsCurrentCategory) {
            categories.add(currentCategory);
        }

        model.addAttribute("editing", true);
        model.addAttribute("productId", id);
        model.addAttribute("formTitle", "Cập nhật sản phẩm");
        model.addAttribute(
                "formAction",
                "/admin/products/" + id + "/edit"
        );
        model.addAttribute("categories", categories);
        model.addAttribute("currentImage", product.getMainImage());
    }

    private String renderEditForm(
            Model model,
            Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            prepareEditForm(model, id);
            return "admin/product/form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/admin/products";
        }
    }

    private int normalizeSize(int size) {
        return size == 10 || size == 20 || size == 50
                ? size
                : 10;
    }
}