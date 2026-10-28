package com.example.shoptech.service;

import com.example.shoptech.dto.OrderDto;
import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.*;
import static com.example.shoptech.service.CartService.error;

@Service @Transactional(readOnly = true)
public class OrderService {
    private final UserRepository users; private final CartRepository carts;
    private final CartItemRepository items; private final ProductRepository products;
    private final OrderRepository orders;
    private final ShopService shops;
    private final StoredOrderNotificationService notifications;
    private final ShopOrderHistoryRepository shopHistory;
    private final ShopOrderRepository shopOrders;
    private final ApplicationEventPublisher events;
    public OrderService(UserRepository users, CartRepository carts, CartItemRepository items, ProductRepository products, OrderRepository orders,ShopService shops,StoredOrderNotificationService notifications,ShopOrderHistoryRepository shopHistory,ShopOrderRepository shopOrders,ApplicationEventPublisher events) {
        this.users=users; this.carts=carts; this.items=items; this.products=products; this.orders=orders;this.shops=shops;this.notifications=notifications;this.shopHistory=shopHistory;this.shopOrders=shopOrders;this.events=events;
    }
    @Transactional
    public OrderDto checkout(String username, OrderDto.Checkout input) {
        User user = users.lockActiveByUsername(username).orElseThrow(() -> error(HttpStatus.UNAUTHORIZED,"Vui lòng đăng nhập lại."));
        String code = orderCode(input.requestKey());
        // The unique order code retains the full UUID and acts as a durable idempotency key.
        Optional<Order> previous = orders.findByOrderCodeAndUserIdAndIsDeletedFalse(code,user.getId());
        if (previous.isPresent()) return detail(previous.get());
        if (!"COD".equals(input.paymentMethod())) throw error(HttpStatus.BAD_REQUEST,"Chỉ hỗ trợ COD.");
        Cart cart = carts.findByUserId(user.getId()).orElseThrow(() -> error(HttpStatus.CONFLICT,"Giỏ hàng trống."));
        List<CartItem> lines = items.findByCartIdOrderByProductIdAsc(cart.getId());
        if (lines.isEmpty()) throw error(HttpStatus.CONFLICT,"Giỏ hàng trống.");
        Order order = new Order(); order.setUser(user); order.setOrderCode(code);
        order.setRecipientName(input.recipientName().strip()); order.setRecipientPhone(input.recipientPhone().strip());
        order.setShippingAddress(input.shippingAddress().strip()); order.setNote(input.note());
        BigDecimal total = BigDecimal.ZERO;
        Map<Long,ShopOrder> groups=new LinkedHashMap<>();
        // Cart mutations use the same owner lock. Product locks always follow ascending IDs.
        for (CartItem line : lines) {
            Product product = products.lockById(line.getProduct().getId()).orElseThrow(() -> error(HttpStatus.CONFLICT,"Sản phẩm không còn tồn tại."));
            // The earlier cart graph may already have loaded the product; refresh under lock for its current stock/price.
            refreshLocked(product);
            CartService.validateProduct(product,line.getQuantity());
            ShopOrder group=groups.computeIfAbsent(product.getShop().getId(),key->{
                ShopOrder s=new ShopOrder();s.setOrder(order);s.setShop(product.getShop());s.setShopName(product.getShop().getName());
                s.setCommissionRate(shops.effectiveRate(product.getShop()));order.getShopOrders().add(s);return s;
            });
            BigDecimal price = CartService.price(product);
            OrderDetail snapshot = new OrderDetail(); snapshot.setOrder(order); snapshot.setProductId(product.getId());
            snapshot.setShopOrder(group);
            snapshot.setProductName(product.getName()); snapshot.setProductImage(product.getMainImage());
            snapshot.setPrice(price); snapshot.setQuantity(line.getQuantity()); snapshot.setSubtotal(price.multiply(BigDecimal.valueOf(line.getQuantity())));
            order.getDetails().add(snapshot); total = total.add(snapshot.getSubtotal());
            group.setSubtotal(group.getSubtotal().add(snapshot.getSubtotal()));
            product.setStockQuantity(product.getStockQuantity()-line.getQuantity());
        }
        if (total.compareTo(new BigDecimal("9999999999999.99")) > 0) throw error(HttpStatus.CONFLICT,"Tổng tiền vượt giới hạn đơn hàng.");
        order.setSubtotalAmount(total); order.setTotalAmount(total);
        for(ShopOrder group:groups.values()){
            group.setTotal(group.getSubtotal());
            group.setCommissionAmount(group.getSubtotal().multiply(group.getCommissionRate()).divide(new BigDecimal("100"),2,java.math.RoundingMode.HALF_UP));
            group.setSellerAmount(group.getTotal().subtract(group.getCommissionAmount()));
        }
        orders.saveAndFlush(order); notifications.placed(order); items.deleteAll(lines); items.flush();
        return detail(order);
    }
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private void refreshLocked(Product product) { entityManager.refresh(product, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE); }
    public Page<OrderDto.Summary> history(String username, int page) {
        return orders.findByUserUsernameAndIsDeletedFalse(username, PageRequest.of(Math.max(page,0),10,Sort.by("createdAt").descending().and(Sort.by("id").descending()))).map(OrderService::summary);
    }
    public OrderDto owned(String username, Long id) {
        return detail(orders.findByIdAndUserUsernameAndIsDeletedFalse(id,username).orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Không tìm thấy đơn hàng.")));
    }
    public List<com.example.shoptech.dto.ShopOrderDto> ownedShops(String username,Long id){
        Order order=orders.findByIdAndUserUsernameAndIsDeletedFalse(id,username).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Không tìm thấy đơn hàng."));
        return order.getShopOrders().stream().map(SellerOrderService::summary).toList();
    }
    public record StatusStep(OrderStatus status, java.time.OffsetDateTime at) {}
    public Map<Long,List<StatusStep>> ownedShopHistory(String username,Long id){
        Order order=orders.findByIdAndUserUsernameAndIsDeletedFalse(id,username).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Không tìm thấy đơn hàng."));
        Map<Long,List<StatusStep>> result=new LinkedHashMap<>();
        for(ShopOrder shopOrder:order.getShopOrders()){
            List<StatusStep> steps=new ArrayList<>();
            var history=shopHistory.findByShopOrderIdOrderByIdAsc(shopOrder.getId());
            OrderStatus initial=Boolean.TRUE.equals(shopOrder.getHistoricalImport())
                    ? (history.isEmpty()?shopOrder.getStatus():history.get(0).getPreviousStatus()) : OrderStatus.PENDING;
            steps.add(new StatusStep(initial,shopOrder.getCreatedAt()));
            history.forEach(h->steps.add(new StatusStep(h.getNewStatus(),h.getCreatedAt())));
            result.put(shopOrder.getId(),steps);
        }
        return result;
    }
    @Transactional
    public void requestReturn(String username, Long orderId, Long shopOrderId, String reason) {
        Order parent=orders.lockActiveById(orderId).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Không tìm thấy đơn hàng."));
        if(!parent.getUser().getUsername().equals(username))throw error(HttpStatus.NOT_FOUND,"Không tìm thấy đơn hàng.");
        ShopOrder shopOrder=shopOrders.findByIdAndOrderIsDeletedFalse(shopOrderId).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Không tìm thấy đơn của shop."));
        if(!shopOrder.getOrder().getId().equals(orderId))throw error(HttpStatus.NOT_FOUND,"Không tìm thấy đơn của shop.");
        entityManager.refresh(shopOrder);
        if(shopOrder.getStatus()!=OrderStatus.COMPLETED)throw error(HttpStatus.CONFLICT,"Chỉ yêu cầu trả hàng cho đơn đã giao thành công.");
        String note=reason==null?"":reason.strip();
        if(note.length()<10||note.length()>1000)throw error(HttpStatus.BAD_REQUEST,"Lý do trả hàng cần từ 10 đến 1000 ký tự.");
        ShopOrderHistory audit=new ShopOrderHistory();audit.setShopOrder(shopOrder);audit.setActor(parent.getUser());
        audit.setPreviousStatus(shopOrder.getStatus());audit.setNewStatus(OrderStatus.RETURN_REQUESTED);audit.setReason(note);shopHistory.save(audit);
        shopOrder.setStatus(OrderStatus.RETURN_REQUESTED);SellerOrderService.aggregate(parent);orders.flush();notifications.changed(shopOrder);
        events.publishEvent(new OrderStatusChanged(username,new com.example.shoptech.dto.OrderStatusNotification(parent.getId(),parent.getOrderCode(),parent.getOrderStatus())));
    }
    static OrderDto.Summary summary(Order o) { return new OrderDto.Summary(o.getId(),o.getOrderCode(),o.getUser().getUsername(),o.getRecipientName(),o.getTotalAmount(),o.getOrderStatus(),o.getCreatedAt()); }
    static OrderDto detail(Order o) {
        var retained=o.getShopOrders().stream().filter(s->s.getStatus()!=OrderStatus.CANCELLED&&s.getStatus()!=OrderStatus.REFUNDED).toList();
        BigDecimal subtotal=retained.stream().map(ShopOrder::getSubtotal).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal shipping=retained.stream().map(ShopOrder::getShippingFee).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal total=retained.stream().map(ShopOrder::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new OrderDto(o.getId(),o.getOrderCode(),o.getUser().getUsername(),o.getRecipientName(),o.getRecipientPhone(),o.getShippingAddress(),o.getNote(),subtotal,shipping,total,o.getPaymentMethod(),o.getPaymentStatus(),o.getOrderStatus(),o.getCreatedAt(),
                o.getDetails().stream().map(d -> new OrderDto.Line(d.getProductName(),d.getProductImage(),d.getPrice(),d.getQuantity(),d.getSubtotal())).toList());
    }
    private static String orderCode(String key) {
        try { UUID uuid = UUID.fromString(key); return "ORD-" + Base64.getUrlEncoder().withoutPadding().encodeToString(ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array()); }
        catch (IllegalArgumentException | NullPointerException e) { throw error(HttpStatus.BAD_REQUEST,"Mã yêu cầu không hợp lệ."); }
    }
}
