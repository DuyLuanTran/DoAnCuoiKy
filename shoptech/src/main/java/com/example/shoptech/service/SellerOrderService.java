package com.example.shoptech.service;

import com.example.shoptech.dto.*;
import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.util.*;
import static com.example.shoptech.service.CartService.error;

@Service @Transactional(readOnly=true) @PreAuthorize("hasRole('SELLER')")
public class SellerOrderService {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final ShopService shops; private final ShopOrderRepository shopOrders;
    private final OrderRepository orders; private final ProductRepository products;
    private final UserRepository users;private final ShopOrderHistoryRepository history;private final ApplicationEventPublisher events;
    public SellerOrderService(ShopService shops,ShopOrderRepository shopOrders,OrderRepository orders,ProductRepository products,UserRepository users,ShopOrderHistoryRepository history,ApplicationEventPublisher events){
        this.shops=shops;this.shopOrders=shopOrders;this.orders=orders;this.products=products;this.users=users;this.history=history;this.events=events;
    }
    public Page<ShopOrderDto> search(String username,String keyword,OrderStatus status,int page){
        if(keyword.length()>100)throw error(HttpStatus.BAD_REQUEST,"Từ khóa tối đa 100 ký tự.");
        return shopOrders.search(keyword.strip(),status,shops.requiredOwn(username).getId(),PageRequest.of(Math.max(0,page),10,Sort.by("createdAt").descending().and(Sort.by("id").descending()))).map(SellerOrderService::summary);
    }
    public ShopOrder get(String username,Long id){return shopOrders.findByIdAndShopIdAndOrderIsDeletedFalse(id,shops.requiredOwn(username).getId()).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Không tìm thấy đơn hàng."));}
    public OrderDto detail(String username,Long id){return detail(get(username,id));}
    public List<ShopOrderHistory> history(String username,Long id){get(username,id);return history.findByShopOrderIdOrderByIdDesc(id);}
    @Transactional
    public OrderDto transition(String username,Long id,OrderDto.Transition input,String reason){
        // Parent lock serializes sibling shops and retries. Always lock products in ascending order afterwards.
        ShopOrder initial=get(username,id);
        Order parent=orders.lockActiveById(initial.getOrder().getId()).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Không tìm thấy đơn hàng."));
        entityManager.refresh(parent);
        ShopOrder current=get(username,id);
        entityManager.refresh(current);
        if(!Boolean.TRUE.equals(current.getShop().getOwner().getIsActive())||Boolean.TRUE.equals(current.getShop().getOwner().getIsDeleted()))throw error(HttpStatus.FORBIDDEN,"Tài khoản không hoạt động.");
        // Existing orders must still be fulfilled/refunded while a shop is suspended.
        if(current.getShop().getStatus()!=ShopStatus.ACTIVE&&current.getShop().getStatus()!=ShopStatus.SUSPENDED)throw error(HttpStatus.FORBIDDEN,"Shop chưa hoạt động.");
        if(needsReconciliation(current))throw error(HttpStatus.CONFLICT,"Đơn lịch sử lệch tổng tiền. Cần đối soát trước khi xử lý hoặc hoàn kho.");
        if(current.getStatus()!=input.expectedStatus())throw error(HttpStatus.CONFLICT,"Đơn đã được cập nhật. Vui lòng tải lại.");
        if(!allowed(current.getStatus()).contains(input.status()))throw error(HttpStatus.CONFLICT,"Chuyển trạng thái không hợp lệ.");
        String note=reason==null?"":reason.strip();
        if(note.length()>1000)throw error(HttpStatus.BAD_REQUEST,"Lý do tối đa 1000 ký tự.");
        if(Set.of(OrderStatus.CANCELLED,OrderStatus.RETURN_REQUESTED,OrderStatus.REFUNDED).contains(input.status())&&note.isEmpty())throw error(HttpStatus.BAD_REQUEST,"Vui lòng nhập lý do hủy hoặc trả hàng / hoàn tiền.");
        if(input.status()==OrderStatus.CANCELLED && (!"COD".equals(parent.getPaymentMethod())||!"UNPAID".equals(current.getPaymentStatus())))throw error(HttpStatus.CONFLICT,"Không thể hủy đơn đã thanh toán.");
        if(input.status()==OrderStatus.REFUNDED && !"PAID".equals(current.getPaymentStatus()))throw error(HttpStatus.CONFLICT,"Chỉ hoàn tiền đơn đã thanh toán.");
        if(input.status()==OrderStatus.CANCELLED||input.status()==OrderStatus.REFUNDED){
            for(OrderDetail line:parent.getDetails().stream().filter(d->d.getShopOrder().getId().equals(id)).sorted(Comparator.comparing(OrderDetail::getProductId)).toList()){
                Product p=products.lockById(line.getProductId()).orElseThrow(()->error(HttpStatus.CONFLICT,"Không thể hoàn kho sản phẩm."));
                long quantity=(long)p.getStockQuantity()+line.getQuantity();if(quantity>Integer.MAX_VALUE)throw error(HttpStatus.CONFLICT,"Tồn kho vượt giới hạn.");p.setStockQuantity((int)quantity);
            }
        }
        if(input.status()==OrderStatus.COMPLETED&&"COD".equals(parent.getPaymentMethod()))current.setPaymentStatus("PAID");
        if(input.status()==OrderStatus.REFUNDED)current.setPaymentStatus("REFUNDED");
        ShopOrderHistory audit=new ShopOrderHistory();audit.setShopOrder(current);audit.setActor(users.findByUsernameAndIsDeletedFalse(username).orElseThrow());audit.setPreviousStatus(current.getStatus());audit.setNewStatus(input.status());audit.setReason(note);history.save(audit);
        current.setStatus(input.status());aggregate(parent);orders.flush();
        events.publishEvent(new OrderStatusChanged(parent.getUser().getUsername(),new OrderStatusNotification(parent.getId(),parent.getOrderCode(),parent.getOrderStatus())));
        return detail(current);
    }
    public boolean needsReconciliation(ShopOrder s){
        return Boolean.TRUE.equals(s.getHistoricalImport())&&s.getOrder().getDetails().stream().filter(d->d.getShopOrder().getId().equals(s.getId())).map(OrderDetail::getSubtotal).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add).compareTo(s.getSubtotal())!=0;
    }
    public static Set<OrderStatus> allowed(OrderStatus status){return Collections.unmodifiableSet(new LinkedHashSet<>(switch(status){
        case PENDING->List.of(OrderStatus.CONFIRMED,OrderStatus.CANCELLED);
        case CONFIRMED->List.of(OrderStatus.PICKED_UP,OrderStatus.CANCELLED);
        case PICKED_UP->List.of(OrderStatus.SHIPPING);
        case SHIPPING->List.of(OrderStatus.COMPLETED);
        case COMPLETED->List.of(OrderStatus.RETURN_REQUESTED);
        case RETURN_REQUESTED->List.of(OrderStatus.REFUNDED);
        default->List.of();
    }));}
    private void aggregate(Order parent){
        List<ShopOrder> all=parent.getShopOrders();
        // The least advanced live shipment represents progress; individual shop states are always shown.
        List<OrderStatus> live=all.stream().map(ShopOrder::getStatus).filter(s->s!=OrderStatus.CANCELLED&&s!=OrderStatus.REFUNDED).toList();
        OrderStatus status;
        if(live.isEmpty())status=all.stream().anyMatch(s->s.getStatus()==OrderStatus.REFUNDED)?OrderStatus.REFUNDED:OrderStatus.CANCELLED;
        else if(live.contains(OrderStatus.RETURN_REQUESTED))status=OrderStatus.RETURN_REQUESTED;
        else status=live.stream().min(Comparator.comparingInt(SellerOrderService::progress)).orElseThrow();
        parent.setOrderStatus(status);
        if(all.stream().allMatch(s->Set.of("PAID","REFUNDED").contains(s.getPaymentStatus())||s.getStatus()==OrderStatus.CANCELLED)){
            parent.setPaymentStatus(all.stream().anyMatch(s->"PAID".equals(s.getPaymentStatus()))?"PAID":all.stream().anyMatch(s->"REFUNDED".equals(s.getPaymentStatus()))?"REFUNDED":"UNPAID");
        }else parent.setPaymentStatus(all.stream().anyMatch(s->"PAID".equals(s.getPaymentStatus()))?"PARTIALLY_PAID":"UNPAID");
    }
    private static int progress(OrderStatus s){return switch(s){case PENDING->0;case CONFIRMED->1;case PICKED_UP->2;case SHIPPING->3;default->4;};}
    public static ShopOrderDto summary(ShopOrder s){Order o=s.getOrder();return new ShopOrderDto(s.getId(),o.getId(),o.getOrderCode()+" / S"+s.getId(),s.getShopName(),o.getUser().getUsername(),o.getRecipientName(),s.getTotal(),s.getStatus(),s.getPaymentStatus(),s.getCreatedAt(),s.getCommissionRate(),s.getCommissionAmount(),s.getSellerAmount(),Boolean.TRUE.equals(s.getHistoricalImport()));}
    static OrderDto detail(ShopOrder s){Order o=s.getOrder();return new OrderDto(s.getId(),o.getOrderCode()+" / S"+s.getId(),o.getUser().getUsername(),o.getRecipientName(),o.getRecipientPhone(),o.getShippingAddress(),o.getNote(),s.getSubtotal(),s.getShippingFee(),s.getTotal(),o.getPaymentMethod(),s.getPaymentStatus(),s.getStatus(),s.getCreatedAt(),o.getDetails().stream().filter(d->d.getShopOrder().getId().equals(s.getId())).map(d->new OrderDto.Line(d.getProductName(),d.getProductImage(),d.getPrice(),d.getQuantity(),d.getSubtotal())).toList());}
}
