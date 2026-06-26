package com.myweb.shoppe_fake.Controller;

import com.myweb.shoppe_fake.Model.Product;
import com.myweb.shoppe_fake.Service.CartService;
import com.myweb.shoppe_fake.Service.ProductService;
// Import thêm các class cần thiết cho giỏ hàng
// import com.myweb.shoppe_fake.Service.CartService; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    // TODO: Bạn cần tiêm (inject) CartService vào đây để đếm số lượng giỏ hàng thực tế
     @Autowired
     private CartService cartService;

    @GetMapping("/")
    public String index(
            @RequestParam(value = "search", defaultValue = "") String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "8") int size,
            Principal principal, // THÊM DÒNG NÀY: Để lấy thông tin người dùng đang đăng nhập
            Model model) {
        
        Page<Product> productsPage;
        
        // Nếu có từ khóa tìm kiếm thì gọi searchProducts, không thì gọi getAllProductsPaginated
        if (search != null && !search.trim().isEmpty()) {
            productsPage = productService.searchProducts(search.trim(), page, size);
            model.addAttribute("search", search);
        } else {
            productsPage = productService.getAllProductsPaginated(page, size);
            model.addAttribute("search", "");
        }
        
        // Tính toán danh sách page numbers để hiển thị trong pagination
        List<Integer> pageNumbers = new ArrayList<>();
        int totalPages = productsPage.getTotalPages();
        
        // Hiển thị tối đa 5 page numbers
        int startPage = Math.max(0, page - 2);
        int endPage = Math.min(totalPages - 1, page + 2);
        
        for (int i = startPage; i <= endPage; i++) {
            pageNumbers.add(i);
        }

            // Pass data về template
        model.addAttribute("products", productsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageNumbers", pageNumbers);
        model.addAttribute("hasPreviousPage", page > 0);
        model.addAttribute("hasNextPage", page < totalPages - 1);
        model.addAttribute("totalElements", productsPage.getTotalElements());
        
        return "user/index";
    }
    
    @GetMapping("/product/{id}")
    public String productDetail(
            @PathVariable("id") String id, 
            Principal principal, // THÊM VÀO ĐÂY NỮA (Để vào trang chi tiết icon giỏ hàng không bị reset về 0)
            Model model) {
        
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm!"));
        
        // XỬ LÝ LẤY SỐ LƯỢNG GIỎ HÀNG (Làm tương tự trang chủ)
        int cartCount = 0;
        if (principal != null) {
            String username = principal.getName();
            // cartCount = cartService.getCartItemCount(username);
            cartCount = 5; // Để tạm số 5 để test
        }
        model.addAttribute("cartCount", cartCount);
        
        model.addAttribute("product", product);
        return "user/detail";
    }
}