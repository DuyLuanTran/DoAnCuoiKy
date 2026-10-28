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
    public List<com.example.shoptech.dto.ShopOrderDto> groups(Long id){return orders.findByIdAndIsDeletedFalse(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Không tìm thấy đơn hàng.")).getShopOrders().stream().map(SellerOrderService::summary).toList();}
    public OrderDto transition(Long id, OrderDto.Transition input) {
        throw error(HttpStatus.FORBIDDEN,"Seller của shop quản lý trạng thái đơn hàng.");
    }
}
