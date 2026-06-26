package com.myweb.shoppe_fake.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.myweb.shoppe_fake.Model.User;
import com.myweb.shoppe_fake.Service.UserService;

@Controller
public class AuthController {
    @Autowired
    private UserService userService;

    // --- ĐĂNG NHẬP ---
    @GetMapping("/login")
    public String loginPage() {
        return "user/login";// Trả về giao diện login.html
    }

    // --- ĐĂNG KÝ ---
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "user/register";
    }

    @PostMapping("/register")
    public String processRegister(@ModelAttribute("user") User user, Model model) {
        try {
            String result = userService.registerUser(user);
            if ("success".equals(result)) {
                return "redirect:/login?registerSuccess=true";
            }
            model.addAttribute("error", result);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage()); // Bắt lỗi trùng username từ UserService
        }
        return "user/register";
    }

    // --- QUÊN MẬT KHẨU ---
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "user/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, Model model) {
        String result = userService.generateResetToken(email);
        if ("success".equals(result)) {
            model.addAttribute("message", "Link đặt lại mật khẩu đã được gửi vào email của bạn. Vui lòng kiểm tra hộp thư!");
        } else {
            model.addAttribute("error", result);
        }
        return "user/forgot-password";
    }
    
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(value = "token", required = false) String token, Model model) {
        if (token == null || token.isEmpty()) {
            model.addAttribute("error", "Token không hợp lệ hoặc đã hết hạn.");
            return "user/forgot-password"; // Bắt nhập lại email nếu không có token
        }
        
        // Đẩy token sang giao diện HTML để tí nữa POST lên lại
        model.addAttribute("token", token); 
        return "user/reset-password"; // Trả về file reset-password.html
    }

    // 2. Hàm này xử lý khi người dùng bấm nút "Lưu mật khẩu"
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token, 
                                       @RequestParam("newPassword") String newPassword, 
                                       Model model) {
        try {
           
            userService.resetPassword(token, newPassword);

            return "redirect:/login?resetSuccess=true"; 
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            model.addAttribute("token", token);
            return "user/reset-password";
        }
    } 
}
                                       