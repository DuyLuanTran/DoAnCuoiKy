package com.example.shoptech.controller;
import com.example.shoptech.dto.OrderDto;
import com.example.shoptech.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
@Controller @PreAuthorize("isAuthenticated()")
public class OrderController {
    private final OrderService orders;
    public OrderController(OrderService orders) { this.orders=orders; }
    @GetMapping("/checkout") public String checkout() { return "user/checkout"; }
    @PostMapping("/api/orders") @ResponseBody
    public OrderDto place(Principal user,@Valid @RequestBody OrderDto.Checkout input) { return orders.checkout(user.getName(),input); }
    @GetMapping("/orders") public String history(Principal user,@RequestParam(defaultValue="0") int page,Model model) {
        model.addAttribute("orders",orders.history(user.getName(),page)); return "user/orders";
    }
    @GetMapping("/orders/{id}") public String detail(Principal user,@PathVariable Long id,Model model) {
        model.addAttribute("order",orders.owned(user.getName(),id)); return "user/order-detail";
    }
}
