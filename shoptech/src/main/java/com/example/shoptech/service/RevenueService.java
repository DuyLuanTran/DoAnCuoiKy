package com.example.shoptech.service;
import com.example.shoptech.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.example.shoptech.service.CartService.error;

@Service @Transactional(readOnly=true) @PreAuthorize("hasRole('ADMIN')")
public class RevenueService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public enum Group { DAY, MONTH }
    public record Point(String label, BigDecimal revenue, long orders) {}
    public record Report(BigDecimal totalRevenue, long completedOrders, String timezone, List<Point> points) {}
    private final OrderRepository orders;
    public RevenueService(OrderRepository orders) { this.orders=orders; }
    public Report report(LocalDate from, LocalDate to, Group group) {
        if (from == null || to == null || group == null || from.isAfter(to) || ChronoUnit.DAYS.between(from,to) > 365 || to.getYear() > 9998 || from.getYear() < 1900)
            throw error(HttpStatus.BAD_REQUEST,"Chọn khoảng ngày hợp lệ, tối đa 366 ngày.");
        Map<String,Point> buckets=new LinkedHashMap<>();
        for (LocalDate day=group==Group.DAY?from:from.withDayOfMonth(1); !day.isAfter(to); day=group==Group.DAY?day.plusDays(1):day.plusMonths(1)) {
            String key=group==Group.DAY?day.toString():YearMonth.from(day).toString();
            buckets.put(key,new Point(key,BigDecimal.ZERO,0));
        }
        BigDecimal total=BigDecimal.ZERO; long count=0;
        for (OrderRepository.RevenueDay day : orders.revenueDays(from.atStartOfDay(ZONE).toOffsetDateTime(),to.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime())) {
            String key=group==Group.DAY?day.getBucket().toString():YearMonth.from(day.getBucket()).toString();
            Point previous=buckets.get(key);
            if (previous != null) buckets.put(key,new Point(key,previous.revenue().add(day.getRevenue()),previous.orders()+day.getOrderCount()));
            total=total.add(day.getRevenue()); count+=day.getOrderCount();
        }
        return new Report(total,count,ZONE.toString(),List.copyOf(buckets.values()));
    }
}
