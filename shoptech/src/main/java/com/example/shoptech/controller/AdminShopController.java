package com.example.shoptech.controller;
import com.example.shoptech.entity.*;
import com.example.shoptech.service.ShopService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.math.BigDecimal;
@Controller @RequestMapping("/admin/shops") @PreAuthorize("hasRole('ADMIN')")
public class AdminShopController {
    private final ShopService shops;
    public AdminShopController(ShopService shops){this.shops=shops;}
    @GetMapping public String list(@RequestParam(defaultValue="") String keyword,@RequestParam(required=false) ShopStatus status,@RequestParam(defaultValue="0") int page,Model model){
        model.addAttribute("shops",shops.search(keyword,status,page));model.addAttribute("keyword",keyword);model.addAttribute("status",status);model.addAttribute("stats",shops.statistics());model.addAttribute("defaultRate",shops.defaultRate());return "admin/shop/list";
    }
    @GetMapping("/{id}") public String detail(@PathVariable Long id,Model model){Shop s=shops.get(id);model.addAttribute("shop",s);model.addAttribute("history",shops.history(id));model.addAttribute("defaultRate",shops.defaultRate());model.addAttribute("effectiveRate",shops.effectiveRate(s));return "admin/shop/detail";}
    @PostMapping("/commission") public String defaultRate(Principal user,@RequestParam BigDecimal rate,RedirectAttributes flash){try{shops.setDefaultRate(user.getName(),rate);flash.addFlashAttribute("success","Đã lưu mức chiết khấu mặc định.");}catch(ResponseStatusException e){flash.addFlashAttribute("error",e.getReason());}return "redirect:/admin/shops";}
    @PostMapping("/{id}/commission") public String shopRate(Principal user,@PathVariable Long id,@RequestParam(required=false) BigDecimal rate,RedirectAttributes flash){try{shops.setShopRate(user.getName(),id,rate);flash.addFlashAttribute("success","Đã cập nhật chiết khấu của shop.");}catch(ResponseStatusException e){flash.addFlashAttribute("error",e.getReason());}return "redirect:/admin/shops/"+id;}
    @PostMapping("/{id}/{action}") public String review(Principal user,@PathVariable Long id,@PathVariable String action,@RequestParam ShopStatus expectedStatus,@RequestParam(defaultValue="") String reason,RedirectAttributes flash){
        try{shops.review(user.getName(),id,action,expectedStatus,reason);flash.addFlashAttribute("success","Đã cập nhật trạng thái shop.");}catch(ResponseStatusException e){flash.addFlashAttribute("error",e.getReason());}return "redirect:/admin/shops/"+id;
    }
}
