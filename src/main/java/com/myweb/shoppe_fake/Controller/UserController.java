package com.myweb.shoppe_fake.Controller;

import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.myweb.shoppe_fake.Model.User;
import com.myweb.shoppe_fake.Service.UserService;

@Controller
@RequestMapping("/admin/users")
public class UserController {

    @Autowired
    private UserService userService;

    // 1. Hiển thị danh sách (Tìm kiếm & Phân trang)
    @GetMapping
    public String listUsers(Model model, 
                            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                            @RequestParam(value = "keyword", required = false) String keyword) {
        int pageSize = 5; // Số user trên 1 trang
        Page<User> page = userService.getPaginatedUsers(pageNo, pageSize, keyword);
        
        model.addAttribute("users", page.getContent());
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("keyword", keyword);
        return "admin/user/list";
    }

    // 2. Form thêm mới
    @GetMapping("/add")
    public String showAddForm(Model model) {
        User user = new User();
        user.setRoles(new HashSet<>()); // Khởi tạo set để tránh null
        model.addAttribute("user", user);
       return "admin/user/add";
    }

    // 3. Form cập nhật
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        User user = userService.getUserById(id);
        if (user == null) {
            return "redirect:/admin/users";
        }
        model.addAttribute("user", user);
        return "admin/user/edit";
    }

    // 4. Lưu người dùng (Cho cả Thêm và Sửa)
    @PostMapping("/save")
    public String saveUser(@ModelAttribute("user") User user) {
        // Lưu ý: Trong thực tế, bạn cần mã hóa password (VD: BCrypt) trước khi lưu.
        userService.saveUser(user);
        return "redirect:/admin/users";
    }

    // 5. Khóa / Mở khóa người dùng
    @GetMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable("id") String id) {
        userService.toggleUserStatus(id);
        return "redirect:/admin/users";
    }
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") String id) {
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }
}
