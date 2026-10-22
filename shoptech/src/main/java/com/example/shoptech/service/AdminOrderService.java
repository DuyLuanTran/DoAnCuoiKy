package com.example.shoptech.service;
import com.example.shoptech.dto.OrderDto;
import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.example.shoptech.service.CartService.error;

@Service @Transactional(readOnly = true) @PreAuthorize("hasRole('ADMIN')")
public class AdminOrderService {
    private final OrderRepository orders; private final ProductRepository products;
    private final org.springframework.context.ApplicationEventPublisher events;
    public AdminOrderService(OrderRepository orders, ProductRepository products, org.springframework.context.ApplicationEventPublisher events) { this.orders=orders; this.products=products; this.events=events; }
    public Page<OrderDto.Summary> search(String keyword, OrderStatus status, int page) {
        if (keyword.length() > 100) throw error(HttpStatus.BAD_REQUEST,"Từ khóa quá dài.");
        return orders.searchAdmin(keyword.strip(),status,PageRequest.of(Math.max(0,page),10,Sort.by("createdAt").descending().and(Sort.by("id").descending()))).map(OrderService::summary);
    }
    public OrderDto get(Long id) { return OrderService.detail(orders.findByIdAndIsDeletedFalse(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Không tìm thấy đơn hàng."))); }
    @Transactional
    public OrderDto transition(Long id, OrderDto.Transition input) {
        Order order=orders.lockActiveById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Không tìm thấy đơn hàng."));
        if (order.getOrderStatus() != input.expectedStatus()) throw error(HttpStatus.CONFLICT,"Đơn đã được cập nhật. Vui lòng tải lại.");
        if (!allowed(order.getOrderStatus()).contains(input.status())) throw error(HttpStatus.CONFLICT,"Chuyển trạng thái không hợp lệ.");
        if (input.status() == OrderStatus.CANCELLED) {
            if (!"COD".equals(order.getPaymentMethod()) || !"UNPAID".equals(order.getPaymentStatus()))
                throw error(HttpStatus.CONFLICT,"Không thể hủy đơn đã thanh toán hoặc thanh toán trực tuyến.");
            for (OrderDetail line : order.getDetails().stream().sorted(Comparator.comparing(OrderDetail::getProductId)).toList()) {
                Product product=products.lockById(line.getProductId()).orElseThrow(() -> error(HttpStatus.CONFLICT,"Không thể hoàn kho sản phẩm."));
                long restored=(long)product.getStockQuantity()+line.getQuantity();
                if (restored > Integer.MAX_VALUE) throw error(HttpStatus.CONFLICT,"Tồn kho vượt giới hạn.");
                product.setStockQuantity((int)restored);
            }
        }
        if (input.status() == OrderStatus.COMPLETED && "COD".equals(order.getPaymentMethod())) order.setPaymentStatus("PAID");
        order.setOrderStatus(input.status()); orders.flush();
        events.publishEvent(new OrderStatusChanged(order.getUser().getUsername(),new com.example.shoptech.dto.OrderStatusNotification(order.getId(),order.getOrderCode(),order.getOrderStatus())));
        return OrderService.detail(order);
    }
    public static Set<OrderStatus> allowed(OrderStatus status) {
        return switch(status) {
            case PENDING -> Set.of(OrderStatus.CONFIRMED,OrderStatus.CANCELLED);
            case CONFIRMED -> Set.of(OrderStatus.SHIPPING,OrderStatus.CANCELLED);
            case SHIPPING -> Set.of(OrderStatus.COMPLETED);
            default -> Set.of();
        };
    }
}
