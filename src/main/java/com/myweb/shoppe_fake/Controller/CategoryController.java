package com.myweb.shoppe_fake.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.myweb.shoppe_fake.Model.Category;
import com.myweb.shoppe_fake.Service.CategoryService;

@Controller
@RequestMapping("/admin/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // 1. Hiển thị danh sách danh mục (Có tìm kiếm & Phân trang) - Tương đương 0.25 điểm
    @GetMapping
    public String listCategories(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            Model model) {

        Page<Category> categoryPage = categoryService.getCategoriesWithPaginationAndSearch(keyword, page, size);

        model.addAttribute("categoryPage", categoryPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", categoryPage.getTotalPages());
        
        // Trả về file giao diện: src/main/resources/templates/admin/category/list.html
        return "admin/category/list";
    }

    // 2. Hiển thị Form Thêm Mới
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/category/add";
    }

    // Xử lý lưu danh mục mới (Thêm danh mục - Tương đương 0.25 điểm)
    @PostMapping("/add")
    public String addCategory(@ModelAttribute Category category, Model model) {
        try {
            categoryService.addCategory(category);
            return "redirect:/admin/categories"; // Thêm thành công thì quay lại trang danh sách
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage()); // Báo lỗi nếu trùng tên
            return "admin/category/add";
        }
    }

    // 3. Hiển thị Form Sửa
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model) {
        Category category = categoryService.getCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục!"));
        model.addAttribute("category", category);
        return "admin/category/edit";
    }

    // Xử lý cập nhật danh mục (Sửa danh mục - Tương đương 0.25 điểm)
    @PostMapping("/edit/{id}")
    public String updateCategory(@PathVariable String id, @ModelAttribute Category categoryDetails, Model model) {
        try {
            categoryService.updateCategory(id, categoryDetails);
            return "redirect:/admin/categories";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "admin/category/edit";
        }
    }

    // 4. Xử lý Xóa danh mục (Xóa danh mục - Tương đương 0.25 điểm)
    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return "redirect:/admin/categories";
    }
}