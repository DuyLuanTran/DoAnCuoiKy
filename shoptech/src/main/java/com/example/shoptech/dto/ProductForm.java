package com.example.shoptech.dto;

import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Locale;

public class ProductForm {

    @NotNull(message = "Vui lòng chọn danh mục.")
    @Positive(message = "Danh mục không hợp lệ.")
    private Long categoryId;

    @NotBlank(message = "Tên sản phẩm không được để trống.")
    @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự.")
    private String name;

    @Size(max = 50, message = "Mã sản phẩm tối đa 50 ký tự.")
    @Pattern(
            regexp = "[A-Z0-9_-]+",
            message = "Mã sản phẩm chỉ gồm chữ không dấu, số, dấu gạch ngang hoặc gạch dưới."
    )
    private String sku;

    @Size(max = 20000, message = "Mô tả tối đa 20000 ký tự.")
    private String description;

    @Size(max = 20000, message = "Thông số kỹ thuật tối đa 20000 ký tự.")
    private String specifications;

    @NotNull(message = "Vui lòng nhập giá sản phẩm.")
    @DecimalMin(value = "0.01", message = "Giá sản phẩm phải lớn hơn 0.")
    @Digits(
            integer = 13,
            fraction = 2,
            message = "Giá tối đa 13 chữ số phần nguyên và 2 chữ số thập phân."
    )
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "Giá khuyến mãi phải lớn hơn 0.")
    @Digits(
            integer = 13,
            fraction = 2,
            message = "Giá khuyến mãi tối đa 13 chữ số phần nguyên và 2 chữ số thập phân."
    )
    private BigDecimal discountPrice;

    @NotNull(message = "Vui lòng nhập số lượng tồn kho.")
    @Min(value = 0, message = "Số lượng tồn kho không được âm.")
    private Integer stockQuantity = 0;

    private boolean active = true;

    private MultipartFile imageFile;

    @AssertTrue(message = "Giá khuyến mãi phải nhỏ hơn giá gốc.")
    public boolean isDiscountPriceValid() {
        if (discountPrice == null || price == null) {
            return true;
        }

        return discountPrice.compareTo(price) < 0;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.strip();
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        if (sku == null || sku.isBlank()) {
            this.sku = null;
        } else {
            this.sku = sku.strip().toUpperCase(Locale.ROOT);
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = normalizeOptionalText(description);
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = normalizeOptionalText(specifications);
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(BigDecimal discountPrice) {
        this.discountPrice = discountPrice;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public MultipartFile getImageFile() {
        return imageFile;
    }

    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }
}