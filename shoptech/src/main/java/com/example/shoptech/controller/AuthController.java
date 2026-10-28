package com.example.shoptech.controller;

import com.example.shoptech.dto.RegisterDTO;
import com.example.shoptech.dto.ForgotPasswordForm;
import com.example.shoptech.dto.ResetPasswordForm;
import com.example.shoptech.security.JwtCookieAuthenticationFilter;
import com.example.shoptech.service.JwtService;
import com.example.shoptech.service.PasswordResetService;
import com.example.shoptech.service.UserService;
import com.example.shoptech.service.CategoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final CategoryService categoryService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;
    private final boolean secureCookie;

    @ModelAttribute
    public void initializeCsrfToken(CsrfToken csrfToken) {
        csrfToken.getToken();
    }

    public AuthController(
            UserService userService,
            CategoryService categoryService,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            PasswordResetService passwordResetService,
            @Value("${shoptech.security.cookie-secure:false}") boolean secureCookie) {
        this.userService = userService;
        this.categoryService = categoryService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
        this.secureCookie = secureCookie;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String redirect, Model model) {
        if (redirect != null && !redirect.isBlank()) {
            model.addAttribute("redirect", redirect);
        }
        return "admin/login";
    }

    @PostMapping("/login")
    public String authenticate(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String redirect,
            jakarta.servlet.http.HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));
            String token = jwtService.createToken(authentication.getName());
            ResponseCookie cookie = ResponseCookie.from(JwtCookieAuthenticationFilter.COOKIE_NAME, token)
                    .httpOnly(true)
                    .secure(secureCookie)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(jwtService.getValiditySeconds())
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
                boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
                if (!isAdmin) {
                    return "redirect:" + redirect;
                }
            }
            return redirectAfterLogin();
        } catch (AuthenticationException exception) {
            String redirectParam = (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//"))
                    ? "&redirect=" + java.net.URLEncoder.encode(redirect, java.nio.charset.StandardCharsets.UTF_8)
                    : "";
            return "redirect:/login?error" + redirectParam;
        }
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model, Authentication authentication) {
        model.addAttribute("adminName", authentication.getName());
        return "admin/index";
    }

    @GetMapping("/user/home")
    public String userHome(){return "redirect:/";}

    @GetMapping("/account")
    public String account(Authentication authentication, Model model) {
        return userService.findByUsername(authentication.getName())
                .map(user -> {
                    model.addAttribute("account", user);
                    boolean admin = user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
                    boolean seller = user.getRoles().stream().anyMatch(role -> "ROLE_SELLER".equals(role.getName()));
                    model.addAttribute("accountRole", admin ? "Quản trị viên" : seller ? "Người bán" : "Khách hàng");
                    model.addAttribute("categories", categoryService.getAvailableCategories());
                    return "user/account";
                })
                .orElse("redirect:/login?error");
    }

    @GetMapping("/register")
    public String registerForm(@RequestParam(required = false) String redirect, Model model) {
        model.addAttribute("registerForm", new RegisterDTO());
        if (redirect != null && !redirect.isBlank()) {
            model.addAttribute("redirect", redirect);
        }
        return "admin/register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam(required = false) String redirect,
            @Valid @ModelAttribute("registerForm") RegisterDTO registerForm,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            if (redirect != null && !redirect.isBlank()) {
                model.addAttribute("redirect", redirect);
            }
            return "admin/register";
        }

        try {
            userService.registerUser(registerForm);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("registration.failed", exception.getMessage());
            if (redirect != null && !redirect.isBlank()) {
                model.addAttribute("redirect", redirect);
            }
            return "admin/register";
        }

        String redirectParam = (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//"))
                ? "&redirect=" + java.net.URLEncoder.encode(redirect, java.nio.charset.StandardCharsets.UTF_8)
                : "";
        return "redirect:/login?registered" + redirectParam;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm(Model model) {
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestPasswordReset(
            @Valid @ModelAttribute("forgotPasswordForm")
            ForgotPasswordForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }
        try {
            passwordResetService.requestPasswordReset(form.getEmail());
        } catch (MailException exception) {
            logger.error("Could not send password reset email", exception);
        }
        redirectAttributes.addFlashAttribute("resetRequestSubmitted", true);
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam(defaultValue = "") String token, Model model) {
        ResetPasswordForm form = new ResetPasswordForm();
        form.setToken(token);
        model.addAttribute("resetPasswordForm", form);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }
        try {
            passwordResetService.resetPassword(form.getToken(), form.getPassword());
            return "redirect:/login?passwordReset";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("reset.failed", exception.getMessage());
            return "auth/reset-password";
        }
    }

    private String redirectAfterLogin() {
        return "redirect:/products";
    }
}
