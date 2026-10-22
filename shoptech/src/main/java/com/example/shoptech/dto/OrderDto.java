package com.example.shoptech.dto;
import com.example.shoptech.entity.OrderStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
public record OrderDto(Long id, String code, String customer, String recipientName, String recipientPhone,
        String shippingAddress, String note, BigDecimal subtotal, BigDecimal shippingFee, BigDecimal total,
        String paymentMethod, String paymentStatus, OrderStatus status, OffsetDateTime createdAt, List<Line> items) {
    public record Transition(@NotNull OrderStatus expectedStatus, @NotNull OrderStatus status) {}
    public record Line(String name, String image, BigDecimal price, int quantity, BigDecimal subtotal) {}
    public record Summary(Long id, String code, String customer, String recipientName, BigDecimal total,
                          OrderStatus status, OffsetDateTime createdAt) {}
    public record Checkout(@NotBlank @Size(max=100) String recipientName,
            @NotBlank @Pattern(regexp="[+0-9][0-9 .()-]{7,19}") String recipientPhone,
            @NotBlank @Size(max=2000) String shippingAddress, @Size(max=2000) String note,
            @NotNull @Pattern(regexp="COD") String paymentMethod,
            @NotNull @Pattern(regexp="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") String requestKey) {}
}
