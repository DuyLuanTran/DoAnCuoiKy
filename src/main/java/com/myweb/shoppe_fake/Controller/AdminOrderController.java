package com.myweb.shoppe_fake.Controller;

import com.myweb.shoppe_fake.Model.Order;
import com.myweb.shoppe_fake.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/orders") // Định tuyến riêng cho Admin
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    // 1. Hiển thị danh sách đơn hàng toàn hệ thống (Tìm kiếm + Phân trang)
    @GetMapping
    public String listOrders(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model) {

        Page<Order> orderPage = orderService.getOrdersWithPagination(keyword, page, size);

        model.addAttribute("orderPage", orderPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "admin/order/list"; // Trỏ tới file giao diện Dashmin của bạn
    }

    // 2. Xem chi tiết một đơn hàng bất kỳ
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable String id, Model model) {
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + id));
        model.addAttribute("order", order);
        return "admin/order/detail";
    }

    // 3. Admin cập nhật trạng thái đơn hàng (PENDING -> SHIPPING -> COMPLETED)
    @PostMapping("/{id}/update-status")
    public String updateStatus(@PathVariable String id, @RequestParam("status") String status) {
        orderService.updateOrderStatus(id, status);
        return "redirect:/admin/orders/" + id;
    }
}