package com.example.shoptech.controller;
import com.example.shoptech.service.*;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
@Controller @RequestMapping("/admin/products") @PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {
    private final ProductService products;private final CategoryService categories;private final ShopService shops;
    public AdminProductController(ProductService products,CategoryService categories,ShopService shops){this.products=products;this.categories=categories;this.shops=shops;}
    @GetMapping public String list(@RequestParam(defaultValue="") String keyword,@RequestParam(required=false) Long categoryId,@RequestParam(required=false) Long shopId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,Model model){
        model.addAttribute("products",products.searchAdmin(keyword,categoryId,shopId,PageRequest.of(Math.max(0,page),size==20||size==50?size:10,Sort.by("id").descending())));
        model.addAttribute("categories",categories.getAvailableCategories());model.addAttribute("shopOptions",shops.adminOptions());
        model.addAttribute("keyword",keyword);model.addAttribute("categoryId",categoryId);model.addAttribute("shopId",shopId);model.addAttribute("productBase","/admin/products");model.addAttribute("trash",false);return "admin/product/list";
    }
    @GetMapping("/{id}") public String detail(@PathVariable Long id,Model model){model.addAttribute("product",products.getById(id));model.addAttribute("history",products.moderationHistory(id));return "admin/product/review";}
    @PostMapping("/{id}/moderation") public String moderate(Principal user,@PathVariable Long id,@RequestParam boolean blocked,@RequestParam boolean expectedBlocked,@RequestParam String reason,RedirectAttributes flash){
        try{products.moderate(user.getName(),id,blocked,expectedBlocked,reason);flash.addFlashAttribute("successMessage","Đã cập nhật trạng thái giám sát sản phẩm.");}catch(IllegalArgumentException|ResponseStatusException e){flash.addFlashAttribute("errorMessage",e instanceof ResponseStatusException r?r.getReason():e.getMessage());}return "redirect:/admin/products/"+id;
    }
    // Former operational routes explicitly refuse mutations instead of redirecting a POST.
    @GetMapping({"/new","/{id}/edit"}) @ResponseStatus(HttpStatus.FORBIDDEN) public void noAdminEdit(){}
    @PostMapping({"/new","/{id}/edit","/{id}/delete"}) @ResponseStatus(HttpStatus.FORBIDDEN) public void noAdminMutation(){}
}
