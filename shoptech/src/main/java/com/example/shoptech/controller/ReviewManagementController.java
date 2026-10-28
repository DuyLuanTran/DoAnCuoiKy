package com.example.shoptech.controller;

import com.example.shoptech.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@Controller
public class ReviewManagementController {
    private final ProductCommunityService community;private final ShopService shops;
    public ReviewManagementController(ProductCommunityService community,ShopService shops){this.community=community;this.shops=shops;}
    @GetMapping("/seller/reviews") @PreAuthorize("hasRole('SELLER')")
    public String seller(Principal user,@RequestParam(defaultValue="0")int page,Model model){model.addAttribute("sellerArea",true);model.addAttribute("shop",shops.requiredOwn(user.getName()));model.addAttribute("reviews",community.manage(user.getName(),true,page));return "admin/reviews";}
    @GetMapping("/admin/reviews") @PreAuthorize("hasRole('ADMIN')")
    public String admin(Principal user,@RequestParam(defaultValue="0")int page,Model model){model.addAttribute("reviews",community.manage(user.getName(),false,page));return "admin/reviews";}
    public record Reply(@Min(0)long version,@NotBlank @Size(max=2000)String reply){}
    public record Moderation(@Min(0)long version,boolean hidden,@NotBlank @Size(min=5,max=1000)String reason){}
    @PatchMapping("/seller/reviews/{id}/reply") @PreAuthorize("hasRole('SELLER')") @ResponseBody
    public void reply(Principal user,@PathVariable Long id,@Valid @RequestBody Reply input){community.reply(user.getName(),id,input.version(),input.reply());}
    @PatchMapping("/admin/reviews/{id}/moderation") @PreAuthorize("hasRole('ADMIN')") @ResponseBody
    public void moderate(Principal user,@PathVariable Long id,@Valid @RequestBody Moderation input){community.moderate(user.getName(),id,input.version(),input.hidden(),input.reason());}
}
