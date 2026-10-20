package com.example.shoptech.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterDTO {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập từ 3 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không quá 100 ký tự")
    private String fullName;

    @Pattern(regexp = "^$|^[0-9+(). -]{8,15}$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;

    @Size(max = 255, message = "Địa chỉ không quá 255 ký tự")
    private String address;

    public RegisterDTO() {
    }

    public RegisterDTO(String username, String email, String password, String fullName,
                       String phoneNumber, String address) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public static RegisterDTOBuilder builder() {
        return new RegisterDTOBuilder();
    }

    public static class RegisterDTOBuilder {
        private String username;
        private String email;
        private String password;
        private String fullName;
        private String phoneNumber;
        private String address;

        public RegisterDTOBuilder username(String username) { this.username = username; return this; }
        public RegisterDTOBuilder email(String email) { this.email = email; return this; }
        public RegisterDTOBuilder password(String password) { this.password = password; return this; }
        public RegisterDTOBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public RegisterDTOBuilder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
        public RegisterDTOBuilder address(String address) { this.address = address; return this; }

        public RegisterDTO build() {
            return new RegisterDTO(username, email, password, fullName, phoneNumber, address);
        }
    }
}
