package com.example.shoptech.controller;
import com.example.shoptech.dto.OrderDto;
import com.example.shoptech.service.OrderService;
import com.example.shoptech.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
@Controller @PreAuthorize("isAuthenticated()")
public class OrderController {
    private final OrderService orders;
    private final CategoryService categories;
    public OrderController(OrderService orders, CategoryService categories) { this.orders=orders; this.categories=categories; }
    @GetMapping("/checkout") public String checkout(Model model) {
        model.addAttribute("categories", categories.getAvailableCategories());
        return "user/checkout";
    }
    @PostMapping("/api/orders") @ResponseBody
    public OrderDto place(Principal user,@Valid @RequestBody OrderDto.Checkout input) { return orders.checkout(user.getName(),input); }
    @GetMapping("/orders") public String history(Principal user,@RequestParam(defaultValue="0") int page,Model model) {
        model.addAttribute("categories",categories.getAvailableCategories());
        model.addAttribute("orders",orders.history(user.getName(),page)); return "user/orders";
    }
    @GetMapping("/orders/{id}") public String detail(Principal user,@PathVariable Long id,Model model) {
        model.addAttribute("order",orders.owned(user.getName(),id));
        model.addAttribute("shopOrders",orders.ownedShops(user.getName(),id));
        model.addAttribute("categories", categories.getAvailableCategories());
        return "user/order-detail";
    }
}
