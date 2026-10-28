package com.example.shoptech.controller;
import com.example.shoptech.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.LocalDate;
@Controller @RequestMapping("/seller/revenue") @PreAuthorize("hasRole('SELLER')")
public class SellerRevenueController {
    private final RevenueService revenue;private final ShopService shops;
    public SellerRevenueController(RevenueService revenue,ShopService shops){this.revenue=revenue;this.shops=shops;}
    @GetMapping public String page(Principal user,Model model){LocalDate today=LocalDate.now(RevenueService.ZONE);model.addAttribute("from",today.minusDays(29));model.addAttribute("to",today);model.addAttribute("sellerArea",true);model.addAttribute("shop",shops.requiredOwn(user.getName()));return "admin/revenue";}
    @GetMapping("/data") @ResponseBody public RevenueService.Report data(Principal user,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to,@RequestParam(defaultValue="DAY")RevenueService.Group group){return revenue.sellerReport(user.getName(),from,to,group);}
}
