package com.myweb.shoppe_fake.Controller;

import com.myweb.shoppe_fake.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin") // Đường dẫn gốc của Admin
public class AdminDashboardController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public String dashboard(Model model) {
        // 1. Thống kê tổng quan
        model.addAttribute("totalRevenue", orderService.getTotalRevenue());
        model.addAttribute("totalCompletedOrders", orderService.countCompletedOrders());
        
        // 2. Dữ liệu cho biểu đồ (Mảng 12 phần tử)
        List<Double> monthlyRevenue = orderService.getMonthlyRevenueForCurrentYear();
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        
        return "admin/admin"; // Trỏ đến file dashboard.html
    }
}