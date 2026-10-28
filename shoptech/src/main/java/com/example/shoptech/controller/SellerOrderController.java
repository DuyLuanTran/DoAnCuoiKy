package com.example.shoptech.controller;
import com.example.shoptech.dto.OrderDto;
import com.example.shoptech.entity.OrderStatus;
import com.example.shoptech.service.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
@Controller @RequestMapping("/seller/orders") @PreAuthorize("hasRole('SELLER')")
public class SellerOrderController {
    private final SellerOrderService orders;private final ShopService shops;
    public SellerOrderController(SellerOrderService orders,ShopService shops){this.orders=orders;this.shops=shops;}
    @ModelAttribute void area(Principal user,Model model){model.addAttribute("sellerArea",true);model.addAttribute("shop",shops.requiredOwn(user.getName()));}
    @GetMapping public String list(Principal user,@RequestParam(defaultValue="")String keyword,@RequestParam(required=false)OrderStatus status,@RequestParam(defaultValue="0")int page,Model model){
        model.addAttribute("orders",orders.search(user.getName(),keyword,status,page));model.addAttribute("keyword",keyword);model.addAttribute("status",status);model.addAttribute("statuses",OrderStatus.values());return "seller/orders";
    }
    @GetMapping("/{id}") public String detail(Principal user,@PathVariable Long id,Model model){
        model.addAttribute("order",orders.detail(user.getName(),id));model.addAttribute("shipment",SellerOrderService.summary(orders.get(user.getName(),id)));model.addAttribute("needsReconciliation",orders.needsReconciliation(orders.get(user.getName(),id)));model.addAttribute("nextStatuses",orders.needsReconciliation(orders.get(user.getName(),id))?java.util.Set.of():SellerOrderService.allowed(orders.get(user.getName(),id).getStatus()));model.addAttribute("history",orders.history(user.getName(),id));return "seller/order-detail";
    }
    public record Change(@jakarta.validation.constraints.NotNull OrderStatus expectedStatus,@jakarta.validation.constraints.NotNull OrderStatus status,@jakarta.validation.constraints.Size(max=1000)String reason){}
    @PatchMapping("/{id}/status") @ResponseBody public OrderDto update(Principal user,@PathVariable Long id,@Valid @RequestBody Change input){return orders.transition(user.getName(),id,new OrderDto.Transition(input.expectedStatus(),input.status()),input.reason());}
}
