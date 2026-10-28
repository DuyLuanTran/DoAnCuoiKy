package com.example.shoptech.controller;
import com.example.shoptech.dto.ShopForm;
import com.example.shoptech.entity.*;
import com.example.shoptech.service.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
@Controller @RequestMapping("/shop-registration") @PreAuthorize("isAuthenticated()")
public class ShopRegistrationController {
    private final ShopService shops; private final CategoryService categories; private final UserService users;
    public ShopRegistrationController(ShopService shops,CategoryService categories,UserService users){this.shops=shops;this.categories=categories;this.users=users;}
    @GetMapping public String page(Principal user,Model model){
        Shop shop=shops.own(user.getName()).orElse(null);
        if(shop!=null)model.addAttribute("shopForm",shops.form(shop));
        else{ShopForm form=new ShopForm();users.findByUsername(user.getName()).ifPresent(u->{form.setEmail(u.getEmail());form.setPhone(u.getPhoneNumber());form.setAddress(u.getAddress());});model.addAttribute("shopForm",form);}
        return view(user,model);
    }
    @PostMapping public String submit(Principal user,@Valid @ModelAttribute("shopForm") ShopForm form,BindingResult errors,Model model,RedirectAttributes flash){
        if(errors.hasErrors())return view(user,model);
        try{shops.register(user.getName(),form);flash.addFlashAttribute("success","Đã gửi hồ sơ đăng ký shop. Vui lòng chờ Admin duyệt.");return "redirect:/shop-registration";}
        catch(ResponseStatusException e){errors.reject("shop",e.getReason());return view(user,model);}
    }
    private String view(Principal user,Model model){model.addAttribute("shop",shops.own(user.getName()).orElse(null));model.addAttribute("categories",categories.getAvailableCategories());return "user/shop-registration";}
}
