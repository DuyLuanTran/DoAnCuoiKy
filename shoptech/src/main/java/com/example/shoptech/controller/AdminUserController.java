package com.example.shoptech.controller;

import com.example.shoptech.dto.AdminUserForm;
import com.example.shoptech.dto.RegisterDTO;
import com.example.shoptech.entity.User;
import com.example.shoptech.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(0, page), normalizeSize(size), Sort.by("id").descending());
        model.addAttribute("users", userService.getActiveUsers(keyword, pageable));
        model.addAttribute("keyword", keyword);
        return "admin/user/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("registerForm", new RegisterDTO());
        model.addAttribute("formTitle", "Tạo tài khoản");
        model.addAttribute("formAction", "/admin/users/new");
        model.addAttribute("editing", false);
        return "admin/user/form";
    }

    @PostMapping("/new")
    public String create(
            @Valid @ModelAttribute("registerForm") RegisterDTO registerForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareCreateForm(model);
            return "admin/user/form";
        }

        try {
            userService.registerUser(registerForm);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản thành công!");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("user.create.failed", exception.getMessage());
            prepareCreateForm(model);
            return "admin/user/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.findById(id)
                .filter(existingUser -> !Boolean.TRUE.equals(existingUser.getIsDeleted()))
                .orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản cần sửa.");
            return "redirect:/admin/users";
        }

        AdminUserForm form = new AdminUserForm();
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        form.setFullName(user.getFullName());
        form.setPhoneNumber(user.getPhoneNumber());
        form.setAddress(user.getAddress());
        model.addAttribute("userId", id);
        model.addAttribute("userForm", form);
        model.addAttribute("formTitle", "Cập nhật tài khoản");
        model.addAttribute("formAction", "/admin/users/" + id + "/edit");
        model.addAttribute("editing", true);
        return "admin/user/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(
            @PathVariable Long id,
            @Valid @ModelAttribute("userForm") AdminUserForm userForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareEditForm(id, model);
            return "admin/user/form";
        }

        try {
            userService.updateUser(id, userForm);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công!");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("user.update.failed", exception.getMessage());
            prepareEditForm(id, model);
            return "admin/user/form";
        }
    }

    @GetMapping("/trash")
    public String listTrashUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(0, page), normalizeSize(size), Sort.by("deletedAt").descending());
        model.addAttribute("trashUsers", userService.getTrashUsers(pageable));
        return "admin/user/trash";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleActiveStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái tài khoản thành công!");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String moveToTrash(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.moveToTrash(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã chuyển tài khoản vào thùng rác!");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/restore")
    public String restoreUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.restoreFromTrash(id);
            redirectAttributes.addFlashAttribute("successMessage", "Khôi phục tài khoản thành công!");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users/trash";
    }

    @PostMapping("/{id}/force-delete")
    public String forceDeleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.forceDelete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vĩnh viễn tài khoản!");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users/trash";
    }

    private void prepareCreateForm(Model model) {
        model.addAttribute("formTitle", "Tạo tài khoản");
        model.addAttribute("formAction", "/admin/users/new");
        model.addAttribute("editing", false);
    }

    private void prepareEditForm(Long id, Model model) {
        model.addAttribute("userId", id);
        model.addAttribute("formTitle", "Cập nhật tài khoản");
        model.addAttribute("formAction", "/admin/users/" + id + "/edit");
        model.addAttribute("editing", true);
    }

    private int normalizeSize(int size) {
        return size == 10 || size == 20 || size == 50 ? size : 10;
    }
}
