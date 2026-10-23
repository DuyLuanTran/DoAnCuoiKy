package com.example.shoptech.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ForgotPasswordForm {

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email không quá 100 ký tự")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
