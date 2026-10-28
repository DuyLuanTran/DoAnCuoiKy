package com.example.shoptech.controller;
import com.example.shoptech.dto.OrderDto;
import com.example.shoptech.entity.OrderStatus;
import com.example.shoptech.service.AdminOrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller @RequestMapping("/admin/orders") @PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {
    private final AdminOrderService orders;
    public AdminOrderController(AdminOrderService orders) { this.orders=orders; }
    @GetMapping public String list(@RequestParam(defaultValue="") String keyword,@RequestParam(required=false) OrderStatus status,
                                  @RequestParam(defaultValue="0") int page,Model model) {
        model.addAttribute("orders",orders.search(keyword,status,page)); model.addAttribute("keyword",keyword); model.addAttribute("status",status); model.addAttribute("statuses",OrderStatus.values()); return "admin/orders";
    }
    @GetMapping("/{id}") public String detail(@PathVariable Long id,Model model) {
        OrderDto order=orders.get(id); model.addAttribute("order",order); model.addAttribute("shopOrders",orders.groups(id)); return "admin/order-detail";
    }
    @PatchMapping("/{id}/status") @ResponseBody
    public OrderDto update(@PathVariable Long id,@Valid @RequestBody OrderDto.Transition input) { return orders.transition(id,input); }
}
