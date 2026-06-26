package com.myweb.shoppe_fake.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.myweb.shoppe_fake.DTO.CartItemDTO;
import com.myweb.shoppe_fake.Model.Cart;
import com.myweb.shoppe_fake.Model.CartItem;
import com.myweb.shoppe_fake.Model.Product;
import com.myweb.shoppe_fake.Repository.ProductRepository;
import com.myweb.shoppe_fake.Service.CartService;

import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository; 

    private String getLoggedInUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || 
        auth.getName().equals("anonymousUser")) {
        return null; // chưa đăng nhập
    }
    return auth.getName();
}

    // API: Xem giỏ hàng
    @GetMapping
    public String viewCart(Model model) {
    String username = getLoggedInUsername();
    if (username == null) return "redirect:/login"; // ← chưa đăng nhập thì về login
    
    Cart cart = cartService.getCartByUserId(username);
        List<CartItemDTO> cartItemDTOs = new ArrayList<>();
        double totalCartPrice = 0;

        for (CartItem item : cart.getItems()) {
            Optional<Product> productOpt = productRepository.findById(item.getProductId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                CartItemDTO dto = new CartItemDTO();
                dto.setProductId(product.getId());
                dto.setProductName(product.getName());
                dto.setImage(product.getImage());
                dto.setPrice(product.getPrice());
                dto.setQuantity(item.getQuantity());
                
                // Tính thành tiền
                dto.setSubtotal(product.getPrice() * item.getQuantity());
                
                cartItemDTOs.add(dto);
                totalCartPrice += dto.getSubtotal();
            }
        }

        model.addAttribute("cartItems", cartItemDTOs);
        model.addAttribute("totalCartPrice", totalCartPrice);
        return "user/cart"; 
    }

    // API: Thêm vào giỏ
    @PostMapping("/add")
    public String addToCart(@RequestParam String productId,
                        @RequestParam(defaultValue = "1") int quantity) {
    String username = getLoggedInUsername();
    if (username == null) return "redirect:/login";
    
    cartService.addToCart(username, productId, quantity);
    return "redirect:/cart";
}
    // API: Xóa khỏi giỏ
    @GetMapping("/remove")
    public String removeFromCart(@RequestParam String productId) {
    String username = getLoggedInUsername();
    if (username == null) return "redirect:/login";
    
    cartService.removeFromCart(username, productId);
    return "redirect:/cart";
}
}