package com.example.shoptech.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class CatalogFilters {
    private String keyword = "";
    private Long categoryId;
    private Long shopId;
    private long minSold;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private boolean inStock;
    private boolean onSale;
    private String sort = "newest";
    private int page;
    private int size = 12;
}
