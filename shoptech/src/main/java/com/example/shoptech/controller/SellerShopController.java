package com.example.shoptech.controller;
import com.example.shoptech.dto.ShopForm;
import com.example.shoptech.entity.Shop;
import com.example.shoptech.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
@Controller @RequestMapping("/seller") @PreAuthorize("hasRole('SELLER')")
public class SellerShopController {
    private final ShopService shops;
    public SellerShopController(ShopService shops){this.shops=shops;}
    @ModelAttribute public void context(Principal user,Model model){model.addAttribute("sellerArea",true);Shop shop=shops.requiredOwn(user.getName());model.addAttribute("shop",shop);model.addAttribute("effectiveRate",shops.effectiveRate(shop));}
    @GetMapping public String dashboard(){return "seller/index";}
    @GetMapping("/shop") public String page(Principal user,Model model){model.addAttribute("shopForm",shops.form(shops.requiredOwn(user.getName())));return "seller/shop";}
    @PostMapping("/shop") public String update(Principal user,@Valid @ModelAttribute("shopForm") ShopForm form,BindingResult errors,RedirectAttributes flash){
        if(errors.hasErrors())return "seller/shop";
        try{shops.updateOwn(user.getName(),form);flash.addFlashAttribute("success","Đã lưu thông tin shop.");return "redirect:/seller/shop";}
        catch(ResponseStatusException e){errors.reject("shop",e.getReason());return "seller/shop";}
    }
}
