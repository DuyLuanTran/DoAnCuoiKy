package com.example.shoptech.controller;
import com.example.shoptech.dto.CartDto;
import com.example.shoptech.service.CartService;
import com.example.shoptech.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@Controller
@PreAuthorize("isAuthenticated()")
public class CartController {
    private final CartService carts;
    private final CategoryService categories;
    public CartController(CartService carts, CategoryService categories) { this.carts = carts; this.categories = categories; }
    @GetMapping("/cart") public String page(Model model) {
        model.addAttribute("categories", categories.getAvailableCategories());
        return "user/cart";
    }
    @GetMapping("/api/cart") @ResponseBody
    public CartDto get(Principal user) { return carts.get(user.getName()); }
    @PostMapping("/api/cart/items") @ResponseBody @ResponseStatus(HttpStatus.CREATED)
    public CartDto add(Principal user, @Valid @RequestBody CartDto.Add input) { return carts.add(user.getName(), input.productId(), input.quantity().intValueExact()); }
    @PatchMapping("/api/cart/items/{id}") @ResponseBody
    public CartDto update(Principal user, @PathVariable Long id, @Valid @RequestBody CartDto.Quantity input) { return carts.update(user.getName(), id, input.quantity().intValueExact()); }
    @DeleteMapping("/api/cart/items/{id}") @ResponseBody
    public CartDto remove(Principal user, @PathVariable Long id) { return carts.remove(user.getName(), id); }
}
