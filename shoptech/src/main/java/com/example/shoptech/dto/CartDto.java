package com.example.shoptech.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record CartDto(List<Item> items, BigDecimal total, long quantity) {
    public record Item(Long id, Long productId, String name, String image, String slug,
                       BigDecimal price, int quantity, int stock, BigDecimal subtotal) {}
    public record Add(@NotNull @Positive Long productId,
            @NotNull @DecimalMin("1") @DecimalMax("1000000") @Digits(integer=7,fraction=0) BigDecimal quantity) {}
    public record Quantity(@NotNull @DecimalMin("1") @DecimalMax("1000000") @Digits(integer=7,fraction=0) BigDecimal quantity) {}
}
