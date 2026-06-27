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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.myweb.shoppe_fake.Model.Product;
import com.myweb.shoppe_fake.Service.CategoryService;
import com.myweb.shoppe_fake.Service.ProductService;

@Controller
@RequestMapping("/admin/products")
public class ProductController {

    @Autowired
    private ProductService productService;

     @Autowired
     private CategoryService categoryService; 

    // --- HIỂN THỊ DANH SÁCH (Tìm kiếm & Phân trang) ---
    @GetMapping
    public String listProducts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            Model model) {
            
        // Chuyển đổi page từ UI (bắt đầu từ 1) sang Spring Data (bắt đầu từ 0)
        int pageIndex = page - 1; 
        Page<Product> productPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            productPage = productService.searchProducts(keyword, pageIndex, size);
        } else {
            productPage = productService.getAllProductsPaginated(pageIndex, size);
        }
        
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        
        return "admin/product/list"; // Tên file HTML danh sách sản phẩm
    }

    // --- THÊM SẢN PHẨM ---
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("product", new Product());
        
        // Truyền danh sách Category để hiển thị dropdown (select box)
        model.addAttribute("categories", categoryService.getAllCategories()); 
        
        return "admin/product/add"; // Tên file HTML form thêm/sửa
    }

    @PostMapping("/save")
    public String saveProduct(@ModelAttribute("product") Product product) {
        productService.saveProduct(product);
        return "redirect:/admin/products";
    }

    // --- SỬA SẢN PHẨM ---
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        Product product = productService.getProductById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + id));
            
        model.addAttribute("product", product);
        
        model.addAttribute("categories", categoryService.getAllCategories());
        
        return "admin/product/edit";
    }

    // --- XÓA SẢN PHẨM ---
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable("id") String id,  RedirectAttributes redirectAttributes) {
       try {
        productService.deleteProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa sản phẩm thành công!");
        return "redirect:/admin/products";

    } catch (Exception e) {
        // Dùng getProductById + Optional
        String productName = productService.getProductById(id)
                .map(p -> p.getName())   // nếu tìm thấy thì lấy tên
                .orElse("Không xác định"); // nếu không tìm thấy

        redirectAttributes.addFlashAttribute("errorMessage","Sản phẩm đang tồn tại trong đơn hàng, không thể xóa.");
        redirectAttributes.addFlashAttribute("productName", productName);
        redirectAttributes.addFlashAttribute("productId", id);
        return "redirect:/admin/products/delete-error";
    }

    }
    @GetMapping("/delete-error")
    public String deleteError() {
         return "admin/product/delete-error"; 
}
}