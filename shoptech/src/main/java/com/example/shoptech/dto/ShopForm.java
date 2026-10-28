package com.example.shoptech.dto;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class ShopForm {
    @NotBlank(message="Nhập tên shop.") @Size(max=100) private String name;
    @NotBlank(message="Nhập email liên hệ.") @Email(message="Email không hợp lệ.") @Size(max=100) private String email;
    @NotBlank(message="Nhập số điện thoại.") @Pattern(regexp="[+0-9][0-9 .()-]{7,19}",message="Số điện thoại không hợp lệ.") private String phone;
    @NotBlank(message="Nhập địa chỉ lấy hàng.") @Size(max=500) private String address;
    @Size(max=2000) private String description;
}
