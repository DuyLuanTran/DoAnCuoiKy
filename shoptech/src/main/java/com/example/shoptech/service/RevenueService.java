package com.example.shoptech.service;
import com.example.shoptech.repository.ShopOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.example.shoptech.service.CartService.error;
@Service @Transactional(readOnly=true)
public class RevenueService {
    public static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    public enum Group { DAY, MONTH }
    public record Point(String label,BigDecimal revenue,long orders,BigDecimal commission,BigDecimal sellerAmount){}
    public record Report(BigDecimal totalRevenue,long completedOrders,String timezone,List<Point> points,BigDecimal totalCommission,BigDecimal totalSellerAmount){}
    private final ShopOrderRepository orders;private final ShopService shops;
    public RevenueService(ShopOrderRepository orders,ShopService shops){this.orders=orders;this.shops=shops;}
    @PreAuthorize("hasRole('ADMIN')") public Report report(LocalDate from,LocalDate to,Group group){return calculate(null,from,to,group);}
    @PreAuthorize("hasRole('SELLER')") public Report sellerReport(String username,LocalDate from,LocalDate to,Group group){return calculate(shops.requiredOwn(username).getId(),from,to,group);}
    private Report calculate(Long shopId,LocalDate from,LocalDate to,Group group){
        if(from==null||to==null||group==null||from.isAfter(to)||ChronoUnit.DAYS.between(from,to)>365||to.getYear()>9998||from.getYear()<1900)throw error(HttpStatus.BAD_REQUEST,"Chọn khoảng ngày hợp lệ, tối đa 366 ngày.");
        Map<String,Point> buckets=new LinkedHashMap<>();
        for(LocalDate day=group==Group.DAY?from:from.withDayOfMonth(1);!day.isAfter(to);day=group==Group.DAY?day.plusDays(1):day.plusMonths(1)){
            String key=group==Group.DAY?day.toString():YearMonth.from(day).toString();buckets.put(key,new Point(key,BigDecimal.ZERO,0,BigDecimal.ZERO,BigDecimal.ZERO));
        }
        BigDecimal total=BigDecimal.ZERO,commission=BigDecimal.ZERO,seller=BigDecimal.ZERO;long count=0;
        for(var order:orders.revenue(shopId,from.atStartOfDay(ZONE).toOffsetDateTime(),to.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime())){
            LocalDate date=order.getCreatedAt().atZoneSameInstant(ZONE).toLocalDate();String key=group==Group.DAY?date.toString():YearMonth.from(date).toString();Point old=buckets.get(key);
            buckets.put(key,new Point(key,old.revenue().add(order.getTotal()),old.orders()+1,old.commission().add(order.getCommissionAmount()),old.sellerAmount().add(order.getSellerAmount())));
            total=total.add(order.getTotal());commission=commission.add(order.getCommissionAmount());seller=seller.add(order.getSellerAmount());count++;
        }
        return new Report(total,count,ZONE.toString(),List.copyOf(buckets.values()),commission,seller);
    }
}
