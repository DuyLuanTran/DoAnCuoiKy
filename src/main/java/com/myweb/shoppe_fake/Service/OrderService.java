package com.myweb.shoppe_fake.Service;

import com.myweb.shoppe_fake.Model.Order;
import com.myweb.shoppe_fake.Model.OrderDetail;
import com.myweb.shoppe_fake.Model.Product;
import com.myweb.shoppe_fake.Repository.OrderRepository;
import com.myweb.shoppe_fake.Repository.ProductRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;

public Order createOrder(Order order) {
        // Kiểm tra xem đơn hàng có sản phẩm nào không
        if (order.getDetails() != null && !order.getDetails().isEmpty()) {
            
        
            for (OrderDetail detail : order.getDetails()) {
                
                // Lấy Product ra bằng 'getProductId()'
                Product product = productRepository.findById(detail.getProductId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong hệ thống"));

                // 1. Kiểm tra trạng thái hoạt động 
                // (Giả sử trong Product bạn dùng biến boolean isActive)
                if (!product.isActive()) {
                    throw new RuntimeException("LỖI: Sản phẩm '" + product.getName() + "' hiện đang ngừng kinh doanh!");
                }

                // 2. Kiểm tra tồn kho
                // (Giả sử trong Product bạn dùng biến số lượng là 'quantity', nếu bạn dùng 'stock' thì đổi lại nhé)
                if (product.getQuantity() <= 0) {
                    throw new RuntimeException("LỖI: Sản phẩm '" + product.getName() + "' đã hết hàng!");
                }
                
                if (product.getQuantity() < detail.getQuantity()) {
                    throw new RuntimeException("LỖI: Sản phẩm '" + product.getName() + 
                        "' chỉ còn " + product.getQuantity() + " cái, không đủ để đặt!");
                }
            }
        }

        order.setStatus("PENDING");
        return orderRepository.save(order);
    }
    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }

    public Optional<Order> getOrderById(String orderId) {
        return orderRepository.findById(orderId);
    }

    public List<Order> getAllOrders() { return orderRepository.findAll(); }

    public Order updateOrderStatus(String orderId, String status) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        order.setStatus(status);
        return orderRepository.save(order);
    }
    public Page<Order> getOrdersWithPagination(String keyword, int page, int size) {
        // PageRequest.of(page - 1) vì Spring Data tính page từ 0
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("orderDate").descending());
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            return orderRepository.findByPhoneContainingIgnoreCaseOrStatusContainingIgnoreCase(keyword, keyword, pageable);
        }
        return orderRepository.findAll(pageable);
    }
    public double getTotalRevenue() {
        List<Order> completedOrders = orderRepository.findByStatus("COMPLETED");
        double total = 0;
        for (Order order : completedOrders) {
            total += order.getTotalAmount();
        }
        return total;
    }

    // 2. Đếm số đơn thành công
    public long countCompletedOrders() {
        return orderRepository.findByStatus("COMPLETED").size();
    }

    // 3. Tính doanh thu 12 tháng của năm hiện tại (Biểu đồ - 0.25đ)
    public List<Double> getMonthlyRevenueForCurrentYear() {
        Double[] monthlyRevenue = new Double[12];
        Arrays.fill(monthlyRevenue, 0.0); // Khởi tạo mảng 12 tháng = 0
        
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        
        List<Order> completedOrders = orderRepository.findByStatus("COMPLETED");
        
        for (Order order : completedOrders) {
            cal.setTime(order.getOrderDate());
            if (cal.get(Calendar.YEAR) == currentYear) {
                int month = cal.get(Calendar.MONTH); // Trả về từ 0 (Tháng 1) đến 11 (Tháng 12)
                monthlyRevenue[month] += order.getTotalAmount();
            }
        }
        return Arrays.asList(monthlyRevenue);
    }
}