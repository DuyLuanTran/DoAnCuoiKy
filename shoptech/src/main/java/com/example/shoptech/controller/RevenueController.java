package com.example.shoptech.controller;
import com.example.shoptech.service.RevenueService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@Controller @RequestMapping("/admin/revenue") @PreAuthorize("hasRole('ADMIN')")
public class RevenueController {
    private final RevenueService revenue;
    public RevenueController(RevenueService revenue) { this.revenue=revenue; }
    @GetMapping public String page(Model model) {
        LocalDate today=LocalDate.now(RevenueService.ZONE); model.addAttribute("from",today.minusDays(29)); model.addAttribute("to",today); return "admin/revenue";
    }
    @GetMapping("/data") @ResponseBody
    public RevenueService.Report data(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(defaultValue="DAY") RevenueService.Group group) { return revenue.report(from,to,group); }
}
