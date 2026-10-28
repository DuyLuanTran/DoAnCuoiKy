package com.example.shoptech.service;

import com.example.shoptech.entity.*;
import com.example.shoptech.repository.OrderNotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;

import static com.example.shoptech.service.CartService.error;

@Service
public class StoredOrderNotificationService {
    public record Item(Long id, String message, String url, OffsetDateTime createdAt, boolean read) {}
    public record Inbox(long unread, List<Item> items, boolean hasMore) {}
    private final OrderNotificationRepository notifications;
    public StoredOrderNotificationService(OrderNotificationRepository notifications) { this.notifications = notifications; }

    @Transactional
    public void placed(Order order) {
        save(order.getUser(), order, null, "BUYER", "Đặt hàng thành công. Mã đơn " + order.getOrderCode() + ".");
        for (ShopOrder shopOrder : order.getShopOrders()) {
            save(shopOrder.getShop().getOwner(), order, shopOrder, "SELLER",
                    "Shop có đơn hàng mới " + order.getOrderCode() + ".");
        }
    }

    @Transactional
    public void changed(ShopOrder shopOrder) {
        Order order = shopOrder.getOrder();
        String shop = shopOrder.getShopName();
        OrderStatus status = shopOrder.getStatus();
        save(order.getUser(), order, shopOrder, "BUYER", "Đơn của shop " + shop + " " + buyerText(status) + ".");
        if (status == OrderStatus.RETURN_REQUESTED) {
            save(shopOrder.getShop().getOwner(), order, shopOrder, "SELLER",
                    "Shop có yêu cầu trả hàng cho đơn " + order.getOrderCode() + ".");
        }
    }

    private String buyerText(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> "đã được xác nhận";
            case PICKED_UP -> "đã được lấy hàng";
            case SHIPPING -> "đang được giao";
            case COMPLETED -> "đã giao thành công";
            case CANCELLED -> "đã bị hủy";
            case RETURN_REQUESTED -> "đã có yêu cầu trả hàng";
            case REFUNDED -> "đã được hoàn tiền";
            case PENDING -> "đang chờ xác nhận";
        };
    }

    private void save(User recipient, Order order, ShopOrder shopOrder, String audience, String message) {
        OrderNotification notification = new OrderNotification();
        notification.setRecipient(recipient); notification.setOrder(order); notification.setShopOrder(shopOrder);
        notification.setAudience(audience); notification.setMessage(message);
        notifications.save(notification);
    }

    @Transactional(readOnly = true)
    public Inbox inbox(String username, int page) {
        var result = notifications.findByRecipientUsernameOrderByCreatedAtDescIdDesc(username, PageRequest.of(Math.max(0, page), 30));
        List<Item> items = result
                .map(n -> new Item(n.getId(), n.getMessage(), "/notifications/" + n.getId() + "/open",
                        n.getCreatedAt(), n.getReadAt() != null)).getContent();
        return new Inbox(notifications.countByRecipientUsernameAndReadAtIsNull(username), items, result.hasNext());
    }

    @Transactional
    public String open(String username, Long id) {
        OrderNotification notification = notifications.findByIdAndRecipientUsername(id, username)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo."));
        if (notification.getReadAt() == null) notification.setReadAt(OffsetDateTime.now());
        if ("SELLER".equals(notification.getAudience())) {
            return "/seller/orders/" + notification.getShopOrder().getId();
        }
        return "/orders/" + notification.getOrder().getId();
    }
}
