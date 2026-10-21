package com.example.shoptech.controller;

import com.example.shoptech.dto.CategoryForm;
import com.example.shoptech.service.CategoryService;
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

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // Tạo token trước khi render HTML để cookie được ghi sớm.
    @ModelAttribute
    public void initializeCsrfToken(CsrfToken csrfToken) {
        csrfToken.getToken();
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String keyword,
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
                "categories",
                categoryService.search(normalizedKeyword, pageable)
        );
        model.addAttribute("keyword", normalizedKeyword);

        return "admin/category/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        prepareForm(model, null);

        return "admin/category/form";
    }

    @PostMapping("/new")
    public String create(
            @Valid @ModelAttribute("categoryForm") CategoryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, null);
            return "admin/category/form";
        }

        try {
            categoryService.create(form);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Thêm danh mục thành công!"
            );

            return "redirect:/admin/categories";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject(
                    "category.create.failed",
                    exception.getMessage()
            );
        } catch (DataIntegrityViolationException exception) {
            bindingResult.reject(
                    "category.create.conflict",
                    "Dữ liệu bị trùng hoặc không phù hợp "
                            + "với ràng buộc database. Vui lòng kiểm tra lại."
            );
        }

        prepareForm(model, null);
        return "admin/category/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            model.addAttribute(
                    "categoryForm",
                    categoryService.getFormById(id)
            );
            prepareForm(model, id);

            return "admin/category/form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return "redirect:/admin/categories";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("categoryForm") CategoryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, id);
            return "admin/category/form";
        }

        try {
            categoryService.update(id, form);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cập nhật danh mục thành công!"
            );

            return "redirect:/admin/categories";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject(
                    "category.update.failed",
                    exception.getMessage()
            );
        } catch (DataIntegrityViolationException exception) {
            bindingResult.reject(
                    "category.update.conflict",
                    "Không thể lưu dữ liệu do ràng buộc database. "
                            + "Vui lòng kiểm tra lại."
            );
        }

        prepareForm(model, id);
        return "admin/category/form";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            categoryService.moveToTrash(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã chuyển danh mục vào thùng rác!"
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        } catch (DataIntegrityViolationException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không thể xóa danh mục do dữ liệu liên quan."
            );
        }

        return "redirect:/admin/categories";
    }

    private void prepareForm(Model model, Long id) {
        boolean editing = id != null;

        model.addAttribute("editing", editing);
        model.addAttribute("categoryId", id);
        model.addAttribute(
                "formTitle",
                editing ? "Cập nhật danh mục" : "Thêm danh mục"
        );
        model.addAttribute(
                "formAction",
                editing
                        ? "/admin/categories/" + id + "/edit"
                        : "/admin/categories/new"
        );
    }

    private int normalizeSize(int size) {
        return size == 10 || size == 20 || size == 50
                ? size
                : 10;
    }
}